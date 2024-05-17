package twomillions.plugin.advancedwish.utils.texts;

import me.clip.placeholderapi.PlaceholderAPI;
import twomillions.plugin.advancedwish.Main;
import twomillions.plugin.advancedwish.annotations.JsInteropJavaType;
import twomillions.plugin.advancedwish.managers.config.ConfigManager;
import twomillions.plugin.advancedwish.managers.register.RegisterManager;
import twomillions.plugin.advancedwish.utils.scripts.ScriptUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 实用工具类。
 *
 * @author 2000000
 * @date 2022/11/21 12:39
 */
@JsInteropJavaType
@SuppressWarnings({"deprecation", "unused"})
public class QuickUtils {
    private static final JavaPlugin plugin = Main.getInstance();

    private static final String CHAT_BAR = ChatColor.GRAY.toString() + ChatColor.STRIKETHROUGH + "------------------------------------------------";

    /**
     * 科学技术法转换为普通字符串
     *
     * @param string 科学计数法
     * @return 普通字符串
     */
    public static String toPlainString(String string) {
        try {
            return new BigDecimal(string).toPlainString();
        } catch (Exception exception) {
            return string;
        }
    }

    /**
     * 将传入字符串颜色代码翻译为实际的颜色。
     *
     * @param string 要翻译的字符串
     * @return 翻译后的字符串
     */
    public static String translate(String string) {
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    /**
     * 向控制台发送消息。
     *
     * @param message 要发送的消息
     */
    public static void sendConsoleMessage(String message) {
        Bukkit.getConsoleSender().sendMessage(translate("&e[Advanced Wish] " + message));
    }

    /**
     * 去除输入字符串中的颜色代码。
     *
     * @param string 待处理的字符串
     * @return 去除颜色代码后的新字符串
     */
    public static String stripColor(String string) {
        return ChatColor.stripColor(string);
    }

    /**
     * 去除输入字符串数组中的颜色代码。
     *
     * @param strings 待处理的字符串数组
     * @return 去除颜色代码后的新字符串数组
     */
    public static String[] stripColor(String[] strings) {
        return Arrays.stream(strings)
                .map(ChatColor::stripColor)
                .toArray(String[]::new);
    }

    /**
     * 将传入的字符串替换变量。
     *
     * @param message 要替换和翻译的字符串
     * @param player 可选用于变量替换的第一玩家
     * @param params 替换的可选参数
     * @return 替换后的字符串
     */
    public static String replace(String message, Player player, String... params) {
        message = message
                .replaceAll("<version>", plugin.getDescription().getVersion())
                .replaceAll("<wishlist>", RegisterManager.getRegisterWish().toString())
                .replaceAll("<CHAT_BAR>", CHAT_BAR);

        if (player != null) {
            message = message.replaceAll("<player>", player.getName());
        }

        if (params != null) {
            for (int i = 0; i < params.length; i += 2) {
                message = message.replaceAll(params[i], params[i + 1]);
            }
        }

        return message;
    }

    /**
     * 将传入的字符串替换变量并翻译颜色代码。
     *
     * @param string 要替换和翻译的字符串
     * @param player 可选用于变量替换的第一玩家
     * @return 替换后和翻译后的字符串
     */
    public static String replaceTranslate(String string, Player player) {
        return translate(replace(string, player));
    }

    /**
     * 从 message.yml 内读取对应信息并发送。
     *
     * @param player 玩家
     * @param string message.yml 内对应信息
     * @param params 替换的可选参数
     */
    public static void sendMessage(Player player, String string, Object... params) {
        ConfigManager.getMessageYaml().getStringList(string).stream()
                .map(message -> {
                    if (params != null) {
                        for (int i = 0; i < params.length; i += 2) {
                            message = message.replaceAll(params[i].toString(), params[i + 1].toString());
                        }
                    }

                    return QuickUtils.handleString(message, player);
                })
                .forEach(player::sendMessage);
    }

    /**
     * 从 message.yml 内读取对应信息并发送。
     *
     * @param sender 指令发送对象
     * @param string message.yml 内对应信息
     * @param params 替换的可选参数
     */
    public static void sendMessage(CommandSender sender, String string, Object... params) {
        ConfigManager.getMessageYaml().getStringList(string).stream()
                .map(message -> {
                    if (params != null) {
                        for (int i = 0; i < params.length; i += 2) {
                            message = message.replaceAll(params[i].toString(), params[i + 1].toString());
                        }
                    }

                    return QuickUtils.handleString(message);
                })
                .forEach(sender::sendMessage);
    }

    /**
     * 获取玩家名对应的 UUID.
     *
     * @param playerName 玩家名
     */
    public static String getPlayerUUID(String playerName) {
        if (Main.isOfflineMode()) {
            return UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes()).toString();
        }

        return Bukkit.getOfflinePlayer(playerName).getUniqueId().toString();
    }

