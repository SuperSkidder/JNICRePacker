package top.skidder.jlf.util;

import top.skidder.jlf.util.Util;

import java.io.*;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class JarFileHelper {
    private Enumeration<JarEntry> entries;
    private JarOutputStream out;
    JarFile jarFile;
    File file;
    public JarFileHelper(File jar) throws IOException {
        file = jar;
        jarFile = new JarFile(file.getAbsolutePath());
    }

    public void deleteOnExit(){
        file.deleteOnExit();
    }

    public Enumeration<JarEntry> getEntries(){
        if (entries == null)
            entries = jarFile.entries();
        return entries;
    }

    public void existOrCreate(String path) throws IOException {
        File file = new File(path);
        if (!file.exists()){
            if (!file.createNewFile()) {
                throw new IOException("Failed to create file: " + path);
            }
        }
    }

    public InputStream getEntryStream(JarEntry entry) throws IOException {
        return jarFile.getInputStream(entry);
    }

    public byte[] getEntryBytes(JarEntry entry) throws IOException {
        InputStream entryStream = getEntryStream(entry);
        return Util.getBytes(entryStream);
    }


    public JarOutputStream getOut(String output) throws IOException {
        existOrCreate(output);
        if (out == null) {
            FileOutputStream stream = new FileOutputStream(output);
            out = new JarOutputStream(stream);
        }
        return out;
    }


    public void close() throws IOException {
        out.close();
        jarFile.close();
    }
}
