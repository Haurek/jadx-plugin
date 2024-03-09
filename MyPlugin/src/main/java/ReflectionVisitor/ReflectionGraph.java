package ReflectionVisitor;

import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

public class ReflectionGraph {
    private CopyOnWriteArrayList<ReflectionRoot> roots;
    private CopyOnWriteArrayList<ReflectionNode> nodes;

    public ReflectionGraph() {
        roots = new CopyOnWriteArrayList<>();
        nodes = new CopyOnWriteArrayList<>();
    }

    public boolean addNode(ReflectionNode node) {
        BiConsumer<ReflectionNode, ReflectionNode> consumer = ReflectionNode::addSuccessor;
        if (node.getType().equals(ReflectionType.FORNAME)) {
            // add root
            roots.add((ReflectionRoot) node);
            return true;
        }
        for (ReflectionNode root : roots) {
            if (searchParent(root, node, consumer)) {
                nodes.add(node);
                return true;
            }
        }
        return false;
    }

    public boolean removeNode(ReflectionNode node) {
        if (!nodes.remove(node)) {
            return false;
        }
        BiConsumer<ReflectionNode, ReflectionNode> consumer = ReflectionNode::removeSuccessor;
        for (ReflectionNode root : roots) {
            if (searchParent(root, node, consumer)) {
                return true;
            }
        }
        return false;
    }

    public boolean removeRoot(ReflectionRoot root) {
        if (!nodes.remove(root)) {
            return false;
        }
        for (ReflectionRoot r : roots) {
            if (r.equals(root)) {
                roots.remove(root);
                return true;
            }
        }
        return false;
    }

    public boolean searchParent(ReflectionNode root, ReflectionNode node, BiConsumer<ReflectionNode, ReflectionNode> consumer) {
        if (isParent(root, node)) {
            consumer.accept(root, node);
            return true;
        } else {
            for (ReflectionNode successor : root.getSuccessors()) {
                if (searchParent(successor, node, consumer)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean depthSearch(ReflectionNode root, ReflectionNode node, BiConsumer<ReflectionNode, ReflectionNode> consumer) {
        if (root.equals(node)) {
            consumer.accept(root, node);
            return true;
        } else {
            for (ReflectionNode successor : root.getSuccessors()) {
                if (depthSearch(successor, node, consumer)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isParent(ReflectionNode parent, ReflectionNode child) {
        try {
            SSAVar p_res = parent.getResult().getSVar();
            SSAVar c_arg0 = ((RegisterArg) child.getArgs().get(0)).getSVar();
            return p_res.equals(c_arg0);
        } catch (Exception e) {
            return false;
        }
    }

    public void addList(ArrayList<ReflectionNode> list) {
        if (list.get(0) instanceof ReflectionRoot) {
            roots.add(((ReflectionRoot) list.get(0)));
            for (int j = 0; j < list.size() - 1; ++j) {
                ReflectionNode r = list.get(j);
                ReflectionNode s = list.get(j + 1);
                r.addSuccessor(s);
                nodes.add(s);
            }
        } else {
            if (addNode(list.get(0))) {
                for (int j = 0; j < list.size() - 1; ++j) {
                    ReflectionNode r = list.get(j);
                    ReflectionNode s = list.get(j + 1);
                    r.addSuccessor(s);
                    nodes.add(s);
                }
            }
        }
    }

    public CopyOnWriteArrayList<ReflectionNode> getNodes() {
        return nodes;
    }

    public CopyOnWriteArrayList<ReflectionRoot> getRoots() {
        return roots;
    }

    public boolean shouldOptimize() {
        if (roots.size() == 0) {
            return false;
        }
        for (ReflectionNode node : nodes) {
            if (node.getType().equals(ReflectionType.INVOKE)) {
                return true;
            }
        }
        return false;
    }

    public void clear() {
        roots.clear();
        nodes.clear();
    }
}