    /**
     * 将输入字符串转换为 PlaceholderAPI 字符串，如果当前服务器已经安装了 PlaceholderAPI 插件，则使用该插件进行转换。
     *
     * @param string 待转换的字符串
     * @return 如果当前服务器已经安装了 PlaceholderAPI 插件，则返回转换后的字符串，否则返回原始字符串。
     */
    public static String toPapi(String string) {
        Matcher matcher = Pattern.compile("%.*%").matcher(string);

        if (matcher.find() && !RegisterManager.isUsingPapi()) {
            QuickUtils.sendConsoleMessage("&c尝试解析占位符，但未找到 &ePlaceholder API &c插件，您安装了它吗?");
            return string;
        }

        if (!RegisterManager.isUsingPapi()) {
            return string;
        }

        String result = PlaceholderAPI.setPlaceholders(null, string);
        Matcher matcherResult = Pattern.compile("%.*%").matcher(result);

        if (matcherResult.find()) {
            QuickUtils.sendConsoleMessage("&c您安装了 &ePlaceholder API &c插件，但依然存在未正常解析的占位符，您安装了完整的占位符拓展吗?");
        }

        return result;
    }

    /**
     * 将输入字符串转换为 PlaceholderAPI 字符串，如果当前服务器已经安装了 PlaceholderAPI 插件，则使用该插件进行转换。
     *
     * @param string 待转换的字符串
     * @param player 转换过程中使用的玩家
     * @return 如果当前服务器已经安装了 PlaceholderAPI 插件，则返回转换后的字符串，否则返回原始字符串。
     */
    public static String toPapi(String string, Player player) {
        Matcher matcher = Pattern.compile("%.*%").matcher(string);

        if (matcher.find() && !RegisterManager.isUsingPapi()) {
            QuickUtils.sendConsoleMessage("&c尝试解析占位符，但未找到 &ePlaceholder API &c插件，您安装了它吗?");
            return string;
        }

        if (!RegisterManager.isUsingPapi()) {
            return string;
        }

        String result = PlaceholderAPI.setPlaceholders(player, string);
        Matcher matcherResult = Pattern.compile("%.*%").matcher(result);

        if (matcherResult.find()) {
            QuickUtils.sendConsoleMessage("&c您安装了 &ePlaceholder API &c插件，但依然存在未正常解析的占位符，您安装了完整的占位符拓展吗?");
        }

        return result;
    }

    /**
     * 对传入的字符串进行处理，包括替换、翻译和随机语句支持等操作。
     *
     * @param string 待处理的字符串
     * @param params 替换的可选参数
     * @return 处理后的字符串
     */
    public static String handleString(String string, Object... params) {
        return ScriptUtils.eval(string, null, params);
    }

    /**
     * 对传入的字符串进行处理，包括替换、翻译和随机语句支持等操作。
     *
     * @param string 待处理的字符串
     * @param player 可选用于变量替换的第一玩家
     * @param params 替换的可选参数
     * @return 处理后的字符串
     */
    public static String handleString(String string, Player player, Object... params) {
        return ScriptUtils.eval(string, player, params);
    }

