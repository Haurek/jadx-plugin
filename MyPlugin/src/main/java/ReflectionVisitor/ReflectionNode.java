package ReflectionVisitor;

import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.InsnNode;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class ReflectionNode {
    private ReflectionType type;
    private InsnNode insnNode;
    private ArrayList<InsnArg> args;
    private RegisterArg result;

    private CopyOnWriteArrayList<ReflectionNode> successors;

    public ReflectionNode() {
        type = null;
        args = null;
        result = null;
        insnNode = null;
        successors = new CopyOnWriteArrayList<>();
    }

    public ReflectionNode(ReflectionType t, InsnArg arg0, InsnNode insn) {
        type = t;
        args = new ArrayList<InsnArg>();
        args.add(arg0);
        insnNode = insn;
        successors = new CopyOnWriteArrayList<>();
    }

    public ReflectionNode(ReflectionType t, ArrayList<InsnArg> as, InsnNode insn) {
        args = as;
        type = t;
        insnNode = insn;
        successors = new CopyOnWriteArrayList<>();
    }

    public void setType(ReflectionType type) {
        this.type = type;
    }

    public void setArgs(ArrayList<InsnArg> args) {
        this.args = args;
    }

    public void setResult(RegisterArg res) {
        this.result = res;
    }

    public void setInsnNode(InsnNode insn) {
        this.insnNode = insn;
    }

    public InsnNode getInsnNode() {
        return this.insnNode;
    }

    public ReflectionType getType() {
        return this.type;
    }

    public ArrayList<InsnArg> getArgs() {
        return this.args;
    }

    public RegisterArg getResult() {
        return this.result;
    }

    public void addSuccessor(ReflectionNode successor) {
        successors.add(successor);
    }

    public boolean removeSuccessor(ReflectionNode node) {
        return successors.remove(node);
    }

    public CopyOnWriteArrayList<ReflectionNode> getSuccessors() {
        return successors;
    }

    public ReflectionNode getSuccessorAt(int i) {
        if (i >= 0 && i < successors.size()) {
            return successors.get(i);
        }
        return null;
    }
}