package twomillions.plugin.advancedwish.utils.scripts.interop;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import twomillions.plugin.advancedwish.annotations.JsInteropJavaType;
import twomillions.plugin.advancedwish.interfaces.ScriptInteropInterface;
import twomillions.plugin.advancedwish.interfaces.TriFunction;
import twomillions.plugin.advancedwish.utils.commands.CommandUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * @author 2000000
 * @date 2023/4/29
 */
@Getter @Setter
@JsInteropJavaType
@SuppressWarnings("unused")
@Builder(setterPrefix = "set")
public class ScriptCommandHandler implements ScriptInteropInterface {
    /**
     * ScriptCommandHandler 列表。
     */
    @Getter private static final ConcurrentLinkedQueue<ScriptCommandHandler> scriptCommandHandler = new ConcurrentLinkedQueue<>();

    /**
     * 指令主名。
     */
    private final @NonNull String name;

    /**
     * 指令别名。
     */
    private final @NonNull String aliases;

    /**
     * 描述。
     */
    private final @NonNull String description;

    /**
     * 用于处理指令执行的代码。
     */
    private final TriFunction<CommandSender, String, String[], Boolean> commandExecute;

    /**
     * 用于处理 Tab 补全的代码。
     */
    private final TriFunction<CommandSender, String, String[], List<String>> tabComplete;

    /**
     * Command 实例。
     */
    @Builder.Default
    private Command command = null;

    /**
     * 注册指令。
     */
    @Override
    public void register() {
        if (command != null) {
            unregister();
        }

        toCommand();
        CommandUtils.registerCommand(command);
        scriptCommandHandler.add(this);
    }

    /**
     * 注销指令。
     */
    @Override
    public void unregister() {
        if (command != null) {
            CommandUtils.unregister(command);
            scriptCommandHandler.remove(this);
        }
    }

    /**
     * 转换 Command 实例。
     */
    private void toCommand() {
        command = new Command(name) {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
                return commandExecute.apply(sender, commandLabel, args);
            }

            @NotNull
            @Override
            public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
                return tabComplete.apply(sender, alias, args);
            }
        }.setAliases(Arrays.stream(aliases.split(","))
                .map(String::trim)
                .collect(Collectors.toList()))
                .setDescription(description);
    }
}
