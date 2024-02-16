import jdk.internal.org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.*;

import java.io.IOException;
import java.io.InputStream;

public class BytesReader {


    public static Integer[] getBytes(InputStream stream) throws Exception {
        ClassReader cr = new ClassReader(stream);
        Integer[] bytes = new Integer[14];
        cr.accept(new ClassVisitor(Opcodes.ASM5) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if ((access & Opcodes.ACC_STATIC) != 0) { // static
                    return new MethodVisitor(Opcodes.ASM5, mv) {
                        private Integer lastConstant;
                        private int count = 0;
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                            if ("java/nio/ByteBuffer".equals(owner) && "putInt".equals(name)) {
                                bytes[count] = lastConstant;
                                count++;
                            }
                            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                        }

                        @Override
                        public void visitLdcInsn(Object cst) {
                            if (cst instanceof Integer)
                                lastConstant = (Integer) cst;
                            super.visitLdcInsn(cst);
                        }
                    };
                }
                return mv;
            }
        }, 0);
        return bytes;
    }
}
