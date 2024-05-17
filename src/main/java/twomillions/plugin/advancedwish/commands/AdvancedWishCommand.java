package twomillions.plugin.advancedwish.commands;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import twomillions.plugin.advancedwish.Main;
import twomillions.plugin.advancedwish.managers.WishManager;
import twomillions.plugin.advancedwish.managers.config.ConfigManager;
import twomillions.plugin.advancedwish.managers.logs.LogManager;
import twomillions.plugin.advancedwish.managers.register.RegisterManager;
import twomillions.plugin.advancedwish.utils.others.CaffeineUtils;
import twomillions.plugin.advancedwish.utils.texts.QuickUtils;
import twomillions.plugin.advancedwish.utils.texts.StringEncrypter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 该类实现 {@link TabExecutor}，通过反射与注解处理指令及 Tab 补全。
 *
 * @author 2000000
 * @date 2023/3/26
 */
@SuppressWarnings({"unused", "SameParameterValue"})
public class AdvancedWishCommand extends Command {
    private static final JavaPlugin plugin = Main.getInstance();

    /**
     * 实例对象变量。
     */
    @Getter private static final AdvancedWishCommand advancedWishCommand = createInstance();

    /**
     * 获取实例对象。
     *
     * @return 实例对象
     */
    private static AdvancedWishCommand createInstance() {
        String name = ConfigManager.getAdvancedWishYaml().getString("COMMAND-NAME");
        List<String> aliases = ConfigManager.getAdvancedWishYaml().getStringList("ALIASES");
        String description = ConfigManager.getAdvancedWishYaml().getString("DESCRIPTION");

        name = name.isEmpty() ? "advancedwish" : name;
        aliases = aliases.isEmpty() ? Collections.singletonList("aw") : aliases;
        description = description.isEmpty() ? "Advanced Wish 默认命令。" : description;

        return new AdvancedWishCommand(name, aliases, description);
    }

    /**
     * 使用 SubCommand 的方法名与方法。
     */
    private final Cache<String, Method> subCommandMap = CaffeineUtils.buildBukkitCache();

    /**
     * 构造器，扫描指令处理方法并缓存。
     */
    private AdvancedWishCommand(String name, List<String> aliases, String description) {
        super(name);
        setAliases(aliases);
        setDescription(description);

        Arrays.stream(getClass().getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(SubCommand.class))
                .forEach(method -> subCommandMap.put(method.getAnnotation(SubCommand.class).value().toLowerCase(), method));
    }

    @SubCommand("help")
    public void handleHelpCommand(CommandSender sender, String[] args) {
        boolean isAdmin = isAdmin(sender, false);
        boolean isConsole = isConsole(sender, false);

        if (isAdmin) {
            if (isConsole) {
                QuickUtils.sendMessage(sender, "ADMIN-SHOW-COMMAND");
            } else {
                QuickUtils.sendMessage((Player) sender, "ADMIN-SHOW-COMMAND");
            }
        } else {
            if (isConsole) {
                QuickUtils.sendMessage(sender, "DEFAULT-SHOW-COMMAND");
            } else {
                QuickUtils.sendMessage((Player) sender, "DEFAULT-SHOW-COMMAND");
            }
        }
    }

    @SubCommand("list")
    public void handleTestCommand(CommandSender sender, String[] args) {
        boolean isConsole = isConsole(sender, false);

        if (isConsole) {
            QuickUtils.sendMessage(sender, "LIST");
        } else {
            QuickUtils.sendMessage((Player) sender, "LIST");
        }
    }

