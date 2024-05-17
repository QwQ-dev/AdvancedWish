package twomillions.plugin.advancedwish.managers.register;

import de.leonhard.storage.Yaml;
import lombok.Getter;
import lombok.Setter;
import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import twomillions.plugin.advancedwish.Main;
import twomillions.plugin.advancedwish.commands.AdvancedWishCommand;
import twomillions.plugin.advancedwish.enums.databases.types.DataStorageType;
import twomillions.plugin.advancedwish.listeners.PlayerListener;
import twomillions.plugin.advancedwish.managers.WishManager;
import twomillions.plugin.advancedwish.managers.config.ConfigManager;
import twomillions.plugin.advancedwish.managers.databases.DatabasesManager;
import twomillions.plugin.advancedwish.managers.placeholder.PapiManager;
import twomillions.plugin.advancedwish.tasks.WishLimitResetHandler;
import twomillions.plugin.advancedwish.utils.commands.CommandUtils;
import twomillions.plugin.advancedwish.utils.exceptions.ExceptionUtils;
import twomillions.plugin.advancedwish.utils.others.ConstantsUtils;
import twomillions.plugin.advancedwish.utils.scripts.ScriptUtils;
import twomillions.plugin.advancedwish.utils.scripts.interop.ScriptCommandHandler;
import twomillions.plugin.advancedwish.utils.scripts.interop.ScriptEventHandler;
import twomillions.plugin.advancedwish.utils.scripts.interop.ScriptPlaceholderExpander;
import twomillions.plugin.advancedwish.utils.scripts.interop.ScriptTaskScheduler;
import twomillions.plugin.advancedwish.utils.texts.QuickUtils;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 插件注册处理类。
 *
 * @author 2000000
 * @date 2022/11/24 19:01
 */
public class RegisterManager {
    private static final JavaPlugin plugin = Main.getInstance();

    /**
     * 注册的许愿池列表。
     */
    @Getter private static final ConcurrentLinkedQueue<String> registerWish = new ConcurrentLinkedQueue<>();

    /**
     * Economy 对象。
     */
    @Getter @Setter private volatile static Economy economy;

    /**
     * 是否使用 PlaceholderAPI.
     */
    @Getter @Setter private volatile static boolean usingPapi;

    /**
     * 是否使用 Vulpecula.
     */
    @Getter @Setter private volatile static boolean usingVulpecula;

    /**
     * PlayerPointsAPI 对象。
     */
    @Getter @Setter private volatile static PlayerPointsAPI playerPointsAPI;

    /**
     * 数据迁移检查。
     *
     * @param dataStorageType DATA-STORAGE-TYPE
     * @return 是否开启了迁移
     */
    public static boolean dataMigration(String dataStorageType) {
        if (!dataStorageType.contains(":")) {
            return false;
        }

        String[] dataStorageTypeSplit = dataStorageType.split(":");

        if (dataStorageTypeSplit.length > 2) {
            ExceptionUtils.throwUnknownDataStoreType();
            return true;
        }

        DataStorageType from = DataStorageType.valueOfIgnoreCase(dataStorageTypeSplit[0]);
        DataStorageType to = DataStorageType.valueOfIgnoreCase(dataStorageTypeSplit[1]);

        if (from == to) {
            QuickUtils.sendConsoleMessage("&a原存储类型与新存储类型相同，请检查配置文件是否正确! 即将关闭服务器!");
            Bukkit.shutdown();
            return true;
        }

        if (DatabasesManager.dataMigration(ConfigManager.getAdvancedWishYaml(), from, to)) {
            QuickUtils.sendConsoleMessage("&a数据迁移完成! 即将关闭服务器!");
        } else {
            QuickUtils.sendConsoleMessage("&c数据迁移出错，没有可迁移数据? 迁移或初始化错误? 即将关闭服务器!");
        }

        Bukkit.shutdown();
        return true;
    }

