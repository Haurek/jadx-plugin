import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.AbstractVisitor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class MyRenameVisitor extends AbstractVisitor {
    private final RootNode root;

    public MyRenameVisitor() {
        root = null;
    }

    public MyRenameVisitor(RootNode root) {
        this.root = root;
    }

    @Override
    public boolean visit(ClassNode classNode) {
        // rename classNode
        if (!classNode.getClassInfo().hasAlias() && isObfuscationName(classNode.getFullName())) {
            renameClass(classNode);
        }

        // rename method
        for (MethodNode methodNode : classNode.getMethods()) {
            if (!methodNode.isConstructor() && !methodNode.getMethodInfo().hasAlias() && !methodNode.isDefaultConstructor() && isObfuscationName(methodNode.getName())) {
                renameMethod(methodNode);
            }
        }
        // rename field
        for (FieldNode fieldNode : classNode.getFields()) {
            if (!fieldNode.getFieldInfo().isRenamed() && isObfuscationName(fieldNode.getName())) {
                renameField(fieldNode);
            }
        }
        return true;
    }

    public void renameClass(ClassNode classNode) {
        String oldName = classNode.getShortName();
        StringBuilder newName = new StringBuilder();
        // first the first three letters of the oldName to distinguish the same inheritance
        if (oldName.length() < 3) {
            newName.append(oldName);
        } else {
            newName.append(oldName, 0, 3);
        }

        // classNode has super Class
        if (!classNode.getSuperClass().equals(ArgType.OBJECT)) {
            visitSuperTypes(classNode.getType(), newName);
        }
        // class without extends but has interfaces
        else if (!classNode.getInterfaces().isEmpty()) {
            visitInterfacesType(classNode.getType(), newName);
        } else {
            newName.append("Class");
        }
        classNode.getClassInfo().changeShortName(newName.toString());
    }

    public void renameMethod(MethodNode methodNode) {
        String oldName = methodNode.getName();
        StringBuilder newName = new StringBuilder();
        newName.append(oldName);
        List<ArgType> params = methodNode.getArgTypes();
        if (params.isEmpty()) {
            newName.append("_null");
        } else {
            for (ArgType param : methodNode.getArgTypes()) {
                newName.append("_").append(MyPlugin.extractType(param.toString()));
            }
        }
        methodNode.getMethodInfo().setAlias(newName.toString());
    }

    public void renameField(FieldNode fieldNode) {
        String oldName = fieldNode.getName();
        StringBuilder newName = new StringBuilder();
        newName.append(oldName).append("_").append(MyPlugin.extractType(fieldNode.getType().toString()));
        fieldNode.rename(newName.toString());
    }
    public void visitInterfacesType(ArgType type, StringBuilder newName) {
        ClassNode classNode = this.root.resolveClass(type);
        Iterator<ArgType> ifaces = null;
        if (classNode != null) {
            ifaces = classNode.getInterfaces().iterator();
        }
        if (ifaces != null) {
            while (ifaces.hasNext()) {
                ArgType iface = ifaces.next();
                String ifaceName = MyPlugin.extractClassName(iface.getObject());
                if (ifaceName != null) {
                    // name of super is obfuscation
                    if (isObfuscationName(ifaceName)) {
                        ClassNode ifaceClass = this.root.resolveClass(ifaceName);
                        if (ifaceClass != null) {
                            renameClass(ifaceClass);
                            ifaceName = ifaceClass.getClassInfo().getAliasShortName();
                            newName.append(ifaceName);
                            return;
                        }
                    } else {
                        newName.append(ifaceName);
                    }
                }
                visitInterfacesType(iface, newName);
            }
        }
    }
    public void visitSuperTypes(ArgType type, StringBuilder newName) {
        ClassNode classNode = this.root.resolveClass(type);
        if (classNode != null) {
            ArgType thisType = classNode.getType();
            ArgType superType = classNode.getSuperClass();
            if (superType != null && !superType.equals(ArgType.OBJECT)) {
                String superName = MyPlugin.extractClassName(superType.getObject());
                if (superName != null) {
                    // name of super is obfuscation
                    if (isObfuscationName(superName)) {
                        ClassNode superClass = this.root.resolveClass(superType);
                        if (superClass != null) {
                            renameClass(superClass);
                            superName = superClass.getClassInfo().getAliasShortName();
                            newName.append(superName);
                            return;
                        }
                    } else {
                        newName.append(superName);
                    }
                }
                visitSuperTypes(superType, newName);
            }
        }
    }

    private boolean isObfuscationName(String fullname) {
        HashMap<Character, Integer> count = new HashMap<>();
        String[] part = fullname.split("\\.");
        if (part.length >= 2) {
            String first = part[0];
            String second = part[1];
            if (first.equals("android") || first.equals("androidx") || first.equals("java") || first.equals("kotlin")
                    || first.equals("kotlinx") || (first.equals("com") && second.equals("google"))) {
                return false;
            }
        }
        String name;
        if (part.length > 0) {
            name = part[part.length - 1];
        } else {
            name = fullname;
        }
        int len = name.length();

        if (name.equals("<clinit>"))
            return false;
        // Class R
        if (name.equals("R"))
            return false;

        // Length equals 1
        if (len == 1) {
            return true;
        }

        // Starts with a digit
        if (Character.isDigit(name.charAt(0))) {
            return true;
        }

        // Contains special characters
        if (!name.matches("[a-zA-Z0-9$_]+")) {
            return true;
        }

        // Repeated character occurrence more than 3 times
        for (int i = 0; i < len; i++) {
            if ((i < len - 3)) {
                char c0 = Character.toLowerCase(name.charAt(i));
                char c1 = Character.toLowerCase(name.charAt(i + 1));
                char c2 = Character.toLowerCase(name.charAt(i + 2));
                if (c0 == c1 && c1 == c2) {
                    return true;
                }
            }
            Character c = Character.toLowerCase(name.charAt(i));
            if (count.containsKey(c)) {
                int time = count.get(c);
                if (len > 10 && (time == len / 3))
                    return true;
                count.put(c, time + 1);
            } else {
                count.put(c, 1);
            }
        }

        return false;
    }
}