    @SubCommand("amount")
    public void handleAmountCommand(CommandSender sender, String[] args) {
        boolean isConsole = isConsole(sender, false);

        if (args.length == 1) {
            if (isConsole) {
                QuickUtils.sendMessage(sender, "WISH-NULL");
            } else {
                QuickUtils.sendMessage((Player) sender, "WISH-NULL");
            }
            return;
        }

        String playerName = "";
        String wishName = args[1];

        if (args.length == 2 && isConsole) {
            QuickUtils.sendMessage(sender, "PLAYER-NULL");
            return;
        }

        if (args.length > 2) {
            playerName = args[2];
        }

        if (!WishManager.hasWish(wishName)) {
            if (isConsole) {
                QuickUtils.sendMessage(sender, "WISH-NOT-HAVE");
            } else {
                QuickUtils.sendMessage((Player) sender, "WISH-NOT-HAVE");
            }
            return;
        }

        int playerWishAmount = playerName.isEmpty() ?
                WishManager.getPlayerWishAmount((Player) sender, wishName) : WishManager.getPlayerWishAmount(playerName, wishName);

        if (isConsole) {
            QuickUtils.sendMessage(sender, "GET-AMOUNT"
                    , "<wish>", wishName
                    , "<amount>", playerWishAmount
                    , "<player>", playerName);
        } else {
            QuickUtils.sendMessage((Player) sender, "GET-AMOUNT"
                    , "<wish>", wishName
                    , "<amount>", playerWishAmount
                    , "<player>", sender.getName());
        }
    }

    @SubCommand("guaranteed")
    public void handleGuaranteedCommand(CommandSender sender, String[] args) {
        boolean isConsole = isConsole(sender, false);

        if (args.length == 1) {
            if (isConsole) {
                QuickUtils.sendMessage(sender, "WISH-NULL");
            } else {
                QuickUtils.sendMessage((Player) sender, "WISH-NULL");
            }
            return;
        }

        String playerName = "";
        String wishName = args[1];

        if (args.length == 2 && isConsole) {
            QuickUtils.sendMessage(sender, "PLAYER-NULL");
            return;
        }

        if (args.length > 2) {
            playerName = args[2];
        }

        if (!WishManager.hasWish(wishName)) {
            if (isConsole) {
                QuickUtils.sendMessage(sender, "WISH-NOT-HAVE");
            } else {
                QuickUtils.sendMessage((Player) sender, "WISH-NOT-HAVE");
            }
            return;
        }

        double playerWishGuaranteed = playerName.isEmpty() ?
                WishManager.getPlayerWishGuaranteed((Player) sender, wishName) : WishManager.getPlayerWishGuaranteed(playerName, wishName);

        if (isConsole) {
            QuickUtils.sendMessage(sender, "GET-GUARANTEED"
                    , "<wish>", wishName
                    , "<guaranteed>", playerWishGuaranteed
                    , "<player>", playerName);
        } else {
            QuickUtils.sendMessage((Player) sender, "GET-GUARANTEED"
                    , "<wish>", wishName
                    , "<guaranteed>", playerWishGuaranteed
                    , "<player>", sender.getName());
        }
    }

    @SubCommand("limitamount")
    public void handleLimitAmountCommand(CommandSender sender, String[] args) {
        boolean isConsole = isConsole(sender, false);

        if (args.length == 1) {
            if (isConsole) {
                QuickUtils.sendMessage(sender, "WISH-NULL");
            } else {
                QuickUtils.sendMessage((Player) sender, "WISH-NULL");
            }
            return;
        }

        String playerName = "";
        String wishName = args[1];

        if (args.length == 2 && isConsole) {
            QuickUtils.sendMessage(sender, "PLAYER-NULL");
            return;
        }

        if (args.length > 2) {
            playerName = args[2];
        }

        if (!WishManager.hasWish(wishName)) {
            if (isConsole) {
                QuickUtils.sendMessage(sender, "WISH-NOT-HAVE");
            } else {
                QuickUtils.sendMessage((Player) sender, "WISH-NOT-HAVE");
            }
            return;
        }

        if (!WishManager.isEnabledWishLimit(wishName)) {
            if (isConsole) {
                QuickUtils.sendMessage(sender, "WISH-NOT-ENABLED-LIMIT");
            } else {
                QuickUtils.sendMessage((Player) sender, "WISH-NOT-ENABLED-LIMIT");
            }
            return;
        }

        String limitAmount = QuickUtils.handleString(WishManager.getWishLimitAmount(wishName));

        int playerLimitAmount = playerName.isEmpty() ?
                WishManager.getPlayerWishLimitAmount((Player) sender, wishName) : WishManager.getPlayerWishLimitAmount(playerName, wishName);

        if (isConsole) {
            QuickUtils.sendMessage(sender, "GET-LIMIT-AMOUNT"
                    , "<wish>", wishName
                    , "<limitAmount>", limitAmount
                    , "<playerLimitAmount>", playerLimitAmount
                    , "<player>", playerName);
        } else {
            QuickUtils.sendMessage((Player) sender, "GET-LIMIT-AMOUNT"
                    , "<wish>", wishName
                    , "<limitAmount>", limitAmount
                    , "<playerLimitAmount>", playerLimitAmount
                    , "<player>", sender.getName());
        }
    }

