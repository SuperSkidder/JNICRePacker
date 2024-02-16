package top.skidder.jlf;

import org.objectweb.asm.*;
import top.skidder.jlf.asm.visitors.ExtractVisitor;
import top.skidder.jlf.util.JarFileHelper;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class JNICExtractor {
    public static void extract(String path) throws IOException {
        File file = new File(path);
        JarFileHelper jarFile = new JarFileHelper(file);
        JarOutputStream out = jarFile.getOut("output_dump.jar");

        Enumeration<JarEntry> entries = jarFile.getEntries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.getName().endsWith("JNICLoader.class")) {
                ClassReader cr = new ClassReader(jarFile.getEntryStream(entry));
                ClassWriter cw = new ClassWriter(cr,ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                cr.accept(new ExtractVisitor(cw), 0);
                out.putNextEntry(new JarEntry(entry.getName()));
                out.write(cw.toByteArray());
                out.closeEntry();
            }else{
                out.putNextEntry(new JarEntry(entry.getName()));
                out.write(jarFile.getEntryBytes(entry));
                out.closeEntry();
            }
        }
        jarFile.close();

        String property = System.getProperty("user.dir");


        ClassLoader loader = getClassLoader(property);
        Thread.currentThread().setContextClassLoader(loader);
        Class<?> cls = null;
        try {
            cls = Class.forName(Main.loaderCls.replace(".class","").replace("/","."), true, loader);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static ClassLoader getClassLoader(String property) throws MalformedURLException {
        // 如果已经存在native，删除
        // delete native if existed
        File file1 = new File(property + "/jnic.dll");
        if (file1.exists())
            if (!file1.delete())
                throw new RuntimeException("Failed to delete file: " + property + "/jnic.dll");

        // 用反射执行jar文件
        // execute the jar using reflection
        URLClassLoader ucl = (URLClassLoader) JNICExtractor.class.getClassLoader();
        ClassLoader loader = new URLClassLoader(new java.net.URL[]{new File(property +"/output_dump.jar").toURI().toURL()}, ucl);
        return loader;
    }
}
