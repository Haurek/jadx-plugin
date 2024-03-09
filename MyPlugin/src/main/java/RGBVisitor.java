import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.InsnUtils;

import java.util.ArrayList;
import java.util.List;

public class RGBVisitor extends AbstractVisitor{
    @Override
    public void visit(MethodNode mth) {
        if (mth.isNoCode()) {
            return;
        }
        // search rgb function
        InsnNode rgbNode = InsnUtils.searchInsn(mth, InsnType.INVOKE, (insn) -> {
            InvokeNode invokeNode = (InvokeNode) insn;
            return invokeNode.getCallMth().getName().equals("rgb") && invokeNode.getArgsCount() == 3 &&
                    invokeNode.getArg(0).isLiteral() && invokeNode.getArg(1).isLiteral() && invokeNode.getArg(2).isLiteral();
        });
        // replace rgb function
        if (rgbNode != null) {
            long red = ((LiteralArg) rgbNode.getArg(0)).getLiteral();
            long green = ((LiteralArg) rgbNode.getArg(1)).getLiteral();
            long blue = ((LiteralArg) rgbNode.getArg(2)).getLiteral();
            long color = (red << 16) + (green << 8) + blue;
            List<InsnArg> args = new ArrayList<>();
            args.add(InsnArg.lit(color, ArgType.INT));
            InsnNode replaceNode = new InsnNode(InsnType.CONST, args);
            replaceNode.setResult(rgbNode.getResult());
            // replace invoke instruction
            BlockUtils.replaceInsn(mth, rgbNode, replaceNode);
        }
    }
}