    /**
     * 对传入的字符串进行处理，包括替换、翻译和随机语句支持，算数等操作。
     *
     * @param string 待处理的字符串
     * @param params 替换的可选参数
     * @return 处理后的整数
     */
    public static int handleInt(String string, Object... params) {
        String result = ScriptUtils.eval(string, null, params);

        if (result.contains(".")) {
            result = result.split("\\.")[0];
        }

        return Integer.parseInt(result);
    }

    /**
     * 对传入的字符串进行处理，包括替换、翻译和随机语句支持，算数等操作。
     *
     * @param string 待处理的字符串
     * @param player 可选用于变量替换的第一玩家
     * @param params 替换的可选参数
     * @return 处理后的整数
     */
    public static int handleInt(String string, Player player, Object... params) {
        String result = ScriptUtils.eval(string, player, params);

        if (result.contains(".")) {
            result = result.split("\\.")[0];
        }

        return Integer.parseInt(result);
    }

    /**
     * 对传入的字符串进行处理，包括替换、翻译和随机语句支持，算数等操作。
     *
     * @param string 待处理的字符串
     * @param params 替换的可选参数
     * @return 处理后的长整数
     */
    public static long handleLong(String string, Object... params) {
        String result = ScriptUtils.eval(string, null, params);

        if (result.contains(".")) {
            result = result.split("\\.")[0];
        }

        return Long.parseLong(result);
    }

    /**
     * 对传入的字符串进行处理，包括替换、翻译和随机语句支持，算数等操作。
     *
     * @param string 待处理的字符串
     * @param player 可选用于变量替换的第一玩家
     * @param params 替换的可选参数
     * @return 处理后的长整数
     */
    public static long handleLong(String string, Player player, Object... params) {
        String result = ScriptUtils.eval(string, player, params);

        if (result.contains(".")) {
            result = result.split("\\.")[0];
        }

        return Long.parseLong(result);
    }

    /**
     * 对传入的字符串进行处理，包括替换、翻译和随机语句支持，算数等操作。
     *
     * @param string 待处理的字符串
     * @param params 替换的可选参数
     * @return 处理后的浮点数
     */
    public static double handleDouble(String string, Object... params) {
        return Double.parseDouble(ScriptUtils.eval(string, null, params));
    }

    /**
     * 对传入的字符串进行处理，包括替换、翻译和随机语句支持，算数等操作。
     *
     * @param string 待处理的字符串
     * @param player 可选用于变量替换的第一玩家
     * @param params 替换的可选参数
     * @return 处理后的浮点数
     */
    public static double handleDouble(String string, Player player, Object... params) {
        return Double.parseDouble(ScriptUtils.eval(string, player, params));
    }

    /**
     * 对传入的字符串进行处理，包括替换、翻译和随机语句支持，算数等操作。
     *
     * @param string 待处理的字符串
     * @param params 替换的可选参数
     * @return 处理后的布尔值
     */
    public static boolean handleBoolean(String string, Object... params) {
        return Boolean.parseBoolean(ScriptUtils.eval(string, null, params));
    }

    /**
     * 对传入的字符串进行处理，包括替换、翻译和随机语句支持，算数等操作。
     *
     * @param string 待处理的字符串
     * @param player 可选用于变量替换的第一玩家
     * @param params 替换的可选参数
     * @return 处理后的布尔值
     */
    public static boolean handleBoolean(String string, Player player, Object... params) {
        return Boolean.parseBoolean(ScriptUtils.eval(string, player, params));
    }

