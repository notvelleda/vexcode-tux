package xyz.pugduddly.vexcodetux;

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

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import java.net.HttpURLConnection;
import java.net.URL;

import java.nio.file.Paths;
import java.nio.file.Files;

public class Utils {
    private static File storageDir;
    
    public static String exportResource(String resourceName, String path) throws Exception {
        InputStream stream = null;
        OutputStream resStreamOut = null;
        try {
            stream = Main.class.getResourceAsStream(resourceName);
            if (stream == null) {
                throw new Exception("Cannot get resource \"" + resourceName + "\" from Jar file.");
            }

            int readBytes;
            byte[] buffer = new byte[4096];
            resStreamOut = new FileOutputStream(new File(path + '/' + new File(resourceName).getName()));
            while ((readBytes = stream.read(buffer)) > 0) {
                resStreamOut.write(buffer, 0, readBytes);
            }
        } catch (Exception ex) {
            throw ex;
        } finally {
            stream.close();
            if (resStreamOut != null)
                resStreamOut.close();
        }

        return new File(path + '/' + new File(resourceName).getName()).toString();
    }

    public static String getExtension(File f) {
        String s = f.getName();
        String ext = null;
        int i = s.lastIndexOf('.');

        if (i > 0 &&  i < s.length() - 1) {
            ext = s.substring(i+1).toLowerCase();
        }
        return ext;
    }

    public static String getExtension(String s) {
        String ext = null;
        int i = s.lastIndexOf('.');

        if (i > 0 &&  i < s.length() - 1) {
            ext = s.substring(i+1).toLowerCase();
        }
        return ext;
    }

    public static String getURL(String url) throws java.net.MalformedURLException, IOException, java.net.ProtocolException {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();

        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", Main.title);

        //int responseCode = con.getResponseCode();
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }

    public static int downloadURL(String url, String save) throws java.net.MalformedURLException, IOException, java.net.ProtocolException {
        BufferedInputStream in = new BufferedInputStream(new URL(url).openStream());
        FileOutputStream out = new FileOutputStream(save);
        byte dataBuffer[] = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
            out.write(dataBuffer, 0, bytesRead);
        }
        return bytesRead;
    }

    public static String readFile(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }

    public static String readFile(File file) throws IOException {
        return new String(Files.readAllBytes(Paths.get(file.toString())));
    }

    public static void unzip(String fileZip, String destDirStr) throws IOException {
        File destDir = new File(destDirStr);
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            File newFile = newFile(destDir, zipEntry);
            if (zipEntry.isDirectory()) {
                newFile.mkdirs();
            } else {
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
    }
     
    public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());
         
        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();
         
        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }
        
        return destFile;
    }

    public static File getStorageDirectory() {
        if (storageDir == null) {
            String string = "vexcodetux";
            String string2 = System.getProperty("user.home", ".");
            switch (getOSType()) {
                case "unix": 
                case "linux": 
                case "solaris": 
                case "sunos":
                    storageDir = new File(string2, '.' + string + '/');
                    break;
                case "win":
                    String string3 = System.getenv("APPDATA");
                    if (string3 != null) {
                        storageDir = new File(string3, "." + string + '/');
                        break;
                    }
                    storageDir = new File(string2, '.' + string + '/');
                    break;
                case "mac":
                    storageDir = new File(string2, "Library/Application Support/" + string);
                    break;
                default:
                    storageDir = new File(string2, string + '/');
            }
            if (!storageDir.exists() && !storageDir.mkdirs()) {
                throw new RuntimeException("The working directory could not be created: " + storageDir);
            }
        }
        return storageDir;
    }

    public static String getOSType() {
        String string = System.getProperty("os.name").toLowerCase();
        if (string.contains("win")) {
            return "win";
        }
        if (string.contains("mac")) {
            return "mac";
        }
        if (string.contains("solaris")) {
            return "solaris";
        }
        if (string.contains("sunos")) {
            return "sunos";
        }
        if (string.contains("linux")) {
            return "linux";
        }
        if (string.contains("unix")) {
            return "unix";
        }
        return "other";
    }
}