    @SubCommand("makewish")
    public void handleMakeWishCommand(CommandSender sender, String[] args) {
        boolean isConsole = isConsole(sender, false);

        if (args.length == 1) {
            if (isConsole) {
                QuickUtils.sendMessage(sender, "WISH-NULL");
            } else {
                QuickUtils.sendMessage((Player) sender, "WISH-NULL");
            }
            return;
        }

        String wishName = args[1];

        if (!WishManager.hasWish(wishName)) {
            if (isConsole) {
                QuickUtils.sendMessage(sender, "WISH-NOT-HAVE");
            } else {
                QuickUtils.sendMessage((Player) sender, "WISH-NOT-HAVE");
            }
            return;
        }

        Player targetPlayer;
        boolean force = false;
        boolean otherPlayerWish = false;

        if (args.length > 2) {
            targetPlayer = Bukkit.getPlayerExact(args[2]);

            if (targetPlayer == null) {
                if (isConsole) {
                    QuickUtils.sendMessage(sender, "PLAYER-OFFLINE");
                } else {
                    QuickUtils.sendMessage((Player) sender, "PLAYER-OFFLINE");
                }
                return;
            }

            otherPlayerWish = true;
        } else {
            if (isConsole) {
                QuickUtils.sendMessage(sender, "PLAYER-NULL");
                return;
            } else {
                targetPlayer = (Player) sender;
            }
        }

        if (args.length == 4 && isAdmin(sender, false)) {
            force = Boolean.parseBoolean(args[3]);
        }

        WishManager.makeWish(targetPlayer, wishName, force);

        if (otherPlayerWish) {
            if (isConsole) {
                QuickUtils.sendMessage(sender, "DONE");
            } else {
                QuickUtils.sendMessage((Player) sender, "DONE");
            }
        }
    }

    @SubCommand("setAmount")
    public void handleSetAmountCommand(CommandSender sender, String[] args) {
        boolean isConsole = isConsole(sender, false);

        if (!isAdmin(sender, true)) {
            return;
        }

        if (args.length < 4) {
            if (isConsole) {
                QuickUtils.sendMessage(sender, "COMMAND-ERROR");
            } else {
                QuickUtils.sendMessage((Player) sender, "COMMAND-ERROR");
            }
            return;
        }

        String wishName = args[1];
        String playerName = args[2];
        int amount;

        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException exception) {
            if (isConsole) {
                QuickUtils.sendMessage(sender, "MUST-NUMBER");
            } else {
                QuickUtils.sendMessage((Player) sender, "MUST-NUMBER");
            }
            return;
        }

        if (!WishManager.hasWish(wishName)) {
            if (isConsole) {
                QuickUtils.sendMessage(sender, "WISH-NOT-HAVE");
            } else {
                QuickUtils.sendMessage((Player) sender, "WISH-NOT-HAVE");
            }
            return;
        }

        WishManager.setPlayerWishAmount(playerName, wishName, amount);

