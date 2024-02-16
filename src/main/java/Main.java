import org.objectweb.asm.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import static org.objectweb.asm.Opcodes.*;


public class Main {
    private static InputStream nativeFile;

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the path to the jar file: ");
        String s = scanner.nextLine();
        JarFile jarFile = new JarFile(s);
        new File("output.jar").createNewFile();
        FileOutputStream stream = new FileOutputStream("output.jar");
        JarOutputStream out = new JarOutputStream(stream);
        Enumeration<JarEntry> entries = jarFile.entries();
        Integer[] bytes = new Integer[0];
        String loaderName = "";

        String property = System.getProperty("user.dir");

        JNICExtractor.extract(s);

        nativeFile = Files.newInputStream(Paths.get(property + "/jnic.dll"));

        while (entries.hasMoreElements()) {
            JarEntry jarEntry = entries.nextElement();
            if (jarEntry.getName().endsWith(".class")) {
                if (jarEntry.getName().contains("JNICLoader")) {
                    loaderName = jarEntry.getName();
                    System.out.println("transform loader " + loaderName);
                    InputStream inputStream = jarFile.getInputStream(jarEntry);
                    bytes = BytesReader.getBytes(inputStream);
                    System.out.println(Arrays.toString(bytes));
                } else if (!jarEntry.getName().contains("dev/jnic")) {
                    InputStream is = jarFile.getInputStream(jarEntry);
                    ClassReader cr = new ClassReader(is);
                    ClassWriter cw = new ClassWriter(cr,0);
                    cr.accept(new MyClassVisitor(cw),0);
                    out.putNextEntry(new JarEntry(jarEntry.getName()));
                    out.write(cw.toByteArray());
                    out.closeEntry();
                }
            } else if (jarEntry.getName().endsWith(".dat") && jarEntry.getName().contains("dev/jnic")){

            }else{
                out.putNextEntry(jarEntry);
                out.write(getBytes(jarFile.getInputStream(jarEntry)));
                out.closeEntry();
            }
        }


        out.putNextEntry(new JarEntry("jnic.dll"));
        out.write(getBytes(nativeFile));

