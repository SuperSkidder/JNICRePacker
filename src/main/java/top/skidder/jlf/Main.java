package top.skidder.jlf;

import org.objectweb.asm.*;
import top.skidder.jlf.asm.JNICASM;
import top.skidder.jlf.asm.visitors.AuthVisitor;
import top.skidder.jlf.util.JarFileHelper;
import top.skidder.jlf.util.UserInput;
import top.skidder.jlf.util.Util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;


public class Main {
    private static InputStream nativeFile;

    static UserInput userInput = new UserInput();
    public static Integer[] authentification = new Integer[0];
    public static String loaderCls = "";

    public static void main(String[] args) throws Exception {
        String s = userInput.input("Enter the path to the jar file");

        JarFileHelper input = new JarFileHelper(new File(s));
        JarOutputStream out = input.getOut("output.jar");
        Enumeration<JarEntry> entries = input.getEntries();


        while (entries.hasMoreElements()) {
            JarEntry jarEntry = entries.nextElement();
            if (jarEntry.getName().endsWith(".class")) {
                if (jarEntry.getName().contains("JNICLoader")) {
                    loaderCls = jarEntry.getName();
                    InputStream inputStream = input.getEntryStream(jarEntry);
                    authentification = AuthVisitor.getBytes(inputStream);

                    System.out.println("Auth Visitor: get auth data -> " + Arrays.toString(authentification));
                } else if (!jarEntry.getName().contains("dev/jnic")) {
                    out.putNextEntry(new JarEntry(jarEntry.getName()));
                    out.write(input.getEntryBytes(jarEntry));
                    out.closeEntry();
                }
            }else if (!(jarEntry.getName().endsWith(".dat") && jarEntry.getName().contains("dev/jnic"))){
                out.putNextEntry(new JarEntry(jarEntry.getName()));
                out.write(input.getEntryBytes(jarEntry));
                out.closeEntry();
            }
        }


        // 在这里将jnic的库 dump出来
        // dump jnic native library here
        String property = System.getProperty("user.dir");
        JNICExtractor.extract(s);
        nativeFile = Files.newInputStream(Paths.get(property + "/jnic.dll"));


        // 在这里添加dump出来的本机库
        // add dumped native here
        out.putNextEntry(new JarEntry("jnic.dll"));
        out.write(Util.getBytes(nativeFile));

        // 添加自定义的loader
        // add a custom loader here
        JarEntry loader = new JarEntry(loaderCls);
        out.putNextEntry(loader);
        ClassWriter loader_cw = JNICASM.getCustomLoader();
        out.write(loader_cw.toByteArray());

        input.close();
    }
}
