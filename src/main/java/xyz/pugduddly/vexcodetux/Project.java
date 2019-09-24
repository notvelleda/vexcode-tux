package xyz.pugduddly.vexcodetux;

import java.io.File;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Base64;
import java.util.Iterator;

import org.json.JSONObject;

import org.apache.commons.lang3.exception.ExceptionUtils;

import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class Project {
    private String name;
    private String description;
    private String icon;
    private int slot;
    private boolean competition;
    private File file;

    public Project() {
        this.setName("Project Name");
        this.setDescription("Project Description");
        this.setSlot(1);
        this.setCompetition(true);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setDescription(String desc) {
        this.description = desc;
    }

    public String getDescription() {
        return this.description;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    public int getSlot() {
        return this.slot;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getIcon() {
        return this.icon;
    }

    public void setCompetition(boolean comp) {
        this.competition = comp;
    }

    public boolean isCompetition() {
        return this.competition;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public File getFile() {
        return this.file;
    }

    public static Project fromJSON(JSONObject json) {
        Project p = new Project();
        p.setName(json.getString("title"));
        p.setDescription(json.getString("description"));
        p.setSlot(json.getJSONObject("device").getInt("slot"));
        p.setIcon(json.getString("icon"));
        p.setCompetition(json.getBoolean("competition"));
        return p;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("title", this.getName());
        json.put("description", this.getDescription());
        json.put("device", new JSONObject());
        json.getJSONObject("device").put("slot", this.getSlot());
        json.put("icon", this.getIcon());
        json.put("language", "cpp");
        json.put("competition", this.isCompetition());
        return json;
    }

    public static Project convertVEXCodingStudioFile(File file) throws java.io.FileNotFoundException, org.apache.commons.compress.archivers.ArchiveException, IOException, Exception {
        final InputStream is = new FileInputStream(file); 
        final TarArchiveInputStream tarInputStream = (TarArchiveInputStream) new ArchiveStreamFactory().createArchiveInputStream("tar", is);
        TarArchiveEntry entry = null; 
        boolean foundJSON = false;
        Project project = new Project();
        while ((entry = (TarArchiveEntry) tarInputStream.getNextEntry()) != null) {
            if (Utils.getExtension(entry.getName()).equals("json")) {
                foundJSON = true;

                File dir = file.getParentFile();
                dir = new File(dir.toString(), file.getName().replaceFirst("[.][^.]+$", "") + "/");
                dir.mkdirs();
                file = new File(dir, file.getName().replaceFirst("[.][^.]+$", "") + ".v5code");

                JSONObject obj = new JSONObject(IOUtils.toString(tarInputStream, "UTF-8"));
                project = Project.fromJSON(obj);

                Utils.exportResource("/template/makefile", dir.toString());
                new File(dir, "vex").mkdirs();
                Utils.exportResource("/template/vex/mkenv.mk", dir.toString() + "/vex");
                Utils.exportResource("/template/vex/mkrules.mk", dir.toString() + "/vex");
                File include = new File(dir, "include");
                include.mkdirs();
                Utils.exportResource("/template/include/vex.h", dir.toString() + "/include");
                File src = new File(dir, "src");
                src.mkdirs();

                JSONObject files = obj.getJSONObject("files");
                Iterator<String> keys = files.keys();
                while(keys.hasNext()) {
                    String key = keys.next();
                    String ext2 = Utils.getExtension(key);
                    File file2;
                    String insertBefore = "";

                    if (ext2.equals("cpp") || ext2.equals("c")) {
                        file2 = new File(src, key);
                        insertBefore = "#include <vex.h>\n";
                    } else if (ext2.equals("hpp") || ext2.equals("h")) {
                        file2 = new File(include, key);
                    } else {
                        file2 = new File(dir, key);
                    }

                    BufferedWriter writer = new BufferedWriter(new FileWriter(file2));
                    writer.write(insertBefore + new String(Base64.getDecoder().decode(files.getString(key))));
                    writer.close();
                }

                BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                writer.write(project.toJSON().toString());
                writer.close();
                project.setFile(file);

                break;
            }
        }

        tarInputStream.close();

        if (!foundJSON)
            throw new IllegalStateException("No JSON file found in project");

        return project;
    }

    public static Project fromFile(File file) throws IOException {
        Project project = Project.fromJSON(new JSONObject(Utils.readFile(file)));
        project.setFile(file);
        return project;
    }

    public void save() throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(this.getFile()));
        writer.write(this.toJSON().toString());
        writer.close();
    }

    public void saveAs(File file, boolean createFiles, boolean copyOldFiles) throws Exception, java.io.IOException {
        File dir = file.getParentFile();
        if (createFiles && copyOldFiles) {
            File dir2 = this.getFile().getParentFile();
            FileUtils.copyDirectory(new File(dir2, "src/"), new File(dir, "src/"));
            FileUtils.copyDirectory(new File(dir2, "include/"), new File(dir, "include/"));
            FileUtils.copyDirectory(new File(dir2, "vex/"), new File(dir, "vex/"));
            FileUtils.copyFile(new File(dir2, "makefile"), new File(dir, "makefile"));
        } else if (createFiles && !copyOldFiles) {
            Utils.exportResource("/template/makefile", dir.toString());
            new File(dir, "vex").mkdirs();
            Utils.exportResource("/template/vex/mkenv.mk", dir.toString() + "/vex");
            Utils.exportResource("/template/vex/mkrules.mk", dir.toString() + "/vex");
            new File(dir, "include").mkdirs();
            Utils.exportResource("/template/include/vex.h", dir.toString() + "/include");
            new File(dir, "src").mkdirs();
            Utils.exportResource("/template/src/main.cpp", dir.toString() + "/src");
        }

        this.setFile(file);
        this.save();
    }

    private BuildResult buildCmd(String cmd, String type) {
        try {
            String fullCmd = "( " + cmd + " ; echo process exited with code $? ) 2>&1 | tee " + this.getFile().getParentFile() + "/" + type + ".log";
            Process proc = Runtime.getRuntime().exec(new String[] { "bash", "-c", fullCmd }, null, this.getFile().getParentFile());
            proc.waitFor();
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String s = null;
            String out = cmd + "\n";
            while ((s = stdInput.readLine()) != null) {
                out += s + "\n";
            }
            Pattern exitCodePattern = Pattern.compile("process exited with code ([0-9]+)");
            Matcher exitCodeMatcher = exitCodePattern.matcher(out);
            exitCodeMatcher.find();
            return new BuildResult(out, Integer.parseInt(exitCodeMatcher.group(1)));
        } catch (Exception e) {
            e.printStackTrace();
            return new BuildResult(ExceptionUtils.getStackTrace(e), -1);
        }
    }

    public BuildResult build() {
        try {
            String storageDir = Utils.getStorageDirectory().toString();
            String localSdkVersion = SDK.getVersion();
            String cmd = "make T=\"" + storageDir + "/" + localSdkVersion + "/\" P=\"" + this.getName() + "\" HEADERS=8.0.0";
            return buildCmd(cmd, "build");
        } catch (Exception e) {
            e.printStackTrace();
            return new BuildResult(ExceptionUtils.getStackTrace(e), -1);
        }
    }

    public BuildResult upload() {
        try {
            Utils.exportResource("/upload.py", Utils.getStorageDirectory().toString());
            String storageDir = Utils.getStorageDirectory().toString();
            String projDir = this.getFile().getParentFile().toString();
            String cmd = "python3 \"" + storageDir + "/upload.py\" \"" + projDir + "/build/" + this.getName() + ".bin\" \"" + this.getName() + "\" \"" + this.getDescription() + "\" " + this.getSlot() + " " + this.getIcon();
            return buildCmd(cmd, "upload");
        } catch (Exception e) {
            e.printStackTrace();
            return new BuildResult(ExceptionUtils.getStackTrace(e), -1);
        }
    }
}