    /**
     * 对传入的字符串数组进行处理，包括替换、翻译和随机语句支持，算数等操作。
     *
     * @param strings 待处理的字符串数组
     * @param params 替换的可选参数
     * @return 处理后的字符串数组
     */
    public static String[] handleStrings(String[] strings, Object... params) {
        Function<String, String> handleFunction = string -> {
            if (isInt(string)) {
                return String.valueOf(handleInt(string, params));
            } else if (isLong(string)) {
                return String.valueOf(handleLong(string, params));
            } else if (isDouble(string)) {
                return String.valueOf(handleDouble(string, params));
            } else {
                return handleString(string, params);
            }
        };

        return Arrays.stream(strings)
                .map(handleFunction)
                .toArray(String[]::new);
    }

    /**
     * 对传入的字符串数组进行处理，包括替换、翻译和随机语句支持，算数等操作。
     *
     * @param strings 待处理的字符串数组
     * @param player 可选用于变量替换的第一玩家
     * @param params 替换的可选参数
     * @return 处理后的字符串数组
     */
    public static String[] handleStrings(String[] strings, Player player, Object... params) {
        Function<String, String> handleFunction = string -> {
            if (isInt(string)) {
                return String.valueOf(handleInt(string, player, params));
            } else if (isLong(string)) {
                return String.valueOf(handleLong(string, player, params));
            } else if (isDouble(string)) {
                return String.valueOf(handleDouble(string, player, params));
            } else {
                return handleString(string, player, params);
            }
        };

        return Arrays.stream(strings)
                .map(handleFunction)
                .toArray(String[]::new);
    }

    /**
     * 判断输入字符串是否为 long 类型。
     *
     * @param string 待检查的字符串
     * @return 如果字符串为 long 类型，则返回 true，否则返回 false。
     */
    public static boolean isLong(String string) {
        try {
            Long.parseLong(string);
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    /**
     * 判断输入字符串是否为 int 类型。
     *
     * @param string 待检查的字符串
     * @return 如果字符串为 int 类型，则返回 true，否则返回 false。
     */
    public static boolean isInt(String string) {
        try {
            BigDecimal num = new BigDecimal(string);
            return num.stripTrailingZeros().scale() <= 0 && num.compareTo(BigDecimal.valueOf(Integer.MIN_VALUE)) >= 0 && num.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) <= 0;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    /**
     * 判断输入字符串是否为 double 类型，包括可选的负号、至少一个整数位和可选的小数位。
     *
     * @param string 待检查的字符串
     * @return 如果字符串为 double 类型，则返回 true，否则返回 false。
     */
    public static boolean isDouble(String string) {
        return string.matches("^-?\\d+\\.\\d*[1-9]+$|^(-?\\d+)\\.\\d+$");
    }

    /**
     * 替换字符串中位于指定区间的文本为指定的字符串。
     *
     * @param string 要替换的字符串
     * @param start 要替换的区间的起始字符串
     * @param end 要替换的区间的结束字符串
     * @param replace 要替换成的字符串
     * @param removeStartEndString 是否移除起始和结束字符串
     * @return 替换后的字符串
     */
    private static String stringInterceptReplace(String string, String start, String end, String replace, boolean removeStartEndString) {
        int startIndex = string.indexOf(start);
        int endIndex = string.indexOf(end, startIndex + 1);

        if (startIndex == -1 || endIndex == -1) {
            return string;
        }

        String beforeStart = string.substring(0, startIndex + start.length());
        String afterEnd = string.substring(endIndex);

        String replacedString = beforeStart + replace + afterEnd;

        if (removeStartEndString) {
            return replacedString.replace(start, "").replace(end, "");
        } else {
            return beforeStart + replace + afterEnd;
        }
    }

    /**
     * 将集合转为 String.
     *
     * @param collection 集合
     * @return 字符串
     */
    public static <T> String listToString(Collection<T> collection) {
        return collection.stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));
    }

    /**
     * 将 String 转为集合.
     *
     * @param string 字符串
     * @return 集合
     */
    @SuppressWarnings("unchecked")
    public static <T> ConcurrentLinkedQueue<T> stringToList(String string) {
        return Arrays.stream(string.split(","))
                .map(s -> (T) s)
                .collect(Collectors.toCollection(ConcurrentLinkedQueue::new));
    }
}