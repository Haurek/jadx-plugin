// jadx.api
import jadx.api.*;
import jadx.api.plugins.JadxPlugin;
// java
import java.io.File;
import java.io.IOException;

public class App {
    public static void main(String[] args) throws IOException {
        JadxArgs jadxArgs = new JadxArgs();
        // input file
        jadxArgs.setInputFile(new File("app-debug.apk"));
//        jadxArgs.setInputFile(new File("deobfuscate_test.apk"));
//        jadxArgs.setInputFile(new File("base64.dex"));
//        jadxArgs.setInputFile(new File("rename_test.apk"));
        jadxArgs.setUseSourceNameAsClassAlias(true);
        jadxArgs.setDeobfuscationOn(true);
        // output dir
        jadxArgs.setOutDir(new File("output"));

        try (JadxDecompiler jadx = new JadxDecompiler(jadxArgs)) {
            // load decompiler
            jadx.load();
            Iterable<JadxPlugin> plugins = jadx.getPluginManager().getAllPlugins();
            for (JadxPlugin plugin : plugins) {
                // run MyPlugin
                if (plugin instanceof MyPlugin) {
                    // select plugin
                    MyPlugin myPlugin = new MyPlugin(jadx, false, false, true, false);
                    System.out.println("find MyPlugin");
                }
            }
            // save result
            jadx.save();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