    /**
     * 选择数据存储方式。
     *
     * @param dataStorageType DATA-STORAGE-TYPE
     * @return 若选择的方式无法找到则返回 false，否则初始化完成返回 true
     */
    public static boolean selectDataStorageType(String dataStorageType) {
        switch (dataStorageType) {
            case ConstantsUtils.MYSQL_DB_TYPE:
                DatabasesManager.setDataStorageType(DataStorageType.MySQL);
                DatabasesManager.getDatabasesManager().setup(ConfigManager.getAdvancedWishYaml());
                return true;

            case ConstantsUtils.MONGODB_DB_TYPE:
                DatabasesManager.setDataStorageType(DataStorageType.MongoDB);
                DatabasesManager.getDatabasesManager().setup(ConfigManager.getAdvancedWishYaml());
                return true;

            case ConstantsUtils.JSON_DB_TYPE:
                DatabasesManager.setDataStorageType(DataStorageType.Json);
                return true;

            default:
                ExceptionUtils.throwUnknownDataStoreType();
                return false;
        }
    }

    /**
     * 注册指令。
     */
    public static void registerCommands() {
        CommandUtils.registerCommand(AdvancedWishCommand.getAdvancedWishCommand());
    }

    /**
     * 注册监听器。
     */
    public static void registerListener() {
        PluginManager manager = Bukkit.getPluginManager();
        manager.registerEvents(new PlayerListener(), plugin);
    }

    /**
     * 设置路径。
     */
    public static void setupPath() {
        String pluginPath = Main.getPluginPath();

        String logsConfig = ConfigManager.getAdvancedWishYaml().getString("LOGS-PATH");
        String guaranteedConfig = ConfigManager.getAdvancedWishYaml().getString("GUARANTEED-PATH");
        String doListCacheConfig = ConfigManager.getAdvancedWishYaml().getString("DO-LIST-CACHE-PATH");
        String otherDataConfig = ConfigManager.getAdvancedWishYaml().getString("OTHER-DATA-PATH");
        String scriptConfig = ConfigManager.getAdvancedWishYaml().getString("SCRIPT-PATH");

        Main.setLogsPath(logsConfig.isEmpty() ? pluginPath + ConstantsUtils.PLAYER_LOGS : logsConfig);
        Main.setGuaranteedPath(guaranteedConfig.isEmpty() ? pluginPath + ConstantsUtils.PLAYER_GUARANTEED : guaranteedConfig);
        Main.setDoListCachePath(doListCacheConfig.isEmpty() ? pluginPath + ConstantsUtils.PLAYER_CACHE : doListCacheConfig);
        Main.setOtherDataPath(otherDataConfig.isEmpty() ? pluginPath + ConstantsUtils.OTHER_DATA : otherDataConfig);
        Main.setScriptPath(scriptConfig.isEmpty() ? pluginPath + ConstantsUtils.SCRIPT_FILE : scriptConfig);
    }

    /**
     * 设置 Placeholder API.
     */
    public static void setupPlaceholderAPI() {
        Plugin vulpecula = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");

        if (vulpecula == null) {
            return;
        }

        setUsingPapi(true);

        Bukkit.getScheduler().runTask(plugin, () -> new PapiManager().register());

        QuickUtils.sendConsoleMessage("&a检查到服务器存在 &ePlaceholderAPI&a，已注册 &ePlaceholderAPI&a 变量。");
    }

    /**
     * 设置 Vulpecula.
     */
    public static void setupVulpecula() {
        Plugin vulpecula = Bukkit.getPluginManager().getPlugin("Vulpecula");

        if (vulpecula == null) {
            return;
        }

        setUsingVulpecula(true);

        QuickUtils.sendConsoleMessage("&a检查到服务器存在 &eVulpecula&a，已支持使用 &eKether&a 脚本。");
    }

    /**
     * 设置 bStats.
     */
    public static void setupBstats() {
        if (!ConfigManager.getAdvancedWishYaml().getOrDefault("BSTATS", true)) {
            new Metrics(plugin, 16990);
        }
    }

    /**
     * 设置 Vault.
     */
    public static void setupVault() {
        Plugin vault = Bukkit.getPluginManager().getPlugin("Vault");

        if (vault == null) {
            return;
        }

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);

        if (rsp == null) {
            QuickUtils.sendConsoleMessage("&c检查到服务器存在 &eVault&c，但并没有实际插件进行操作? 取消对于 &eVault&c 的设置。");
            return;
        }

