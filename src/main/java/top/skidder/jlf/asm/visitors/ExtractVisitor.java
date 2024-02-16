package top.skidder.jlf.asm.visitors;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.objectweb.asm.Opcodes.*;

public class ExtractVisitor extends ClassVisitor {

        public ExtractVisitor(ClassVisitor classVisitor) {
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