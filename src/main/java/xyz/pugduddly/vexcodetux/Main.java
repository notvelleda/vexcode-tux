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

// Main GUI/program
public class Main extends JFrame implements ActionListener {
    public static final String title = "VEXcode Tux";
    public static final int width = 640;
    public static final int height = 400;

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
        this(null);
    }
    
    public Main(Project project) {
        // Set up GUI
        JPanel content = new JPanel();
        content.setBounds(0, 0, width, height);
        content.setPreferredSize(new Dimension(width, height));
        content.setLayout(null);

        // Message window
        messages = new JTextArea();
        messages.setFont(new Font("monospaced", Font.PLAIN, 12));
        messages.setEditable(false);
        DefaultCaret caret = (DefaultCaret) messages.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        JScrollPane scrollPane = new JScrollPane(messages);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setBounds(5, height - height / 3 * 2 + 5, width - 10, height / 3 * 2 - 10);

        content.add(scrollPane);

        // Menu bar
        menuBar = new JMenuBar();

        // File menu
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

        // Project menu
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

        // Help menu
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        JMenuItem aboutMenuItem = new JMenuItem("About", KeyEvent.VK_A);
        aboutMenuItem.addActionListener(this);
        helpMenu.add(aboutMenuItem);
        menuBar.add(helpMenu);

        this.setJMenuBar(menuBar);

        // Project name text field
        projectName = new JTextField();
        projectName.setBounds(5, 5, width / 3 - 10, 32);
        projectName.setText("Project Name");
        content.add(projectName);

        // Project slot spinner thing
        SpinnerModel slotModel = new SpinnerNumberModel(1, 1, 8, 1);     
        slotSpinner = new JSpinner(slotModel);
        slotSpinner.setBounds(width / 3 + 5, 5, width / 6 - 10, 32);
        content.add(slotSpinner);

        // Project description text field
        projectDesc = new JTextField();
        projectDesc.setBounds(5, 48, width / 2 - 10, 32);
        projectDesc.setText("Project Description");
        content.add(projectDesc);

        // Checkbox to select whether project is competition or not
        isCompetition = new JCheckBox("Competition");
        isCompetition.setBounds(25, 96, 200, 25);
        content.add(isCompetition);
        
        // Disable all GUI elements
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

        // Create open file chooser object & populate with filters
        openFileChooser = new JFileChooser();
        openFileChooser.addChoosableFileFilter(new CustomFileFilter(new String[] { "v5code", "vex" }, "All compatible files"));
        openFileChooser.addChoosableFileFilter(new CustomFileFilter(new String[] { "v5code" }, "VEXcode V5 Text files (.v5code)"));
        openFileChooser.addChoosableFileFilter(new CustomFileFilter(new String[] { "vex" }, "VEX Coding Studio files (.vex)"));
        openFileChooser.setAcceptAllFileFilterUsed(false);

        // Create save file chooser object & populate with filter
        saveFileChooser = new JFileChooser();
        saveFileChooser.addChoosableFileFilter(new CustomFileFilter(new String[] { "v5code" }, "VEXcode V5 Text files (.v5code)"));
        saveFileChooser.setAcceptAllFileFilterUsed(false);
        
        add(content);
        pack();

        // Stuff
        setTitle(this.title);
        setLocationRelativeTo(null);
        setLayout(null);
        setVisible(true);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Check for SDK update then download and install one if available
        messages.append("Checking for SDK update...\n");
        try {
            if (SDK.shouldUpdate()) {
                messages.append("Updating SDK...\n");
                SDK.update();
            }
            messages.append("Done\n");
        } catch (Exception e) {
            e.printStackTrace();
            messages.append("Caught exception " + e + ", aborting\n");
        } finally {
            newMenuItem.setEnabled(true);
            openMenuItem.setEnabled(true);
        }

        if (project != null) {
            this.project = project;
            this.updateGUI();
            this.enableGUI();

            messages.append("Opened project " + project.getName() + " (" + project.getFile() + ")\n");
        }
    }

    // Disable GUI elements
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

    // Enable GUI elements
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

    // Update project with values from GUI
    private void updateProject() {
        this.project.setName(this.projectName.getText());
        this.project.setDescription(this.projectDesc.getText());
        this.project.setSlot((int) this.slotSpinner.getValue());
        this.project.setCompetition(this.isCompetition.isSelected());
    }

    // Update GUI with values from project
    private void updateGUI() {
        this.projectName.setText(this.project.getName());
        this.projectDesc.setText(this.project.getDescription());
        this.slotSpinner.setValue(this.project.getSlot());
        this.isCompetition.setSelected(this.project.isCompetition());
    }

    // GUI action handler
    public void actionPerformed(ActionEvent event) {
        if (event.getActionCommand().equals("About")) {
            // Show about dialog box
            JOptionPane.showMessageDialog(this, title + "\nCopyleft (ↄ) 2019 Pugduddly\n\nUsing SDK version " + SDK.getVersion());
        } else if (event.getActionCommand().equals("New")) {
            // Create a new project
            this.enableGUI();
            saveMenuItem.setEnabled(false);
            buildMenuItem.setEnabled(false);
            uploadMenuItem.setEnabled(false);

            this.project = new Project();
            this.updateGUI();
        } else if (event.getActionCommand().equals("Open")) {
            // Open project from file
            int returnVal = openFileChooser.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = openFileChooser.getSelectedFile();
                String ext = Utils.getExtension(file);

                try {
                    if (ext.equals("v5code")) {
                        // VEXcode V5 Text file
                        this.project = Project.fromFile(file);
                        this.updateGUI();
                    } else if (ext.equals("vex")) {
                        // VEX Coding Studio file
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

                this.enableGUI();

                messages.append("Opened project " + project.getName() + " (" + file + ")\n");
            }
        } else if (event.getActionCommand().equals("Build")) {
            // Build project
            messages.append("Building project " + projectName.getText() + "...\n");
            this.disableGUI();
            new Thread() {
                @Override
                public void run() {
                    Main.this.updateProject();
                    messages.append(Main.this.project.build().getLog());
                    messages.append("Done\n");
                    Main.this.enableGUI();
                }
            }.start();
        } else if (event.getActionCommand().equals("Upload to Brain")) {
            // Build project, then upload if build is successful
            messages.append("Building project " + projectName.getText() + "...\n");
            this.disableGUI();
            new Thread() {
                @Override
                public void run() {
                    Main.this.updateProject();
                    BuildResult result = Main.this.project.build();
                    messages.append(result.getLog());
                    messages.append("Done\n");
                    if (result.getExitCode() == 0) {
                        messages.append("Uploading project " + projectName.getText() + "...\n");
                        BuildResult res = Main.this.project.upload();
                        messages.append(res.getLog());
                        messages.append("Done\n");
                        if (res.getExitCode() != 0) {
                            JOptionPane.showMessageDialog(Main.this, "Upload failed! Make sure you have a V5 Brain plugged in and try again.");
                        }
                    } else {
                        messages.append("Build failed, so not uploading project.\n");
                    }
                    Main.this.enableGUI();
                }
            }.start();
        } else if (event.getActionCommand().equals("Save")) {
            // Save project
            try {
                this.updateProject();
                this.project.save();
                messages.append("Saved project " + projectName.getText() + " (" + this.project.getFile() + ")\n");
            } catch (Exception e) {
                e.printStackTrace();
                messages.append("Caught exception " + e + ", aborting\n");
            }
        } else if (event.getActionCommand().equals("Save As")) {
            // Save project to new location
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

    private static void help() {
        System.out.println("Usage: vexcodetux [option]");
        System.out.println("Long options:");
        System.out.println("\t--help\t\t\t: help");
        System.out.println("\t--build\t\t\t: build project");
        System.out.println("\t--convert <path>\t: convert VEX Coding Studio project");
        System.out.println("\t--description <desc>\t: set project description");
        System.out.println("\t--gui\t\t\t: open project in GUI");
        System.out.println("\t--name <name>\t\t: set project name");
        System.out.println("\t--project\t\t: specify project file");
        System.out.println("\t--slot\t\t\t: set project slot");
        System.out.println("\t--upload\t\t: upload project to Brain");
        System.out.println("Short options:");
        System.out.println("\t-bchnpu");
    }

    public static void main(String[] args) {
        try {
            System.out.println(title + "\nCopyleft (ↄ) 2019 Pugduddly\n");
            System.out.println("Using SDK version " + SDK.getVersion() + "\n");

            // hacky solution to fix antialiasing
            System.setProperty("awt.useSystemAAFontSettings", "on");
            System.setProperty("swing.aatext", "true");
        
            try {
                // Ensure GTK look and feel is used on Linux systems
                UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ee) {
                    ee.printStackTrace();
                }
            }
            
            if (args.length == 0) {
                new Main();
            } else {
                // Got args, it's parsing time

                // Options
                Project staticProject = null;
                BuildResult buildResult = null;
                BuildResult uploadResult = null;
                boolean foundArgs = false;
                boolean shouldOpenGUI = false;

                for (int i = 0; i < args.length; i ++) {
                    if (args[i].startsWith("--")) {
                        String arg = args[i].substring(2);
                        if (arg.equals("help")) { // help
                            help();
                        } else if (arg.equals("project")) { // set project
                            staticProject = Project.fromFile(new File(args[++ i]));
                        } else if (arg.equals("convert")) { // convert
                            staticProject = Project.convertVEXCodingStudioFile(new File(args[++ i]));
                        } else if (arg.equals("build")) { // build
                            if (staticProject == null) {
                                System.out.println("No project! Specify a project file with -p <projectFile>");
                            } else {
                                buildResult = staticProject.build();
                                System.out.println(buildResult.getLog());
                            }
                        } else if (arg.equals("upload")) { // upload
                            if (staticProject == null) {
                                System.out.println("No project! Specify a project file with -p <projectFile>");
                            } else {
                                if (buildResult == null) {
                                    buildResult = staticProject.build();
                                    System.out.println(buildResult.getLog());
                                }
                                if (buildResult.getExitCode() == 0) {
                                    uploadResult = staticProject.upload();
                                    System.out.println(uploadResult.getLog());
                                } else {
                                    System.out.println("Build failed, so not uploading project.");
                                }
                            }
                        } else if (arg.equals("name")) { // name
                            if (staticProject == null) {
                                System.out.println("No project! Specify a project file with -p <projectFile>");
                            } else {
                                staticProject.setName(args[++ i]);
                                staticProject.save();
                            }
                        } else if (arg.equals("description")) { // description
                            if (staticProject == null) {
                                System.out.println("No project! Specify a project file with -p <projectFile>");
                            } else {
                                staticProject.setDescription(args[++ i]);
                                staticProject.save();
                            }
                        } else if (arg.equals("slot")) { // slot
                            if (staticProject == null) {
                                System.out.println("No project! Specify a project file with -p <projectFile>");
                            } else {
                                staticProject.setSlot(Integer.parseInt(args[++ i]));
                                staticProject.save();
                            }
                        } else if (arg.equals("gui")) { // open in gui
                            shouldOpenGUI = true;
                        } else {
                            System.out.println("Unrecognized option " + arg);
                            return;
                        }
                    } else if (args[i].startsWith("-")) {
                        String _arg = args[i];
                        for (int j = 1; j < _arg.length(); j ++) {
                            char arg = _arg.charAt(j);
                            if (arg == 'h') { // help
                                help();
                            } else if (arg == 'p') { // set project
                                staticProject = Project.fromFile(new File(args[++ i]));
                            } else if (arg == 'c') { // convert
                                staticProject = Project.convertVEXCodingStudioFile(new File(args[++ i]));
                            } else if (arg == 'b') { // build
                                if (staticProject == null) {
                                    System.out.println("No project! Specify a project file with -p <projectFile>");
                                } else {
                                    buildResult = staticProject.build();
                                    System.out.println(buildResult.getLog());
                                }
                            } else if (arg == 'u') { // upload
                                if (staticProject == null) {
                                    System.out.println("No project! Specify a project file with -p <projectFile>");
                                } else {
                                    if (buildResult == null) {
                                        buildResult = staticProject.build();
                                        System.out.println(buildResult.getLog());
                                    }
                                    if (buildResult.getExitCode() == 0) {
                                        uploadResult = staticProject.upload();
                                        System.out.println(uploadResult.getLog());
                                    } else {
                                        System.out.println("Build failed, so not uploading project.");
                                    }
                                }
                            } else if (arg == 'n') { // name
                                if (staticProject == null) {
                                    System.out.println("No project! Specify a project file with -p <projectFile>");
                                } else {
                                    staticProject.setName(args[++ i]);
                                    staticProject.save();
                                }
                            } else if (arg == 'd') { // description
                                if (staticProject == null) {
                                    System.out.println("No project! Specify a project file with -p <projectFile>");
                                } else {
                                    staticProject.setDescription(args[++ i]);
                                    staticProject.save();
                                }
                            } else if (arg == 's') { // slot
                                if (staticProject == null) {
                                    System.out.println("No project! Specify a project file with -p <projectFile>");
                                } else {
                                    staticProject.setSlot(Integer.parseInt(args[++ i]));
                                    staticProject.save();
                                }
                            } else if (arg == 'g') { // open in gui
                                shouldOpenGUI = true;
                            } else {
                                System.out.println("Unrecognized option " + arg);
                                return;
                            }
                        }
                    } else {
                        System.out.println("Unrecognized option " + args[i]);
                        return;
                    }
                }

                if (shouldOpenGUI) {
                    new Main(staticProject);
                }

                if (uploadResult != null) {
                    System.exit(uploadResult.getExitCode());
                } else if (buildResult != null) {
                    System.exit(buildResult.getExitCode());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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