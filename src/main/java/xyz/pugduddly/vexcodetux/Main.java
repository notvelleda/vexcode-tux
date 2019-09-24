package xyz.pugduddly.vexcodetux;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import javax.swing.filechooser.FileFilter;

import javax.imageio.ImageIO;

import java.awt.event.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;

import java.util.function.Consumer;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.Enumeration;

import org.json.JSONObject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

//import gnu.io.CommPortIdentifier;

public class Main extends JFrame implements ActionListener {
    public static final String title = "VEXCode Tux";
    public static final int width = 640;
    public static final int height = 400;

    private static final Color blurple = new Color(0x7289da);
    private static final Color white = new Color(0xffffff);
    private static final Color gray = new Color(0x99aab5);
    private static final Color darkgray = new Color(0x2c2f33);
    private static final Color darkergray = new Color(0x23272a);

    private JTextArea messages;

    private JMenuBar menuBar;
    private JMenu fileMenu;
    private JMenuItem newMenuItem;
    private JMenuItem openMenuItem;
    private JMenuItem saveMenuItem;
    private JMenuItem saveAsMenuItem;
    private JMenu projectMenu;
    private JMenuItem buildMenuItem;
    private JMenuItem uploadMenuItem;
    private JTextField projectName;
    private JSpinner slotSpinner;
    private JTextField projectDesc;
    private JCheckBox isCompetition;

    private JFileChooser openFileChooser;
    private JFileChooser saveFileChooser;

    private Project project;
    
