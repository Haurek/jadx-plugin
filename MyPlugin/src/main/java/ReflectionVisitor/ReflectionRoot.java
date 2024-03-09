package ReflectionVisitor;

import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.InsnNode;

import java.util.ArrayList;

public class ReflectionRoot extends ReflectionNode {
    private ClassNode classNode;

    public ReflectionRoot() {
        super();
        classNode = null;
    }

    public ReflectionRoot(ReflectionType t, InsnArg arg0, InsnNode insn) {
        super(t, arg0, insn);
    }

    public ReflectionRoot(ReflectionType t, ArrayList<InsnArg> as, InsnNode insn) {
        super(t, as, insn);
    }

    public void setClassNode(ClassNode node) {
        classNode = node;
    }

    public ClassNode getClassNode() {
        return classNode;
    }
}