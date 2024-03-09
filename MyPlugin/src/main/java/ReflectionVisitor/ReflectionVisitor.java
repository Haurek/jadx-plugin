package ReflectionVisitor;
// jadx.core
import jadx.api.JadxDecompiler;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.*;
import jadx.core.dex.instructions.args.*;
import jadx.core.dex.instructions.mods.ConstructorInsn;
import jadx.core.dex.nodes.*;
import jadx.core.dex.visitors.*;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.InsnRemover;

// java
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class ReflectionVisitor extends AbstractVisitor {
    private JadxDecompiler jadx;
    private ReflectionGraph graph;

    public ReflectionVisitor() {
        graph = new ReflectionGraph();
        jadx = null;
    }

    public ReflectionVisitor(JadxDecompiler j) {
        graph = new ReflectionGraph();
        jadx = j;
    }

    @Override
    public void visit(MethodNode mth) {
        for (BlockNode blockNode : mth.getBasicBlocks()) {
            for (InsnNode insnNode : blockNode.getInstructions()) {
                ArrayList<ReflectionNode> nodeList = new ArrayList<>();
                if (isReflection(insnNode, nodeList)) {
                    if (nodeList.size() == 1) {
                        graph.addNode(nodeList.get(0));
                    } else if (nodeList.size() > 1) {
                        graph.addList(nodeList);
                    }
                }
            }
        }
        if (graph.shouldOptimize()) {
            optimizeReflection(mth);
        }
        graph.clear();
    }

    public boolean isReflection(InsnNode insnNode, ArrayList<ReflectionNode> list) {
        if (insnNode.getType().equals(InsnType.INVOKE)) {
            String fullName = ((InvokeNode) insnNode).getCallMth().getFullName();

            switch (fullName) {
                case "java.lang.Class.forName" -> {
                    // only one argument
                    InsnArg arg = insnNode.getArg(0);
                    ReflectionRoot forNameNode = new ReflectionRoot(ReflectionType.FORNAME, arg, insnNode);
                    String className = "";
                    if (arg.isInsnWrap()) {
                        InsnNode node = ((InsnWrapArg) arg).unwrap();
                        if (node instanceof ConstStringNode) {
                            className = ((ConstStringNode) node).getString();
                        }
                    } else if (arg.isLiteral()) {
                        className = ((LiteralArg) arg).toString();
                    }
                    if (!className.isEmpty()) {
                        forNameNode.setClassNode(jadx.searchClassNodeByOrigFullName(className));
                        forNameNode.setResult(insnNode.getResult());
                        list.add(forNameNode);
                        return true;
                    }
                    return false;
                }
                case "java.lang.reflect.Method.invoke" -> {
                    ArrayList<InsnArg> args = new ArrayList<>();
                    for (InsnArg arg : insnNode.getArguments()) {
                        if (arg.isInsnWrap()) {
                            if (!isReflection(((InsnWrapArg) arg).getWrapInsn(), list)) {
                                args.add(arg);
                            }
                        } else {
                            args.add(arg);
                        }
                    }
                    ReflectionNode invokeNode = new ReflectionNode(ReflectionType.INVOKE, args, insnNode);
                    invokeNode.setResult(insnNode.getResult());
                    list.add(invokeNode);
                    return true;
                }
                case "java.lang.Class.getMethod" -> {
                    ArrayList<InsnArg> args = new ArrayList<>();
                    for (InsnArg arg : insnNode.getArguments()) {
                        if (arg.isInsnWrap()) {
                            if (!isReflection(((InsnWrapArg) arg).getWrapInsn(), list)) {
                                args.add(arg);
                            }
                        } else {
                            args.add(arg);
                        }
                    }
                    ReflectionNode getMethodNode = new ReflectionNode(ReflectionType.GETMETHOD, args, insnNode);
                    getMethodNode.setResult((insnNode).getResult());
                    list.add(getMethodNode);
                    return true;
                }
                case "java.lang.Class.getField" -> {
                    ArrayList<InsnArg> args = new ArrayList<>();
                    for (InsnArg arg : insnNode.getArguments()) {
                        if (arg.isInsnWrap()) {
                            if (!isReflection(((InsnWrapArg) arg).getWrapInsn(), list)) {
                                args.add(arg);
                            }
                        } else {
                            args.add(arg);
                        }
                    }
                    ReflectionNode getFieldNode = new ReflectionNode(ReflectionType.GETFIELD, args, insnNode);
                    getFieldNode.setResult((insnNode).getResult());
                    list.add(getFieldNode);
                    return true;
                }
                case "java.lang.Class.getConstructor" -> {
                    ArrayList<InsnArg> args = new ArrayList<>();
                    for (InsnArg arg : insnNode.getArguments()) {
                        if (arg.isInsnWrap()) {
                            if (!isReflection(((InsnWrapArg) arg).getWrapInsn(), list)) {
                                args.add(arg);
                            }
                        } else {
                            args.add(arg);
                        }
                    }
                    ReflectionNode constructorNode = new ReflectionNode(ReflectionType.CONSTRUCTOR, args, insnNode);
                    constructorNode.setResult(insnNode.getResult());
                    list.add(constructorNode);
                    return true;
                }
                case "java.lang.reflect.Constructor.newInstance" -> {
                    ArrayList<InsnArg> args = new ArrayList<>();
                    for (InsnArg arg : insnNode.getArguments()) {
                        if (arg.isInsnWrap()) {
                            if (!isReflection(((InsnWrapArg) arg).getWrapInsn(), list)) {
                                args.add(arg);
                            }
                        } else {
                            args.add(arg);
                        }
                    }
                    ReflectionNode newInstanceNode = new ReflectionNode(ReflectionType.NEWINSTANCE, args, insnNode);
                    newInstanceNode.setResult(insnNode.getResult());
                    list.add(newInstanceNode);
                    return true;
                }
                default -> {
                    return false;
                }
            }
        } else {
            for (InsnArg arg : insnNode.getArguments()) {
                if (arg.isInsnWrap()) {
                    isReflection(((InsnWrapArg) arg).getWrapInsn(), list);
                }
            }
        }
        return false;
    }

    public void optimizeReflection(MethodNode mth) {
        CopyOnWriteArrayList<ReflectionRoot> roots = graph.getRoots();
        for (ReflectionRoot root : roots) {
            buildInstance(root, mth);
            buildInvoke(root, mth);
            // remove forName method
            InsnRemover.remove(mth, root.getInsnNode());
        }
    }

    private void buildInstance(ReflectionRoot root, MethodNode mth) {
        // find NEWINSTANCE
        for (ReflectionNode ConstructorNode : root.getSuccessors()) {
            // has constructor
            if (ConstructorNode.getType().equals(ReflectionType.CONSTRUCTOR)) {
                List<ReflectionNode> instances = ConstructorNode.getSuccessors();
                if (!instances.isEmpty()) {
                    for (ReflectionNode instance : instances) {

                        if (instance.getType().equals(ReflectionType.NEWINSTANCE)) {
                            RegisterArg res = instance.getResult();
                            // get arguments
                            ArrayList<InsnArg> args = ParesArguments(instance);
                            InsnNode newInstance = makeNewInstanceInsn(res, args, mth, root);

                            BlockUtils.replaceInsn(mth, instance.getInsnNode(), newInstance);
                        }
                    }
                    // remove getConstructor method
                    InsnRemover.remove(mth, ConstructorNode.getInsnNode());
                }
                // remove constructorNode and it successors
                this.graph.removeNode(ConstructorNode);
            }
        }
    }

    private void buildInvoke(ReflectionRoot root, MethodNode mth) {
        // get the arguments(the third argument, an array contain all arguments)
        // and call object(the second argument) from INVOKE node
        // get the method name and argument type from GETMETHOD node,
        for (ReflectionNode getMethodNode : root.getSuccessors()) {
            if (getMethodNode.getType().equals(ReflectionType.GETMETHOD)) {
                String methodName = parseMethodName(getMethodNode);
                if (!methodName.isEmpty()) {
                    for (ReflectionNode invoke : getMethodNode.getSuccessors()) {
                        ArrayList<InsnArg> args = ParesArguments(invoke);
                        RegisterArg res = invoke.getResult();
                        InsnNode invokeNode = makeInvokeInsn(mth, methodName, args, res, root);

                        BlockUtils.replaceInsn(mth, invoke.getInsnNode(), invokeNode);
                    }
                    // remove getMethod method
                    InsnRemover.remove(mth, getMethodNode.getInsnNode());
                } else {
                    // fail to parse method name
                    continue;
                }
            }
            this.graph.removeNode(getMethodNode);
        }
    }

    private String parseMethodName(ReflectionNode node) {
        String name = "";
        InsnArg arg = node.getArgs().get(0);
        // not inline call
        if (arg.isRegister()) {
            arg = node.getArgs().get(1);
        }
        if (arg.isLiteral()) {
            name = ((LiteralArg) arg).toString();
        } else if (arg.isInsnWrap()) {
            InsnNode a = ((InsnWrapArg) arg).unwrap();
            if (a instanceof ConstStringNode) {
                name = ((ConstStringNode) a).getString();
            }
            // TODO
            // other type of method name node
        }
        return name;
    }

    private ArrayList<InsnArg> ParesArguments(ReflectionNode node) {
        ArrayList<InsnArg> args = new ArrayList<>();
        for (InsnArg arg : node.getArgs()) {
            if (arg.isRegister()) {
                if (node.getType().equals(ReflectionType.NEWINSTANCE)) {
                    if (((RegisterArg) arg).getSVar().getCodeVar().getType().getObject().equals("java.lang.reflect.Constructor")) {
                        continue;
                    }
                } else if (node.getType().equals(ReflectionType.INVOKE)) {
                    if (((RegisterArg) arg).getSVar().getCodeVar().getType().getObject().equals("java.lang.reflect.Method")) {
                        continue;
                    }
                }
            } else if (arg.isInsnWrap()) {
                InsnNode insnNode = ((InsnWrapArg) arg).getWrapInsn();
                if (insnNode instanceof NewArrayNode) {
                    // no argument
                    InsnArg a = insnNode.getArg(0);
                    if (a.isLiteral() && ((LiteralArg) a).getLiteral() == 0)
                        continue;
                } else if (insnNode instanceof FilledNewArrayNode) {
                    for (InsnArg a : ((FilledNewArrayNode) insnNode).getArguments()) {
                        args.add(a);
                    }
                    continue;
                }
            }
            args.add(arg);
        }
        return args;
    }

    private InsnNode makeNewInstanceInsn(RegisterArg res, ArrayList<InsnArg> args, MethodNode mth, ReflectionRoot root) {
        // find constructor method
        Iterator var1 = root.getClassNode().getMethods().iterator();
        MethodNode methodNode;
        do {
            if (!var1.hasNext()) {
                return null;
            }

            methodNode = (MethodNode) var1.next();
        } while (!methodNode.isConstructor() && methodNode.getArgRegs().size() == args.size());

        ConstructorInsn constructorInsn = new ConstructorInsn(methodNode.getMethodInfo(), ConstructorInsn.CallType.CONSTRUCTOR);
        constructorInsn.setResult(res);
        for (InsnArg arg : args) {
            constructorInsn.addArg(arg);
        }
        return constructorInsn;
    }

    private InsnNode makeInvokeInsn(MethodNode mth, String name, ArrayList<InsnArg> args, RegisterArg res, ReflectionRoot root) {
        MethodNode callMethod = null;
        if (root.getClassNode() == null) {
            return null;
        }
        for (MethodNode methodNode : root.getClassNode().getMethods()) {
            if (methodNode.getMethodInfo().getName().equals(name)) {
                callMethod = methodNode;
                break;
            }
        }
        if (callMethod != null) {
            MethodInfo methodInfo = callMethod.getMethodInfo();
            InvokeNode invokeNode = new InvokeNode(methodInfo, InvokeType.VIRTUAL, methodInfo.getArgsCount() + 1);
            for (InsnArg arg : args) {
                invokeNode.addArg(arg);
            }
            if (res != null) {
                invokeNode.setResult(res);
            }
            return invokeNode;
        }
        return null;
    }
}