    public Main() {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ee) {
                ee.printStackTrace();
            }
        }

        JPanel content = new JPanel();
        content.setBounds(0, 0, width, height);
        content.setPreferredSize(new Dimension(width, height));
        content.setLayout(null);

        messages = new JTextArea();
        messages.setFont(new Font("monospaced", Font.PLAIN, 12));
        messages.setEditable(false);
        DefaultCaret caret = (DefaultCaret) messages.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        JScrollPane scrollPane = new JScrollPane(messages);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setBounds(5, height - height / 3 * 2 + 5, width - 10, height / 3 * 2 - 10);

        content.add(scrollPane);

        menuBar = new JMenuBar();

        fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        newMenuItem = new JMenuItem("New", KeyEvent.VK_N);
        newMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
        newMenuItem.addActionListener(this);
        fileMenu.add(newMenuItem);
        openMenuItem = new JMenuItem("Open", KeyEvent.VK_O);
        openMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
        openMenuItem.addActionListener(this);
        fileMenu.add(openMenuItem);
        saveMenuItem = new JMenuItem("Save", KeyEvent.VK_S);
        saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
        saveMenuItem.addActionListener(this);
        fileMenu.add(saveMenuItem);
        saveAsMenuItem = new JMenuItem("Save As", KeyEvent.VK_A);
        saveAsMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.SHIFT_MASK | ActionEvent.CTRL_MASK));
        saveAsMenuItem.addActionListener(this);
        fileMenu.add(saveAsMenuItem);
        menuBar.add(fileMenu);

        projectMenu = new JMenu("Project");
        projectMenu.setMnemonic(KeyEvent.VK_P);
        buildMenuItem = new JMenuItem("Build", KeyEvent.VK_B);
        buildMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, ActionEvent.CTRL_MASK));
        buildMenuItem.addActionListener(this);
        projectMenu.add(buildMenuItem);
        uploadMenuItem = new JMenuItem("Upload to Brain", KeyEvent.VK_U);
        uploadMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, ActionEvent.CTRL_MASK));
        uploadMenuItem.addActionListener(this);
        projectMenu.add(uploadMenuItem);
        menuBar.add(projectMenu);

        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_P);
        JMenuItem aboutMenuItem = new JMenuItem("About", KeyEvent.VK_A);
        aboutMenuItem.addActionListener(this);
        helpMenu.add(aboutMenuItem);
        menuBar.add(helpMenu);

        this.setJMenuBar(menuBar);

        projectName = new JTextField();
        projectName.setBounds(5, 5, width / 3 - 10, 32);
        projectName.setText("Project Name");
        content.add(projectName);

        SpinnerModel slotModel = new SpinnerNumberModel(1, 1, 8, 1);     
        slotSpinner = new JSpinner(slotModel);
        slotSpinner.setBounds(width / 3 + 5, 5, width / 6 - 10, 32);
        content.add(slotSpinner);

        projectDesc = new JTextField();
        projectDesc.setBounds(5, 48, width / 2 - 10, 32);
        projectDesc.setText("Project Description");
        content.add(projectDesc);

        isCompetition = new JCheckBox("Competition");
        isCompetition.setBounds(25, 96, 200, 25);
        content.add(isCompetition);
        
        newMenuItem.setEnabled(false);
        openMenuItem.setEnabled(false);
        saveMenuItem.setEnabled(false);
        saveAsMenuItem.setEnabled(false);
        buildMenuItem.setEnabled(false);
        uploadMenuItem.setEnabled(false);
        projectName.setEnabled(false);
        projectDesc.setEnabled(false);
        slotSpinner.setEnabled(false);
        isCompetition.setEnabled(false);

        openFileChooser = new JFileChooser();

        openFileChooser.addChoosableFileFilter(new CustomFileFilter(new String[] { "v5code", "vex" }, "All compatible files"));
        openFileChooser.addChoosableFileFilter(new CustomFileFilter(new String[] { "v5code" }, "VEXCode V5 Text files (.v5code)"));
        openFileChooser.addChoosableFileFilter(new CustomFileFilter(new String[] { "vex" }, "VEX Coding Studio files (.vex)"));

        openFileChooser.setAcceptAllFileFilterUsed(false);

        saveFileChooser = new JFileChooser();

        saveFileChooser.addChoosableFileFilter(new CustomFileFilter(new String[] { "v5code" }, "VEXCode V5 Text files (.v5code)"));

        saveFileChooser.setAcceptAllFileFilterUsed(false);
        
        add(content);
        pack();

        setTitle(this.title);
        setLocationRelativeTo(null);
        setLayout(null);
        setVisible(true);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        messages.append("Checking for SDK update...\n");
        try {
            if (SDK.shouldUpdate()) {
                messages.append("Updating SDK...\n");
                SDK.update();
            }

            messages.append("Done\n");
            newMenuItem.setEnabled(true);
            openMenuItem.setEnabled(true);
        } catch (Exception e) {
            e.printStackTrace();
            messages.append("Got exception " + e + ", aborting\n");
        }
    }

    private void disableGUI() {
        saveMenuItem.setEnabled(false);
        saveAsMenuItem.setEnabled(false);
        buildMenuItem.setEnabled(false);
        uploadMenuItem.setEnabled(false);
        projectName.setEnabled(false);
        projectDesc.setEnabled(false);
        slotSpinner.setEnabled(false);
        isCompetition.setEnabled(false);
    }

    private void enableGUI() {
        saveMenuItem.setEnabled(true);
        saveAsMenuItem.setEnabled(true);
        buildMenuItem.setEnabled(true);
        uploadMenuItem.setEnabled(true);
        projectName.setEnabled(true);
        projectDesc.setEnabled(true);
        slotSpinner.setEnabled(true);
        isCompetition.setEnabled(true);
    }

    private void updateProject() {
        this.project.setName(this.projectName.getText());
        this.project.setDescription(this.projectDesc.getText());
        this.project.setSlot((int) this.slotSpinner.getValue());
        this.project.setCompetition(this.isCompetition.isSelected());
    }

    private void updateGUI() {
        this.projectName.setText(this.project.getName());
        this.projectDesc.setText(this.project.getDescription());
        this.slotSpinner.setValue(this.project.getSlot());
        this.isCompetition.setSelected(this.project.isCompetition());
    }

    public void actionPerformed(ActionEvent event) {
        if (event.getActionCommand().equals("About")) {
            JOptionPane.showMessageDialog(this, title + "\nCopyright (c) 2019 Pugduddly\n\nSDK version " + SDK.getVersion());
        } else if (event.getActionCommand().equals("New")) {
            enableGUI();
            saveMenuItem.setEnabled(false);
            buildMenuItem.setEnabled(false);
            uploadMenuItem.setEnabled(false);

            this.project = new Project();
            this.updateGUI();
        } else if (event.getActionCommand().equals("Open")) {
            int returnVal = openFileChooser.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = openFileChooser.getSelectedFile();
                String ext = Utils.getExtension(file);

                try {
                    if (ext.equals("v5code")) {
                        this.project = Project.fromFile(file);
                        this.updateGUI();
                    } else if (ext.equals("vex")) {
                        int dialogResult = JOptionPane.showConfirmDialog(this, "This project requires conversion. Would you like to convert it?", title, JOptionPane.YES_NO_OPTION);
                        if (dialogResult == JOptionPane.YES_OPTION) {
                            try {
                                this.project = Project.convertVEXCodingStudioFile(file);
                                this.updateGUI();
                                messages.append("Converted project " + projectName.getText() + " (" + file + ")\n");
                            } catch (Exception e) {
                                e.printStackTrace();
                                JOptionPane.showMessageDialog(this, "Caught exception " + e + " while converting project");
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                enableGUI();

                messages.append("Opened project " + project.getName() + " (" + file + ")\n");
            }
        } else if (event.getActionCommand().equals("Build")) {
            messages.append("Building project " + projectName.getText() + "...\n");
            disableGUI();
            new Thread() {
                @Override
                public void run() {
                    updateProject();
                    messages.append(Main.this.project.build().getLog());
                    messages.append("Done\n");
                    enableGUI();
                }
            }.start();
        } else if (event.getActionCommand().equals("Upload to Brain")) {
            messages.append("Building project " + projectName.getText() + "...\n");
            disableGUI();
            new Thread() {
                @Override
                public void run() {
                    updateProject();
                    BuildResult result = Main.this.project.build();
                    messages.append(result.getLog());
                    messages.append("Done\n");
                    if (result.getExitCode() == 0) {
                        messages.append("Uploading project " + projectName.getText() + "...\n");
                        messages.append(Main.this.project.upload().getLog());
                        messages.append("Done\n");
                    } else {
                        messages.append("Build failed, so not uploading project.\n");
                    }
                    enableGUI();
                }
            }.start();
        } else if (event.getActionCommand().equals("Save")) {
            try {
                this.updateProject();
                this.project.save();
                messages.append("Saved project " + projectName.getText() + " (" + this.project.getFile() + ")\n");
            } catch (Exception e) {
                e.printStackTrace();
                messages.append("Caught exception " + e + ", aborting\n");
            }
        } else if (event.getActionCommand().equals("Save As")) {
            this.updateProject();
            int returnVal = saveFileChooser.showSaveDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                try {
                    File file = saveFileChooser.getSelectedFile();
                    File dir = file.getParentFile();

                    if (!file.exists()) {
                        String ext = Utils.getExtension(file);

                        if (ext == null || !ext.equals("v5code")) {
                            file = new File(dir, file.getName() + ".v5code");
                        }

                        int dialogResult = JOptionPane.showConfirmDialog(this, "Would you like to create a directory for this project?", title, JOptionPane.YES_NO_OPTION);
                        if (dialogResult == JOptionPane.YES_OPTION) {
                            dir = new File(dir.toString(), file.getName().replaceFirst("[.][^.]+$", "") + "/");
                            dir.mkdirs();
                            file = new File(dir, file.getName());
                        }

                        if (this.project.getFile() == null) {
                            this.project.saveAs(file, true, false);
                        } else {
                            int dialogResult2 = JOptionPane.showConfirmDialog(this, "Would you like to copy the source files over from the original project?", title, JOptionPane.YES_NO_OPTION);
                            if (dialogResult2 == JOptionPane.YES_OPTION) {
                                this.project.saveAs(file, true, true);
                            } else {
                                this.project.saveAs(file, true, false);
                            }
                        }
                    } else {
                        this.project.saveAs(file, false, false);
                    }

                    messages.append("Saved project " + this.project.getName() + " (" + this.project.getFile() + ")\n");
                    saveMenuItem.setEnabled(true);
                } catch (Exception e) {
                    e.printStackTrace();
                    messages.append("Caught exception " + e + ", aborting\n");
                }
            }
        }
    }

    public static void main(String[] args) {
        new Main();
    }
}

class CustomFileFilter extends FileFilter {
    private String[] choices;
    private String description;

    public CustomFileFilter(String[] choices, String description) {
        super();

        this.choices = choices;
        this.description = description;
    }

    @Override
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }

        String extension = Utils.getExtension(f);
        if (extension != null) {
            for (int i = 0; i < this.choices.length; i ++) {
                if (extension.equals(this.choices[i])) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public String getDescription() {
        return description;
    }
}