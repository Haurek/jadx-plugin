import jadx.api.CommentsLevel;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.IAttributeNode;
import jadx.core.dex.attributes.nodes.JadxCommentsAttr;
import jadx.core.dex.instructions.ConstStringNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.AbstractVisitor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DecodeVisitor extends AbstractVisitor {
    private static final Pattern base64Pattern = Pattern.compile("^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{4})$");
    private static final Pattern UrlPattern = Pattern.compile("%[0-9A-Fa-f]{2}");
    private static final Pattern UnicodePattern = Pattern.compile("\\\\u([0-9A-Fa-f]{4})+");
    public enum DecodeType {
        BASE64,
        URL,
        UNICODE
    }

    public static class DecodeNode {
        private final DecodeType type;
        private final String decodeString;

        public DecodeNode(DecodeType type, String str) {
            this.type = type;
            this.decodeString = str;
        }

        public DecodeType getType() {
            return this.type;
        }

        public String getDecodeString() {
            return this.decodeString;
        }
    }
    @Override
    public boolean visit(ClassNode classNode) {
        for (MethodNode methodNode : classNode.getMethods()) {
            if (methodNode == null)
                continue;
            // get strings
            ArrayList<String> strings = getStringFromMethod(methodNode);
            if (!strings.isEmpty()) {
                // constructor method
                if (methodNode.isConstructor()) {
                    addDecodeComments(classNode, decode(strings));
                } else {
                    addDecodeComments(methodNode, decode(strings));
                }
            }
        }
        return true;
    }
    private HashMap<String, ArrayList<DecodeNode>> decode(ArrayList<String> strings) {
        HashMap<String, ArrayList<DecodeNode>> stringMap = new HashMap<>();
        for (String s : strings) {
            ArrayList<DecodeNode> decodeList = new ArrayList<>();
            decodeString(s, decodeList);
            stringMap.put(s, decodeList);
        }
        return stringMap;
    }
    private void decodeString(String s, ArrayList<DecodeNode> nodes) {
        // decode Base64
        Matcher base64Matcher = base64Pattern.matcher(s);
        if (base64Matcher.find()) {
            String encodeBase64 = base64Matcher.group();
            String decodeBase64 = decodeBase64(encodeBase64);
            if (decodeBase64 != null) {
                nodes.add(new DecodeNode(DecodeType.BASE64, decodeBase64));
                decodeString(decodeBase64, nodes);
            }
        }
        // decode Url
        Matcher urlMatcher = UrlPattern.matcher(s);
        StringBuilder urlSb = new StringBuilder();
        while (urlMatcher.find()) {
            String encodeUrl = urlMatcher.group();
            urlSb.append(decodeUrl(encodeUrl));
        }
        if (!urlSb.isEmpty()) {
            String decodeUrl = urlSb.toString();
            nodes.add(new DecodeNode(DecodeType.URL, decodeUrl));
            decodeString(decodeUrl, nodes);
        }
        // decode Unicode
        Matcher unicodeMatcher = UnicodePattern.matcher(s);
        StringBuilder unicodeSb = new StringBuilder();
        while (unicodeMatcher.find()) {
            String encodeUnicode = unicodeMatcher.group();
            unicodeSb.append(decodeUnicode(encodeUnicode));
        }
        if (!unicodeSb.isEmpty()) {
            String decodeUnicode = unicodeSb.toString();
            nodes.add(new DecodeNode(DecodeType.UNICODE, decodeUnicode));
            decodeString(decodeUnicode, nodes);
        }
    }
    private String decodeBase64(String encodedBase64) {
        try {
            byte[] decodedBytes = java.util.Base64.getDecoder().decode(encodedBase64);
            String decodeStr = new String(decodedBytes);
            return MyPlugin.addEscapeCharacters(decodeStr);
        } catch (Exception e) {
            return null;
        }
    }
    private String decodeUrl(String encodeUrl) {
        return URLDecoder.decode(encodeUrl, StandardCharsets.UTF_8);
    }
    private String decodeUnicode(String unicodeString) {
        StringBuilder sb = new StringBuilder();
        int start = 0;
        int end = unicodeString.indexOf("\\u", start + 2);
        while (end != -1) {
            String codePoint = unicodeString.substring(start + 2, end);
            int intValue = Integer.parseInt(codePoint, 16);
            sb.append((char) intValue);
            start = end;
            end = unicodeString.indexOf("\\u", start + 2);
        }
        String lastCodePoint = unicodeString.substring(start + 2);
        int lastIntValue = Integer.parseInt(lastCodePoint, 16);
        sb.append((char) lastIntValue);
        return sb.toString();
    }
    private ArrayList<String> getStringFromMethod(MethodNode methodNode) {

        ArrayList<String> strings = new ArrayList<>();

        if (methodNode.isNoCode())
            return strings;

        for (InsnNode insnNode : methodNode.getInstructions()) {
            if (insnNode == null)
                continue;
            // get string
            if (insnNode.getType().equals(InsnType.CONST_STR)) {
                String s = ((ConstStringNode) insnNode).getString();
                if (!Objects.equals(s, "")) {
                    strings.add(s);
                }
            }
        }
        return strings;
    }
    private void addDecodeComments(IAttributeNode attributeNode, HashMap<String, ArrayList<DecodeNode>> comments) {

        JadxCommentsAttr jadxCommentsAttr = new JadxCommentsAttr();
        StringBuilder comment = new StringBuilder();

        if (attributeNode instanceof MethodNode) {
            comment = new StringBuilder("Strings Decode at method " + ((MethodNode) attributeNode).getAlias() + ":\n");
        } else if (attributeNode instanceof ClassNode) {
            comment = new StringBuilder("Strings Decode at constructor:\n");
        }

        for (Map.Entry<String, ArrayList<DecodeNode>> entry : comments.entrySet()) {
            String originString = entry.getKey();
            ArrayList<DecodeNode> list = entry.getValue();
            if (!list.isEmpty()) {
                comment.append(originString);
                for (DecodeNode node : list) {
                    switch (node.getType()) {
                        case BASE64 -> {
                            comment.append(" --Base64-> ").append(node.getDecodeString());
                        }
                        case URL -> {
                            comment.append(" --Url-> ").append(node.getDecodeString());
                        }
                        case UNICODE -> {
                            comment.append(" --Unicode-> ").append(node.getDecodeString());
                        }
                        default -> {
                        }
                    }
                }
                comment.append("\n");
            }
        }

        jadxCommentsAttr.add(CommentsLevel.USER_ONLY, comment.toString());
        attributeNode.addAttr(AType.CODE_COMMENTS, comment.toString());
    }
}
