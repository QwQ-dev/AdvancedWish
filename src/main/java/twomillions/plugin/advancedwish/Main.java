package twomillions.plugin.advancedwish;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import twomillions.plugin.advancedwish.commands.AdvancedWishCommand;
import twomillions.plugin.advancedwish.enums.databases.types.DataStorageType;
import twomillions.plugin.advancedwish.managers.WishManager;
import twomillions.plugin.advancedwish.managers.config.ConfigManager;
import twomillions.plugin.advancedwish.managers.databases.DatabasesManager;
import twomillions.plugin.advancedwish.managers.placeholder.PapiManager;
import twomillions.plugin.advancedwish.managers.register.RegisterManager;
import twomillions.plugin.advancedwish.tasks.PlayerCacheHandler;
import twomillions.plugin.advancedwish.tasks.ScheduledTaskHandler;
import twomillions.plugin.advancedwish.tasks.UpdateHandler;
import twomillions.plugin.advancedwish.tasks.WishLimitResetHandler;
import twomillions.plugin.advancedwish.utils.commands.CommandUtils;
import twomillions.plugin.advancedwish.utils.exceptions.ExceptionUtils;
import twomillions.plugin.advancedwish.utils.scripts.ScriptUtils;
import twomillions.plugin.advancedwish.utils.texts.QuickUtils;

import java.sql.SQLException;
import java.util.Arrays;

/**
 * 该类继承 {@link JavaPlugin}，插件主类。
 *
 * @author 2000000
 * @date 2022/11/21 12:00
 */
public final class Main extends JavaPlugin {
    @Getter @Setter private volatile static Main instance;
    @Getter @Setter private volatile static Double serverVersion;

    @Getter @Setter private volatile static String pluginPath;

    @Getter @Setter private volatile static String logsPath;
    @Getter @Setter private volatile static String scriptPath;
    @Getter @Setter private volatile static String guaranteedPath;
    @Getter @Setter private volatile static String doListCachePath;
    @Getter @Setter private volatile static String otherDataPath;

    @Getter private static final boolean isOfflineMode = Bukkit.getServer().getOnlineMode();

    @Getter private static final String packageName = Main.class.getPackage().getName();
    @Getter private static final String codeSourceLocation = Main.class.getProtectionDomain().getCodeSource().getLocation().toString();

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();

        setInstance(this);
        setPluginPath(getInstance().getDataFolder().toString().replace("\\", "/"));

        /*
         * 获取 -> org.bukkit.craftbukkit.v1_7_R4，分割后为 -> 1_7, 最终为 -> 107
         * 1.12.2 -> 101202 1.19.2 -> 101902 这里把 _ 换成 0 是为了放置 1.19 比 1.7 小的问题
         */
        setServerVersion(Double.parseDouble(Arrays.toString(org.apache.commons.lang.StringUtils.substringsBetween(getServer().getClass().getPackage().getName(), ".v", "_R"))
                .replace("_", "0").replace("[", "").replace("]", "")));

        /*
         * 版本检查
         */
        if (!ConfigManager.checkLastVersion(ConfigManager.getMessageYaml()) || !ConfigManager.checkLastVersion(ConfigManager.getAdvancedWishYaml())) {
            return;
        }

        /*
         * 各种设置
         */
        RegisterManager.setupPath();
        RegisterManager.setupBstats();
        RegisterManager.setupVault();
        RegisterManager.setupPlayerPoints();
        RegisterManager.setupVulpecula();
        RegisterManager.setupPlaceholderAPI();

        // 初始化 Script
        try {
            ScriptUtils.setup();
        } catch (Throwable throwable) {
            ExceptionUtils.throwRhinoError(throwable);
        }

        RegisterManager.registerWish();
        RegisterManager.registerCommands();
        RegisterManager.registerListener();

        /*
         * 设置数据存储
         */
        String dataStorageType = ConfigManager.getAdvancedWishYaml().getString("DATA-STORAGE-TYPE").toLowerCase();

        /*
         * 数据迁移
         */
        if (RegisterManager.dataMigration(dataStorageType)) {
            return;
        }

        /*
         * 选择数据存储
         */
        if (!RegisterManager.selectDataStorageType(dataStorageType)) {
            return;
        }

        /*
         * 解析 scriptSetup 函数
         */
        ScriptUtils.invokeFunctionInAllScripts("scriptSetup", null);

        /*
         * 任务处理
         */
        ScheduledTaskHandler.getScheduledTaskHandler().startTask();
        UpdateHandler.getUpdateHandler().startTask();

        /*
         * 如果插件开启时有玩家既是热重载等，检查玩家缓存重新开始任务
         */
        if (!Bukkit.getOnlinePlayers().isEmpty()) {
            Bukkit.getOnlinePlayers().forEach(player -> new PlayerCacheHandler(player).startTask());
        }

        long durationMillis = System.currentTimeMillis() - startTime;
        QuickUtils.sendConsoleMessage("&e&lAdvanced Wish&a 插件已成功加载! 版本: &e" + getDescription().getVersion() + "&a, 作者: &e2000000&a。加载用时: &e" + durationMillis + " &ams!");
    }

    @Override
    public void onDisable() {
        ScriptUtils.invokeFunctionInAllScripts("onDisable", null);

        // 取消任务
        ScheduledTaskHandler.getScheduledTaskHandler().cancelTask();
        UpdateHandler.getUpdateHandler().cancelTask();
        WishLimitResetHandler.cancelAllWishLimitResetTasks();

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            WishManager.savePlayerCacheData(onlinePlayer);
        }

        if (DatabasesManager.getDataStorageType() == DataStorageType.MySQL) {
            try {
                DatabasesManager.getMySQLManager().getDataSource().close();
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        }

        if (RegisterManager.isUsingPapi()) {
            try {
                new PapiManager().unregister();
            } catch (Throwable throwable) {
                QuickUtils.sendConsoleMessage("&ePlaceholder&c 注销异常，这是最新版吗? 请尝试更新它: &ehttps://www.spigotmc.org/resources/placeholderapi.6245/&c，已取消 &ePlaceholder&c 注销。");
            }
        }

        RegisterManager.unregisterAllScriptInterop();

        CommandUtils.unregister(AdvancedWishCommand.getAdvancedWishCommand());

        Bukkit.getScheduler().cancelTasks(this);

        QuickUtils.sendConsoleMessage("&e&lAdvanced Wish&a 插件已成功卸载! 感谢您使用此插件! 版本: &e" + getDescription().getVersion() + "&a, 作者: &e2000000&a。");
    }
}