        if (isConsole) {
            QuickUtils.sendMessage(sender, "DONE");
        } else {
            QuickUtils.sendMessage((Player) sender, "DONE");
        }
    }

    @SubCommand("setGuaranteed")
    public void handleSetGuaranteedCommand(CommandSender sender, String[] args) {
        boolean isConsole = isConsole(sender, false);

        if (!isAdmin(sender, true)) {
            return;
        }

        if (args.length < 4) {
            if (isConsole) {
                QuickUtils.sendMessage(sender, "COMMAND-ERROR");
            } else {
                QuickUtils.sendMessage((Player) sender, "COMMAND-ERROR");
            }
            return;
        }

        String wishName = args[1];
        String playerName = args[2];
        double guaranteed;

        try {
            guaranteed = Double.parseDouble(args[3]);
        } catch (NumberFormatException exception) {
            if (isConsole) {
                QuickUtils.sendMessage(sender, "MUST-NUMBER");
            } else {
                QuickUtils.sendMessage((Player) sender, "MUST-NUMBER");
            }
            return;
        }

        if (!WishManager.hasWish(wishName)) {
            if (isConsole) {
                QuickUtils.sendMessage(sender, "WISH-NOT-HAVE");
            } else {
                QuickUtils.sendMessage((Player) sender, "WISH-NOT-HAVE");
            }
            return;
        }

        WishManager.setPlayerWishGuaranteed(playerName, wishName, guaranteed);

        if (isConsole) {
            QuickUtils.sendMessage(sender, "DONE");
        } else {
            QuickUtils.sendMessage((Player) sender, "DONE");
        }
    }

    @SubCommand("setLimitAmount")
    public void handleSetLimitAmountCommand(CommandSender sender, String[] args) {
        boolean isConsole = isConsole(sender, false);

        if (!isAdmin(sender, true)) {
            return;
        }

        if (args.length < 4) {
            if (isConsole) {
                QuickUtils.sendMessage(sender, "COMMAND-ERROR");
            } else {
                QuickUtils.sendMessage((Player) sender, "COMMAND-ERROR");
            }
            return;
        }

        String wishName = args[1];
        String playerName = args[2];
        int limitAmount;

        try {
            limitAmount = Integer.parseInt(args[3]);
        } catch (NumberFormatException exception) {
            if (isConsole) {
                QuickUtils.sendMessage(sender, "MUST-NUMBER");
            } else {
                QuickUtils.sendMessage((Player) sender, "MUST-NUMBER");
            }
            return;
        }

        if (!WishManager.hasWish(wishName)) {
            if (isConsole) {
                QuickUtils.sendMessage(sender, "WISH-NOT-HAVE");
            } else {
                QuickUtils.sendMessage((Player) sender, "WISH-NOT-HAVE");
            }
            return;
        }

        if (!WishManager.isEnabledWishLimit(wishName)) {
            if (isConsole) {
                QuickUtils.sendMessage(sender, "WISH-NOT-ENABLED-LIMIT");
            } else {
                QuickUtils.sendMessage((Player) sender, "WISH-NOT-ENABLED-LIMIT");
            }
            return;
        }

        WishManager.setPlayerWishLimitAmount(playerName, wishName, limitAmount);

        if (isConsole) {
            QuickUtils.sendMessage(sender, "DONE");
        } else {
            QuickUtils.sendMessage((Player) sender, "DONE");
        }
    }

    @SubCommand("addAmount")
    public void handleAddAmountCommand(CommandSender sender, String[] args) {
        boolean isConsole = isConsole(sender, false);

        if (!isAdmin(sender, true)) {
            return;
        }

        if (args.length < 4) {
            if (isConsole) {
                QuickUtils.sendMessage(sender, "COMMAND-ERROR");
            } else {
                QuickUtils.sendMessage((Player) sender, "COMMAND-ERROR");
            }
            return;
        }

        String wishName = args[1];
        String playerName = args[2];
        int amount;

        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException exception) {
            if (isConsole) {
                QuickUtils.sendMessage(sender, "MUST-NUMBER");
            } else {
                QuickUtils.sendMessage((Player) sender, "MUST-NUMBER");
            }
            return;
        }

        if (!WishManager.hasWish(wishName)) {
            if (isConsole) {
                QuickUtils.sendMessage(sender, "WISH-NOT-HAVE");
            } else {
                QuickUtils.sendMessage((Player) sender, "WISH-NOT-HAVE");
            }
            return;
        }

        WishManager.setPlayerWishAmount(playerName, wishName, WishManager.getPlayerWishAmount(playerName, wishName) + amount);

        if (isConsole) {
            QuickUtils.sendMessage(sender, "DONE");
        } else {
            QuickUtils.sendMessage((Player) sender, "DONE");
        }
    }

    @SubCommand("addGuaranteed")
    public void handleAddGuaranteedCommand(CommandSender sender, String[] args) {
        boolean isConsole = isConsole(sender, false);

        if (!isAdmin(sender, true)) {
            return;
        }

        if (args.length < 4) {
            if (isConsole) {
                QuickUtils.sendMessage(sender, "COMMAND-ERROR");
            } else {
                QuickUtils.sendMessage((Player) sender, "COMMAND-ERROR");
            }
            return;
        }

        String wishName = args[1];
        String playerName = args[2];
        double guaranteed;

        try {
            guaranteed = Double.parseDouble(args[3]);
        } catch (NumberFormatException exception) {
            if (isConsole) {
                QuickUtils.sendMessage(sender, "MUST-NUMBER");
            } else {
                QuickUtils.sendMessage((Player) sender, "MUST-NUMBER");
            }
            return;
        }

        if (!WishManager.hasWish(wishName)) {
            if (isConsole) {
                QuickUtils.sendMessage(sender, "WISH-NOT-HAVE");
            } else {
                QuickUtils.sendMessage((Player) sender, "WISH-NOT-HAVE");
            }
            return;
        }

        WishManager.setPlayerWishGuaranteed(playerName, wishName, WishManager.getPlayerWishGuaranteed(playerName, wishName) + guaranteed);

        if (isConsole) {
            QuickUtils.sendMessage(sender, "DONE");
        } else {
            QuickUtils.sendMessage((Player) sender, "DONE");
        }
    }

    @SubCommand("addLimitAmount")
    public void handleAddLimitAmountCommand(CommandSender sender, String[] args) {
        boolean isConsole = isConsole(sender, false);

        if (!isAdmin(sender, true)) {
            return;
        }

        if (args.length < 4) {
            if (isConsole) {
                QuickUtils.sendMessage(sender, "COMMAND-ERROR");
            } else {
                QuickUtils.sendMessage((Player) sender, "COMMAND-ERROR");
            }
            return;
        }

        String wishName = args[1];
        String playerName = args[2];
        int limitAmount;

        try {
            limitAmount = Integer.parseInt(args[3]);
        } catch (NumberFormatException exception) {
            if (isConsole) {
                QuickUtils.sendMessage(sender, "MUST-NUMBER");
            } else {
                QuickUtils.sendMessage((Player) sender, "MUST-NUMBER");
            }
            return;
        }

        if (!WishManager.hasWish(wishName)) {
            if (isConsole) {
                QuickUtils.sendMessage(sender, "WISH-NOT-HAVE");
            } else {
                QuickUtils.sendMessage((Player) sender, "WISH-NOT-HAVE");
            }
            return;
        }

        if (!WishManager.isEnabledWishLimit(wishName)) {
            if (isConsole) {
                QuickUtils.sendMessage(sender, "WISH-NOT-ENABLED-LIMIT");
            } else {
                QuickUtils.sendMessage((Player) sender, "WISH-NOT-ENABLED-LIMIT");
            }
            return;
        }

        WishManager.setPlayerWishLimitAmount(playerName, wishName, WishManager.getPlayerWishLimitAmount(playerName, wishName) + limitAmount);

        if (isConsole) {
            QuickUtils.sendMessage(sender, "DONE");
        } else {
            QuickUtils.sendMessage((Player) sender, "DONE");
        }
    }

    @SubCommand("resetLimitAmount")
    public void handleResetLimitAmountCommand(CommandSender sender, String[] args) {
        boolean isConsole = isConsole(sender, false);

        if (!isAdmin(sender, true)) {
            return;
        }

        if (args.length == 1) {
            if (isConsole) {
                QuickUtils.sendMessage(sender, "WISH-NULL");
            } else {
                QuickUtils.sendMessage((Player) sender, "WISH-NULL");
            }
            return;
        }

        String wishName = args[1];

        if (!WishManager.hasWish(wishName)) {
            if (isConsole) {
                QuickUtils.sendMessage(sender, "WISH-NOT-HAVE");
            } else {
                QuickUtils.sendMessage((Player) sender, "WISH-NOT-HAVE");
            }
            return;
        }

        if (!WishManager.isEnabledWishLimit(wishName)) {
            if (isConsole) {
                QuickUtils.sendMessage(sender, "WISH-NOT-ENABLED-LIMIT");
            } else {
                QuickUtils.sendMessage((Player) sender, "WISH-NOT-ENABLED-LIMIT");
            }
            return;
        }

        WishManager.resetWishLimitAmount(wishName);

        if (isConsole) {
            QuickUtils.sendMessage(sender, "DONE");
        } else {
            QuickUtils.sendMessage((Player) sender, "DONE");
        }
    }

    @SubCommand("queryLogs")
    public void handleQueryLogsCommand(CommandSender sender, String[] args) {
        boolean isConsole = isConsole(sender, false);

        if (!isAdmin(sender, true)) {
            return;
        }

        if (args.length < 4) {
            if (isConsole) {
                QuickUtils.sendMessage(sender, "COMMAND-ERROR");
            } else {
                QuickUtils.sendMessage((Player) sender, "COMMAND-ERROR");
            }
            return;
        }

        String name = args[1];
        String queryPlayerUUID = QuickUtils.getPlayerUUID(name);

        int startNumber;
        int endNumber;

        try {
            startNumber = Integer.parseInt(args[2]);
            endNumber = Integer.parseInt(args[3]);
        } catch (Exception exception) {
            if (isConsole) {
                QuickUtils.sendMessage(sender, "MUST-NUMBER");
            } else {
                QuickUtils.sendMessage((Player) sender, "MUST-NUMBER");
            }
            return;
        }

        int allLogsSize = LogManager.getPlayerWishLogSize(queryPlayerUUID);
        ConcurrentLinkedQueue<String> logs = LogManager.getPlayerWishLog(queryPlayerUUID, startNumber, endNumber);

        if (logs.size() == 0 || allLogsSize <= 0) {
            if (isConsole) {
                QuickUtils.sendMessage(sender, "QUERY-WISH.LOGS-NULL");
            } else {
                QuickUtils.sendMessage((Player) sender, "QUERY-WISH.LOGS-NULL");
            }
            return;
        }

        // 头消息
        if (isConsole) {
            QuickUtils.sendMessage(sender, "QUERY-WISH.PREFIX", "<size>", logs.size(), "<allSize>", allLogsSize);
        } else {
            QuickUtils.sendMessage((Player) sender, "QUERY-WISH.PREFIX", "<size>", logs.size(), "<allSize>", allLogsSize);
        }

        // 日志消息
        for (String queryLog : logs) {
            String[] queryLogSplit = queryLog.split(";");

            String queryLogTime = queryLogSplit[0].replace("-", " ");
            String queryLogPlayerName = queryLogSplit[1];
            String queryLogWishName = StringEncrypter.decrypt(queryLogSplit[3]);
            String queryLogDoList = queryLogSplit[4];

            if (isConsole) {
                QuickUtils.sendMessage(sender, "QUERY-WISH.QUERY"
                        , "<size>", logs.size()
                        , "<allSize>", allLogsSize
                        , "<targetPlayer>", queryLogPlayerName
                        , "<targetPlayerUUID>", queryPlayerUUID
                        , "<time>", queryLogTime
                        , "<file>", queryLogWishName
                        , "<node>", queryLogDoList);
            } else {
                QuickUtils.sendMessage((Player) sender, "QUERY-WISH.QUERY"
                        , "<size>", logs.size()
                        , "<allSize>", allLogsSize
                        , "<targetPlayer>", queryLogPlayerName
                        , "<targetPlayerUUID>", queryPlayerUUID
                        , "<time>", queryLogTime
                        , "<file>", queryLogWishName
                        , "<node>", queryLogDoList);
            }
        }

        // 尾消息
        if (isConsole) {
            QuickUtils.sendMessage(sender, "QUERY-WISH.SUFFIX"
                    , "<size>", logs.size()
                    , "<allSize>", allLogsSize);
        } else {
            QuickUtils.sendMessage((Player) sender, "QUERY-WISH.SUFFIX"
                    , "<size>", logs.size()
                    , "<allSize>", allLogsSize);
        }
    }

    @SubCommand("reload")
    public void handleReloadCommand(CommandSender sender, String[] args) {
        boolean isConsole = isConsole(sender, false);

        if (!isAdmin(sender, true)) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            RegisterManager.reload();

            if (isConsole) {
                QuickUtils.sendMessage(sender, "DONE");
            } else {
                QuickUtils.sendMessage((Player) sender, "DONE");
            }
        });
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        // 异步调用指令处理方法
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                subCommandMap.get(args[0].toLowerCase(), k -> null).invoke(this, sender, args);
            } catch (NullPointerException | ArrayIndexOutOfBoundsException exception) {
                QuickUtils.sendMessage(sender, "HELP", "<commandName>", this.getName());
            } catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException exception) {
                exception.printStackTrace();
            }
        });

        return true;
    }

    @NotNull
    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        // 如果没有子指令
        if (args.length == 1) {
            return new ArrayList<>(subCommandMap.asMap().keySet());
        }

        ArrayList<String> tabCompletions = new ArrayList<>(RegisterManager.getRegisterWish());
        Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .forEach(tabCompletions::add);

        return StringUtil.copyPartialMatches(args[args.length-1], tabCompletions, new ArrayList<>());
    }

    /**
     * SubCommand 注解。
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface SubCommand {
        String value();
    }

    /**
     * 是否为控制台。
     *
     * @param sender 指令发送对象
     * @param sendMessage 是否发送消息
     * @return 是否为控制台
     */
    private static boolean isConsole(CommandSender sender, boolean sendMessage) {
        if (sender instanceof Player) {
            return false;
        }

        if (sendMessage) {
            sender.sendMessage(" ");
            sender.sendMessage(QuickUtils.translate("&e此服务器正在使用 Advanced Wish 插件。 版本: " + plugin.getDescription().getVersion() + ", 作者: 2000000。"));
            sender.sendMessage(" ");
        }

        return true;
    }

    /**
     * 是否拥有管理员权限。
     *
     * @param sender 指令发送对象
     * @param sendMessage 是否发送消息
     * @return 是否拥有管理员权限
     */
    private static boolean isAdmin(CommandSender sender, boolean sendMessage) {
        if (!(sender instanceof Player)) {
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission(ConfigManager.getAdvancedWishYaml().getString("ADMIN-PERM"))) {
            if (sendMessage) {
                ConfigManager.getMessageYaml().getStringList("NO-PERM").forEach(message -> player.sendMessage(QuickUtils.handleString(message, player)));
            }

            return false;
        }

        return true;
    }

    /**
     * 是否拥有管理员权限。
     *
     * @param player 玩家
     * @param sendMessage 是否发送消息
     * @return 是否拥有管理员权限
     */
    private static boolean isAdmin(Player player, boolean sendMessage) {
        if (!player.hasPermission(ConfigManager.getAdvancedWishYaml().getString("ADMIN-PERM"))) {
            if (sendMessage) {
                ConfigManager.getMessageYaml().getStringList("NO-PERM").forEach(message -> player.sendMessage(QuickUtils.handleString(message, player)));
            }

            return false;
        }

        return true;
    }

    /**
     * 玩家是否在线。
     *
     * @param sender 指令发送对象
     * @param targetPlayer 检查的玩家
     * @param sendMessage 是否发送消息
     * @return 玩家是否在线
     */
    private static boolean isPlayerOnline(Player sender, Player targetPlayer, boolean sendMessage) {
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            if (sendMessage) {
                ConfigManager.getMessageYaml().getStringList("PLAYER-OFFLINE").stream()
                        .map(message -> QuickUtils.handleString(message, sender))
                        .forEach(sender::sendMessage);
            }

            return false;
        }

        return true;
    }

    /**
     * 玩家是否在线。
     *
     * @param sender 指令发送对象
     * @param targetPlayer 检查的玩家
     * @param sendMessage 是否发送消息
     * @return 玩家是否在线
     */
    private static boolean isPlayerOnline(CommandSender sender, Player targetPlayer, boolean sendMessage) {
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            if (sendMessage) {
                ConfigManager.getMessageYaml().getStringList("PLAYER-OFFLINE").stream()
                        .map(QuickUtils::handleString)
                        .forEach(sender::sendMessage);
            }

            return false;
        }

        return true;
    }
}
