import org.objectweb.asm.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import static org.objectweb.asm.Opcodes.*;

public class JNICExtractor {
    public static void extract(String path) throws IOException {
        File file = new File(path);
        JarFile jarFile = new JarFile(file);
        String loaderClass = "";
        FileOutputStream stream = new FileOutputStream("output_dump.jar");
        JarOutputStream out = new JarOutputStream(stream);
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            byte[] bytes = Main.getBytes(jarFile.getInputStream(entry));
            if (entry.getName().endsWith("JNICLoader.class")) {
                loaderClass = entry.getName();
                ClassReader cr = new ClassReader(jarFile.getInputStream(entry));
                ClassWriter cw = new ClassWriter(cr,ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                cr.accept(new JNICModifier(cw), 0);
                out.putNextEntry(new JarEntry(entry.getName()));
                out.write(cw.toByteArray());
                out.closeEntry();
            }else{
                out.putNextEntry(new JarEntry(entry.getName()));
                out.write(bytes);
                out.closeEntry();
            }
        }
        out.flush();
        out.finish();
        out.close();

        // execute the jar using reflection
        String property = System.getProperty("user.dir");
//        ClassLoader loader = new java.net.URLClassLoader(new java.net.URL[]{new File(property+"/output_dump.jar").toURI().toURL()});
        URLClassLoader ucl = (URLClassLoader) JNICExtractor.class.getClassLoader();
        ClassLoader loader = new URLClassLoader(new java.net.URL[]{new File(property+"/output_dump.jar").toURI().toURL()}, ucl);
        Thread.currentThread().setContextClassLoader(loader);
        Class<?> cls = null;
        try {
            cls = Class.forName(loaderClass.replace(".class","").replace("/","."), true, loader);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    static class JNICModifier extends ClassVisitor {

        protected JNICModifier(ClassVisitor classVisitor) {
            super(ASM9, classVisitor);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            if ("<clinit>".equals(name)){
                return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                        if ("java/io/File".equals(owner) && "createTempFile".equals(name)){
                            String property = System.getProperty("user.dir");
                            visitLdcInsn(property + "\\jnic.dll");
                            visitInsn(ICONST_0);
                            visitTypeInsn(ANEWARRAY, "java/lang/String");
                            visitMethodInsn(INVOKESTATIC, "java/nio/file/Paths", "get", "(Ljava/lang/String;[Ljava/lang/String;)Ljava/nio/file/Path;", false);
                            visitInsn(ICONST_0);
                            visitTypeInsn(ANEWARRAY, "java/nio/file/attribute/FileAttribute");
                            visitMethodInsn(INVOKESTATIC, "java/nio/file/Files", "createFile", "(Ljava/nio/file/Path;[Ljava/nio/file/attribute/FileAttribute;)Ljava/nio/file/Path;", false);
                            visitMethodInsn(INVOKEINTERFACE, "java/nio/file/Path", "toFile", "()Ljava/io/File;", true);
                        }else {
                            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                        }
                    }
                };
            }else{
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }
        }
    }
}
