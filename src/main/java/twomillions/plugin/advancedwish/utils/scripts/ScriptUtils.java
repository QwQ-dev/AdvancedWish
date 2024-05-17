package twomillions.plugin.advancedwish.utils.scripts;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import twomillions.plugin.advancedwish.Main;
import twomillions.plugin.advancedwish.annotations.JsInteropJavaType;
import twomillions.plugin.advancedwish.managers.register.RegisterManager;
import twomillions.plugin.advancedwish.utils.others.CaffeineUtils;
import twomillions.plugin.advancedwish.utils.others.ConstantsUtils;
import twomillions.plugin.advancedwish.utils.scripts.interop.ScriptPlaceholderExpander;
import twomillions.plugin.advancedwish.utils.texts.QuickUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.entity.Player;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JavaScript 工具类。
 *
 * @author 2000000
 * @date 2023/3/2
 */
@UtilityClass
@JsInteropJavaType
@SuppressWarnings("unused")
public class ScriptUtils {
    @Getter private static Context rhino;
    @Getter private static Scriptable GLOBAL_SCRIPTABLE;
    @Getter private static final Cache<String, Scriptable> scriptableMap = CaffeineUtils.buildBukkitCache();

    public static Scriptable getScriptable(String scriptable) {
        boolean isGlobal = scriptable.equalsIgnoreCase("global");
        Scriptable result = isGlobal ? GLOBAL_SCRIPTABLE : scriptableMap.get(scriptable, k -> rhino.initStandardObjects());

        if (!isGlobal) {
            result.put("_id_", result, "");
        }

        return result;
    }

    public static void removeScriptable(String scriptable) {
        scriptableMap.invalidate(scriptable);
    }

    /**
     * 初始化，加载默认脚本文件，加载 Java 类。
     */
    @SneakyThrows
    public static void setup() {
        rhino = Context.enter();
        GLOBAL_SCRIPTABLE = rhino.initStandardObjects();

        String scriptsPath = Main.getScriptPath();
        File scriptsFolder = new File(Main.getScriptPath());

        /*
         * 检查文件夹是否存在
         * 如果不存在则创建文件夹并写入 scriptSetup.js
         */
        if (!scriptsFolder.exists()) {
            FileUtils.forceMkdir(scriptsFolder);

            File file = new File(scriptsPath + "/scriptSetup.js");

            InputStream inputStream = Main.class.getClassLoader().getResourceAsStream("Scripts/scriptSetup.js");

            if (inputStream != null) {
                FileUtils.copyInputStreamToFile(inputStream, file);
            }
        }

        rhino.evaluateString(GLOBAL_SCRIPTABLE, "const Bukkit = Packages.org.bukkit.Bukkit", "RhinoJs", 1, null);

        rhino.evaluateString(GLOBAL_SCRIPTABLE, "const Main = Packages.twomillions.plugin.advancedwish.Main.getInstance()", "RhinoJs", 1, null);
        rhino.evaluateString(GLOBAL_SCRIPTABLE, "const plugin = Packages.twomillions.plugin.advancedwish.Main.getInstance()", "RhinoJs", 1, null);

        /*
         * 获取使用 JsInteropJavaType 注解的 Java 类
         */
        for (Class<?> aClass : JsInteropJavaType.Processor.getClasses()) {
            String simpleName = aClass.getSimpleName();
            String canonicalName = aClass.getCanonicalName();

            /*
             * 如果是 ScriptPlaceholderExpander.class 并且服务器没有 Placeholder API
             */
            if (aClass == ScriptPlaceholderExpander.class) {
                if (!RegisterManager.isUsingPapi()) {
                    QuickUtils.sendConsoleMessage("&c取消加载 &eJava&c 类: &e" + simpleName + " &7&o(" + canonicalName +  ")&c，因为服务器中没有 &ePlaceholder API&c 插件。");
                    continue;
                }
            }

            rhino.evaluateString(GLOBAL_SCRIPTABLE, "const " + simpleName + " = Packages." + canonicalName, "RhinoJs", 1, null);
            QuickUtils.sendConsoleMessage("&a成功加载 &eJava&a 类: &e" + simpleName + " &7&o(" + canonicalName + ")&a，可使用 &e" + simpleName + " &a调用类中 &e方法、函数 &a等。");
        }
    }

    /**
     * 添加 Scriptable 对象。
     *
     * @param player 表达式目标玩家
     * @param params 传递给 JavaScript 的可选参数
     */
    private static Scriptable putScopeValues(Scriptable scope, Player player, Object... params) {
        rhino = Context.enter();

        scope.setParentScope(GLOBAL_SCRIPTABLE);

        scope.put("_player_", scope, player);
        scope.put("_pluginPath_", scope, Main.getInstance().getDataFolder().getParentFile().getAbsolutePath());
        scope.put("method", scope, new MethodFunctions(player));

        for (int i = 0; i < params.length; i += 2) {
            scope.put(params[i].toString(), scope, params[i + 1]);
        }

        return scope;
    }

    /**
     * 使用正则匹配获取脚本自定义 Scriptable 值。
     *
     * @param script 需要匹配的脚本
     * @return 自定义 Scriptable 值
     */
    public static String getCustomScriptable(String script) {
        String pattern = "\\[Scriptable:\\s*(.*?)]";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(script);

        return matcher.find() ? matcher.group(1) : "";
    }