        try {
            setEconomy(rsp.getProvider());
            QuickUtils.sendConsoleMessage("&a检查到服务器存在 &eVault&a，已成功设置 &eVault&a。");
        } catch (Throwable exception) {
            QuickUtils.sendConsoleMessage("&c检查到服务器存在 &eVault&c，但 &eVault&c 设置错误，这是最新版吗? 请尝试更新它: &ehttps://www.spigotmc.org/resources/vault.34315/&c，服务器即将关闭。");
            Bukkit.shutdown();
        }
    }

    /**
     * 设置 PlayerPoints.
     */
    public static void setupPlayerPoints() {
        Plugin playerPoints = Bukkit.getPluginManager().getPlugin("PlayerPoints");

        if (playerPoints == null) {
            return;
        }

        try {
            setPlayerPointsAPI(((PlayerPoints) playerPoints).getAPI());
            QuickUtils.sendConsoleMessage("&a检查到服务器存在 &ePlayerPoints&a，已成功设置 &ePlayerPoints&a。");
        } catch (Throwable e) {
            QuickUtils.sendConsoleMessage("&c检查到服务器存在 &ePlayerPoints&c，但 &ePlayerPoints&c 设置错误，这是最新版吗? 请尝试更新它: &ehttps://www.spigotmc.org/resources/playerpoints.80745/&c，服务器即将关闭。");
            Bukkit.shutdown();
        }
    }

    /**
     * 注册所有许愿池，并检查许愿池是否启用许愿限制，启用则创建异步计划任务。
     */
    public static void registerWish() {
        registerWish.clear();
        List<String> wishList = ConfigManager.getAdvancedWishYaml().getStringList("WISH");

        for (String wishName : wishList) {
            if (wishName == null || wishName.trim().isEmpty()) continue;

            Yaml yaml = ConfigManager.createYaml(wishName, ConstantsUtils.WISH, false, true);

            if (!ConfigManager.checkLastVersion(yaml)) {
                continue;
            }

            registerWish.add(wishName);

            QuickUtils.sendConsoleMessage("&a已成功加载许愿池! 许愿池文件名称: &e" + wishName);

            // 许愿限制
            if (WishManager.isEnabledWishLimit(wishName)) {
                if (WishLimitResetHandler.getWishLimitResetTaskList().isEmpty()) {
                    new WishLimitResetHandler(wishName).startTask();
                    QuickUtils.sendConsoleMessage("&a检查到许愿池启用了许愿限制，已成功创建对应异步计划任务! 许愿池文件名称: &e" + wishName);
                }
            }
        }
    }

    /**
     * 注销所有 JavaScript 互操作内容。
     */
    public static void unregisterAllScriptInterop() {
        /*
         * ScriptPlaceholderExpander
         */
        if (RegisterManager.isUsingPapi()) {
            for (ScriptPlaceholderExpander scriptPlaceholder : ScriptPlaceholderExpander.getScriptPlaceholders()) {
                scriptPlaceholder.unregister();
            }
        }

        /*
         * ScriptTaskScheduler
         */
        for (ScriptTaskScheduler scriptScheduler : ScriptTaskScheduler.getScriptSchedulers()) {
            scriptScheduler.unregister();
        }

        /*
         * ScriptEventHandler
         */
        for (ScriptEventHandler scriptListener : ScriptEventHandler.getScriptListeners()) {
            scriptListener.unregister();
        }

        /*
         * ScriptCommandHandler
         */
        for (ScriptCommandHandler scriptCommandHandler : ScriptCommandHandler.getScriptCommandHandler()) {
            scriptCommandHandler.unregister();
        }

        if (ScriptUtils.getRhino() != null) {
            ScriptUtils.getRhino().close();
        }
    }

    /**
     * Reload 方法。
     */
    public static void reload() {
        // 取消任务
        if (ConfigManager.getAdvancedWishYaml().getBoolean("WISH-LIMIT-TASK-RESTART")) {
            WishLimitResetHandler.cancelAllWishLimitResetTasks();
        }

        if (isUsingPapi()) {
            try {
                new PapiManager().unregister();
            } catch (Throwable throwable) {
                QuickUtils.sendConsoleMessage("&ePlaceholder&c 重载异常，这是最新版吗? 请尝试更新它: &ehttps://www.spigotmc.org/resources/placeholderapi.6245/&c，已取消 &ePlaceholder&c 重载。");
            }
        }

        unregisterAllScriptInterop();

        setupVault();
        setupPlayerPoints();
        setupVulpecula();
        setupPlaceholderAPI();

        // 初始化 Script
        try {
            ScriptUtils.setup();
        } catch (Throwable throwable) {
            ExceptionUtils.throwRhinoError(throwable);
        }

        registerWish();
    }
}
