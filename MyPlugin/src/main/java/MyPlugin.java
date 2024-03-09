// jadx.api
import ReflectionVisitor.ReflectionVisitor;
import jadx.api.CommentsLevel;
import jadx.api.JadxDecompiler;
import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginInfo;
// jadx.core
import jadx.core.dex.nodes.*;
import jadx.core.dex.visitors.*;
import jadx.core.dex.visitors.typeinference.TypeInferenceVisitor;

// java
import java.util.*;

public class MyPlugin implements JadxPlugin {
    private final JadxPluginInfo pluginInfo = new JadxPluginInfo("my-plugin", "MyPlugins", "decode, rename and simplify rgb function");
    JadxDecompiler jadx;
    public MyPlugin() {
        jadx = null;
    }
    public MyPlugin(JadxDecompiler j, boolean renameOn, boolean decodeOn, boolean reflectionOn, boolean rgb) {
        this.jadx = j;
        RootNode root = jadx.getRoot();
        List<IDexTreeVisitor> passes = root.getPasses();
        if (renameOn) {
            appendCustomPass(passes, new MyRenameVisitor(root));
        }
        if (decodeOn) {
            root.getArgs().setCommentsLevel(CommentsLevel.USER_ONLY);
            addCustomPassAfter(passes, AttachTryCatchVisitor.class, new DecodeVisitor());
        }
        if (reflectionOn) {
            appendCustomPass(passes, new ReflectionVisitor(jadx));
        }
        if (rgb) {
            addCustomPassAfter(passes, TypeInferenceVisitor.class, new RGBVisitor());
        }
    }

//    public void decodeVisitor() {
//        RootNode root = jadx.getRoot();
//        for (ClassNode classNode : root.getClasses()) {
//            // load class
//            classNode.load();
//            for (MethodNode methodNode : classNode.getMethods()) {
//                if (methodNode == null)
//                    continue;
//                // get strings
//                ArrayList<String> strings = getStringFromMethod(methodNode);
//                if (!strings.isEmpty()) {
//                    // constructor method
//                    if (methodNode.isConstructor()) {
//                        addDecodeComments(classNode, decode(strings));
//                    } else {
//                        addDecodeComments(methodNode, decode(strings));
//                    }
//                }
//            }
//        }
//    }
    public static void addCustomPassAfter(List<IDexTreeVisitor> passes, Class<?> passCls, IDexTreeVisitor customPass) {
        for (int i = 0; i < passes.size(); i++) {
            IDexTreeVisitor pass = passes.get(i);
            if (pass.getClass().equals(passCls)) {
                passes.add(i + 1, customPass);
                break;
            }
        }
    }
    public static void appendCustomPass(List<IDexTreeVisitor> passes, IDexTreeVisitor customPass) {
        passes.add(passes.size(), customPass);
    }
    public static String extractClassName(String fullName) {
        int lastDotIndex = fullName.lastIndexOf(".");
        if (lastDotIndex >= 0) {
            return fullName.substring(lastDotIndex + 1);
        }
        return null;
    }
    public static String extractType(String type) {
        int lastDotIndex = type.lastIndexOf(".");
        if (lastDotIndex >= 0) {
            return type.substring(lastDotIndex + 1);
        }
        return type;
    }
    public static String addEscapeCharacters(String str) {
        StringBuilder builder = new StringBuilder();
        for (char c : str.toCharArray()) {
            switch (c) {
                case '\"':
                    builder.append("\\\"");
                    break;
                case '\'':
                    builder.append("\\\'");
                    break;
                case '\\':
                    builder.append("\\\\");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                case '\b':
                    builder.append("\\b");
                    break;
                case '\f':
                    builder.append("\\f");
                    break;
                default:
                    builder.append(c);
            }
        }
        return builder.toString();
    }
    public JadxPluginInfo getPluginInfo() {
        return this.pluginInfo;
    }
}