        // 添加自定义的loader
        JarEntry loader = new JarEntry(loaderName);
        out.putNextEntry(loader);
        ClassWriter loader_cw = new ClassWriter(0);
        String cleanName = loaderName.replace(".class", "");
        loader_cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, cleanName, null, "java/lang/Object", null);
        loader_cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,"z","Ljava/nio/ByteBuffer;",null,null);

        visitLoadLibrary(loader_cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "loadLibrary", "()V", null, null),cleanName);

        MethodVisitor mv = loader_cw.visitMethod(Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC, "init", "()V", null, null);
        mv.visitCode();

        mv.visitLdcInsn(222);
        mv.visitMethodInsn(INVOKESTATIC,"java/nio/ByteBuffer","allocateDirect","(I)Ljava/nio/ByteBuffer;",false);
        mv.visitFieldInsn(Opcodes.GETSTATIC,"java/nio/ByteOrder","LITTLE_ENDIAN","Ljava/nio/ByteOrder;");
        mv.visitMethodInsn(INVOKEVIRTUAL,"java/nio/ByteBuffer","order","(Ljava/nio/ByteOrder;)Ljava/nio/ByteBuffer;",false);
        mv.visitFieldInsn(Opcodes.PUTSTATIC,cleanName,"z","Ljava/nio/ByteBuffer;");

        for (Integer aByte : bytes) {
            if (aByte == null)
                continue;
            mv.visitFieldInsn(Opcodes.GETSTATIC, cleanName, "z", "Ljava/nio/ByteBuffer;");
            mv.visitLdcInsn(aByte);
            mv.visitMethodInsn(INVOKEVIRTUAL,"java/nio/ByteBuffer","putInt","(I)Ljava/nio/ByteBuffer;",false);
            mv.visitInsn(Opcodes.POP);
        }

        mv.visitMethodInsn(INVOKESTATIC, cleanName, "loadLibrary", "()V", false);

        mv.visitMaxs(3,2);
        mv.visitInsn(RETURN);
        mv.visitEnd();

        loader_cw.visitEnd();
        out.write(loader_cw.toByteArray());
        out.flush();
        out.finish();
        out.close();
        jarFile.close();
    }

    private static void visitLoadLibrary(MethodVisitor methodVisitor,String cleanName) {
        {
            methodVisitor.visitCode();
            Label label0 = new Label();
            Label label1 = new Label();
            Label label2 = new Label();
            methodVisitor.visitTryCatchBlock(label0, label1, label2, "java/io/IOException");
            Label label3 = new Label();
            methodVisitor.visitLabel(label3);
            methodVisitor.visitLineNumber(15, label3);
            methodVisitor.visitLdcInsn(Type.getType("L"+cleanName+";"));
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
            methodVisitor.visitLdcInsn("jnic.dll");
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/ClassLoader", "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;", false);
            methodVisitor.visitVarInsn(ASTORE, 0);
            methodVisitor.visitLabel(label0);
            methodVisitor.visitLineNumber(18, label0);
            methodVisitor.visitLdcInsn("jnic");
            methodVisitor.visitLdcInsn(".dll");
            methodVisitor.visitMethodInsn(INVOKESTATIC, "java/io/File", "createTempFile", "(Ljava/lang/String;Ljava/lang/String;)Ljava/io/File;", false);
            methodVisitor.visitVarInsn(ASTORE, 1);
            Label label4 = new Label();
            methodVisitor.visitLabel(label4);
            methodVisitor.visitLineNumber(19, label4);
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/File", "deleteOnExit", "()V", false);
            Label label5 = new Label();
            methodVisitor.visitLabel(label5);
            methodVisitor.visitLineNumber(20, label5);
            methodVisitor.visitTypeInsn(NEW, "java/io/FileOutputStream");
            methodVisitor.visitInsn(DUP);
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/io/FileOutputStream", "<init>", "(Ljava/io/File;)V", false);
            methodVisitor.visitVarInsn(ASTORE, 2);
            Label label6 = new Label();
            methodVisitor.visitLabel(label6);
            methodVisitor.visitLineNumber(21, label6);
            methodVisitor.visitIntInsn(SIPUSH, 1024);
            methodVisitor.visitIntInsn(NEWARRAY, T_BYTE);
            methodVisitor.visitVarInsn(ASTORE, 3);
            Label label7 = new Label();
            methodVisitor.visitLabel(label7);
            methodVisitor.visitLineNumber(23, label7);
            methodVisitor.visitFrame(Opcodes.F_FULL, 4, new Object[]{"java/io/InputStream", "java/io/File", "java/io/FileOutputStream", "[B"}, 0, new Object[]{});
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitVarInsn(ALOAD, 3);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/InputStream", "read", "([B)I", false);
            methodVisitor.visitInsn(DUP);
            methodVisitor.visitVarInsn(ISTORE, 4);
            Label label8 = new Label();
            methodVisitor.visitLabel(label8);
            methodVisitor.visitInsn(ICONST_M1);
            Label label9 = new Label();
            methodVisitor.visitJumpInsn(IF_ICMPEQ, label9);
            Label label10 = new Label();
            methodVisitor.visitLabel(label10);
            methodVisitor.visitLineNumber(24, label10);
            methodVisitor.visitVarInsn(ALOAD, 2);
            methodVisitor.visitVarInsn(ALOAD, 3);
            methodVisitor.visitInsn(ICONST_0);
            methodVisitor.visitVarInsn(ILOAD, 4);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/FileOutputStream", "write", "([BII)V", false);
            methodVisitor.visitJumpInsn(GOTO, label7);
            methodVisitor.visitLabel(label9);
            methodVisitor.visitLineNumber(26, label9);
            methodVisitor.visitFrame(Opcodes.F_APPEND, 1, new Object[]{Opcodes.INTEGER}, 0, null);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/InputStream", "close", "()V", false);
            Label label11 = new Label();
            methodVisitor.visitLabel(label11);
            methodVisitor.visitLineNumber(27, label11);
            methodVisitor.visitVarInsn(ALOAD, 2);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/FileOutputStream", "close", "()V", false);
            Label label12 = new Label();
            methodVisitor.visitLabel(label12);
            methodVisitor.visitLineNumber(28, label12);
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/File", "getAbsolutePath", "()Ljava/lang/String;", false);
            methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/System", "load", "(Ljava/lang/String;)V", false);
            methodVisitor.visitLabel(label1);
            methodVisitor.visitLineNumber(31, label1);
            Label label13 = new Label();
            methodVisitor.visitJumpInsn(GOTO, label13);
            methodVisitor.visitLabel(label2);
            methodVisitor.visitLineNumber(29, label2);
            methodVisitor.visitFrame(Opcodes.F_FULL, 1, new Object[]{"java/io/InputStream"}, 1, new Object[]{"java/io/IOException"});
            methodVisitor.visitVarInsn(ASTORE, 1);
            Label label14 = new Label();
            methodVisitor.visitLabel(label14);
            methodVisitor.visitLineNumber(30, label14);
            methodVisitor.visitTypeInsn(NEW, "java/lang/RuntimeException");
            methodVisitor.visitInsn(DUP);
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/Throwable;)V", false);
            methodVisitor.visitInsn(ATHROW);
            methodVisitor.visitLabel(label13);
            methodVisitor.visitLineNumber(33, label13);
            methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            methodVisitor.visitInsn(RETURN);
            Label label15 = new Label();
            methodVisitor.visitLabel(label15);
            methodVisitor.visitLocalVariable("jnic", "Ljava/io/File;", null, label4, label1, 1);
            methodVisitor.visitLocalVariable("out", "Ljava/io/FileOutputStream;", null, label6, label1, 2);
            methodVisitor.visitLocalVariable("buffer", "[B", null, label7, label1, 3);
            methodVisitor.visitLocalVariable("len", "I", null, label8, label1, 4);
            methodVisitor.visitLocalVariable("e", "Ljava/io/IOException;", null, label14, label13, 1);
            methodVisitor.visitLocalVariable("url", "Ljava/io/InputStream;", null, label0, label15, 0);
            methodVisitor.visitMaxs(4, 5);
            methodVisitor.visitEnd();
        }
    }

    public static byte[] getBytes(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;

        while ((len = is.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }

        is.close();
        return out.toByteArray();
    }

    static class MyClassVisitor extends ClassVisitor {
        MyClassVisitor(ClassVisitor cv) {
            super(Opcodes.ASM9, cv);
        }

        @Override
        public void visit(int version, int access, String name, String signature,
                          String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            return new MethodVisitor(Opcodes.ASM9, mv) {
                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                }
            };
        }
    }
}
