package twomillions.plugin.advancedwish.utils.commands;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import twomillions.plugin.advancedwish.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * 该提供了与 Bukkit 命令相关的实用方法。
 *
 * @author 2000000
 * @date 2023/4/29
 */
@UtilityClass
public class CommandUtils {
    /**
     * 将命令注册到 Bukkit 的命令映射表中。
     *
     * @param command 要注册的命令
     */
    public static void registerCommand(@NonNull Command command) {
        getCommandMap().register(command.getName(), command);
    }

    /**
     * 从 Bukkit 的命令映射表中注销命令。
     *
     * @param command 要注销的命令
     */
    public static void unregister(@NonNull Command command) {
        CommandMap commandMap = getCommandMap();
        Map<String, Command> knownCommands = getKnownCommands(commandMap);

        String name = command.getName();
        List<String> aliases = command.getAliases();

        knownCommands.entrySet().removeIf(entry -> entry.getKey().equalsIgnoreCase(name) || entry.getKey().startsWith(name + ":") || aliases.contains(entry.getKey()));
    }

    /**
     * 获取 Bukkit 命令映射表。
     *
     * @return Bukkit 命令映射表
     */
    @SneakyThrows
    public static CommandMap getCommandMap() {
        Method method = Bukkit.getServer().getClass().getDeclaredMethod("getCommandMap");
        method.setAccessible(true);
        return (CommandMap) method.invoke(Bukkit.getServer());
    }

    /**
     * 获取 Bukkit 命令映射表中已知的命令。
     *
     * @param commandMap 命令映射表
     * @return 已知命令表
     */
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static Map<String, Command> getKnownCommands(CommandMap commandMap) {
        if (Main.getServerVersion() >= 1013) {
            Method method = commandMap.getClass().getDeclaredMethod("getKnownCommands");
            method.setAccessible(true);
            return (Map<String, Command>) method.invoke(commandMap);
        }

        Field field = commandMap.getClass().getDeclaredField("knownCommands");
        field.setAccessible(true);
        return (Map<String, Command>) field.get(commandMap);
    }
}