    /**
     * 分析 JavaScript 表达式，返回结果字符串。
     *
     * @param string JavaScript 表达式
     * @param player 表达式目标玩家
     * @param params 传递给 JavaScript 的可选参数
     * @return 结果的字符串表示。如果计算失败，则返回原始输入字符串
     */
    public static String eval(String string, Player player, Object... params) {
        rhino = Context.enter();

        Object result;

        string = QuickUtils.toPapi(QuickUtils.replaceTranslate(string, player), player);

        String customScriptable = getCustomScriptable(string);
        Scriptable scriptable = customScriptable.equals("") ? putScopeValues(rhino.initStandardObjects(), player, params) : putScopeValues(getScriptable(customScriptable), player, params);

        string = customScriptable.equals("") ? string : string.replaceAll("\\[Scriptable:\\s*(.*?)]", "");

        try {
            result = Context.toString(rhino.evaluateString(scriptable, string, "RhinoJs", 1, null));
        } catch (Exception exception) {
            return string;
        }

        if (result == null || result.equals("undefined") || result.equals("null")) {
            return "";
        }

        return QuickUtils.toPlainString(result.toString());
    }

    /**
     * 执行指定的 js 文件内的指定函数。
     *
     * @param file 文件
     * @param functionName 要执行的函数名
     * @param player 表达式目标玩家
     * @param params 传递给 JavaScript 的可选参数
     * @return 函数的返回值，如果函数执行失败则返回 null。
     */
    public static Object invokeFunction(File file, String functionName, Player player, Object... params) {
        rhino = Context.enter();

        Object result = null;

        if (file.exists()) {
            try {
                String script = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

                String customScriptable = getCustomScriptable(script);
                Scriptable scriptable = customScriptable.equals("") ? putScopeValues(rhino.initStandardObjects(), player, params) : putScopeValues(getScriptable(customScriptable), player, params);

                script = customScriptable.equals("") ? script : script.replaceAll("\\[Scriptable:\\s*(.*?)]", "");

                result = Context.toString(rhino.evaluateString(scriptable, script + "\n" + functionName + "()", file.getName(), 1, null));

                QuickUtils.sendConsoleMessage("&a成功执行 &eJavaScript &a内函数: &e" + functionName + "&a，文件名: &e" + file.getName() + "&a!");
            } catch (Exception exception) {
                exception.printStackTrace();
                QuickUtils.sendConsoleMessage("&c无法执行 &eJavaScript &c内函数: &e" + functionName + "&c，文件名: &e" + file.getName() + "&c!");
            }
        }

        if (result == null || result.equals("undefined")) {
            return "";
        }

        return QuickUtils.toPlainString(result.toString());
    }

    /**
     * 检查指定的 js 文件是否存在指定的函数。
     *
     * @param file 文件
     * @param functionName 要检查的函数名
     * @return 如果 js 文件存在并且包含指定函数，返回 true；否则返回 false。
     */
    public static boolean hasFunction(File file, String functionName) {
        try {
            String script = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            return Arrays.stream(StringUtils.split(script, "\n"))
                    .map(String::trim)
                    .anyMatch(line -> StringUtils.startsWith(line, "function " + functionName));
        } catch (Exception exception) {
            exception.printStackTrace();
            QuickUtils.sendConsoleMessage("&c无法检查 &eJavaScript &a文件: &e" + file.getName() + "&a!");
        }

        return false;
    }

    /**
     * 遍历默认脚本文件夹下的所有 js 文件，检查其中是否有指定名的函数，如果有，则执行该函数。
     *
     * @param functionName 要执行的函数名
     * @param player 表达式目标玩家
     * @param params 传递给 JavaScript 的可选参数
     */
    @SneakyThrows
    public static void invokeFunctionInAllScripts(String functionName, Player player, Object... params) {
        rhino = Context.enter();

        File scriptsDir = new File(Main.getScriptPath());

        if (!scriptsDir.exists()) {
            return;
        }

        File[] scriptFiles = scriptsDir.listFiles((dir, name) -> name.endsWith(ConstantsUtils.JAVA_SCRIPT_FILE_EXTENSION));

        if (scriptFiles == null) {
            return;
        }

        for (File file : scriptFiles) {
            if (hasFunction(file, functionName)) {
                invokeFunction(file, functionName, player, params);
            }
        }
    }

    /**
     * 遍历指定路径下的所有 js 文件，检查其中是否有指定名的函数，如果有，则执行该函数。
     *
     * @param path 路径
     * @param functionName 要执行的函数名
     * @param player 表达式目标玩家
     * @param params 传递给 JavaScript 的可选参数
     */
    @SneakyThrows
    public static void invokeFunctionInAllScripts(String path, String functionName, Player player, Object... params) {
        rhino = Context.enter();

        File scriptsDir = new File(path);

        if (!scriptsDir.exists()) {
            return;
        }

        File[] scriptFiles = scriptsDir.listFiles((dir, name) -> name.endsWith(ConstantsUtils.JAVA_SCRIPT_FILE_EXTENSION));

        if (scriptFiles == null) {
            return;
        }

        for (File file : scriptFiles) {
            if (hasFunction(file, functionName)) {
                invokeFunction(file, functionName, player, params);
            }
        }
    }
}
