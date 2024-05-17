package twomillions.plugin.advancedwish.tasks;

import de.leonhard.storage.Yaml;
import lombok.Getter;
import lombok.Setter;
import twomillions.plugin.advancedwish.Main;
import twomillions.plugin.advancedwish.abstracts.TasksAbstract;
import twomillions.plugin.advancedwish.events.AsyncWishLimitResetEvent;
import twomillions.plugin.advancedwish.managers.WishManager;
import twomillions.plugin.advancedwish.managers.config.ConfigManager;
import twomillions.plugin.advancedwish.managers.databases.DatabasesManager;
import twomillions.plugin.advancedwish.managers.tasks.ScheduledTaskManager;
import twomillions.plugin.advancedwish.utils.events.EventUtils;
import twomillions.plugin.advancedwish.utils.others.ConstantsUtils;
import twomillions.plugin.advancedwish.utils.texts.QuickUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 该类继承 {@link TasksAbstract}，用于处理许愿池限制次数重置。
 *
 * @author 2000000
 * @date 2023/1/9 15:08
 */
@Getter @Setter
public class WishLimitResetHandler extends TasksAbstract {
    private final String wishName;
    private final Runnable runnable;
    private static final JavaPlugin plugin = Main.getInstance();

    @Getter private static ConcurrentLinkedQueue<BukkitTask> wishLimitResetTaskList = new ConcurrentLinkedQueue<>();

    /**
     * 构造器。
     *
     * @param wishName 许愿池的名称
     */
    public WishLimitResetHandler(String wishName) {
        this.wishName = wishName;
        this.runnable = () -> {
            // 读取
            boolean isResetCompleteSendEnabled = QuickUtils.handleBoolean(WishManager.isResetCompleteSendEnabled(wishName));
            boolean isResetCompleteSendConsoleEnabled = QuickUtils.handleBoolean(WishManager.isResetCompleteSendConsoleEnabled(wishName));

            String storeMode = DatabasesManager.getDataStorageType().toString();

            // 调用异步重置事件
            AsyncWishLimitResetEvent event = EventUtils.callAsyncWishLimitResetEvent(wishName, storeMode
                    , isResetCompleteSendEnabled, isResetCompleteSendConsoleEnabled);

            // 如果事件被取消了，则退出方法
            if (event.isCancelled()) {
                return;
            }

            // 重置许愿池的限制许愿次数
            WishManager.resetWishLimitAmount(wishName);

            // 发送效果
            if (isResetCompleteSendEnabled) {
                Yaml yaml = ConfigManager.createYaml(wishName, ConstantsUtils.WISH, false, false);

                Bukkit.getOnlinePlayers().forEach(player ->
                        ScheduledTaskManager.createPlayerScheduledTasks(player,
                                yaml.getStringList("ADVANCED-SETTINGS.WISH-LIMIT.RESET-COMPLETE")));
            }

            // 控制台发送提示信息
            if (isResetCompleteSendConsoleEnabled) {
                QuickUtils.sendConsoleMessage("&aAdvanced Wish 已清除 &e" + wishName + " &a许愿池玩家限制许愿次数! 存储方式: &e" + storeMode);
            }
        };
    }

    /**
     * 开始为指定的许愿池创建一个限制许愿次数的定时器。
     * 如果已开启限制许愿功能，每隔一定时间就会自动清除玩家的限制许愿次数。
     */
    @Override
    public void startTask() {
        // 读取
        int wishResetLimitStart = QuickUtils.handleInt(WishManager.getWishResetLimitStart(wishName)) * 20;
        int wishResetLimitCycle = QuickUtils.handleInt(WishManager.getWishResetLimitCycle(wishName)) * 20;

        BukkitTask resetBukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, wishResetLimitStart, wishResetLimitCycle);

        // 将任务添加到列表中
        wishLimitResetTaskList.add(resetBukkitTask);
    }

    /**
     * 在 reload 的时候结束所有的任务。
     */
    public static void cancelAllWishLimitResetTasks() {
        for (BukkitTask bukkitTask : wishLimitResetTaskList) {
            bukkitTask.cancel();
            wishLimitResetTaskList.remove(bukkitTask);
        }
    }
}
