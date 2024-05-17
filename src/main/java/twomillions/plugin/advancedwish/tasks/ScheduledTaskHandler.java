package twomillions.plugin.advancedwish.tasks;

import lombok.Getter;
import lombok.Setter;
import twomillions.plugin.advancedwish.Main;
import twomillions.plugin.advancedwish.abstracts.TasksAbstract;
import twomillions.plugin.advancedwish.managers.WishManager;
import twomillions.plugin.advancedwish.managers.effects.EffectSendManager;
import twomillions.plugin.advancedwish.managers.tasks.ScheduledTaskManager;
import twomillions.plugin.advancedwish.utils.texts.QuickUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 该类继承 {@link TasksAbstract}，用于处理玩家正常任务。
 *
 * @author 2000000
 * @date 2022/11/24 16:49
 */
@Getter @Setter
@SuppressWarnings("unused")
public class ScheduledTaskHandler extends TasksAbstract {
    private final Runnable runnable;
    private static final JavaPlugin plugin = Main.getInstance();

    /**
     * 获取实例。
     */
    @Getter private static final ScheduledTaskHandler scheduledTaskHandler = new ScheduledTaskHandler();

    /**
     * 构造器。
     */
    private ScheduledTaskHandler() {
        runnable = () -> Bukkit.getOnlinePlayers().forEach(player -> {
            // 获取玩家的任务列表
            ConcurrentLinkedQueue<String> playerScheduledTasks = ScheduledTaskManager.getPlayerScheduledTasks(player);

            // 检查修复和移除许愿列表
            checkRepairAndRemove(playerScheduledTasks, player);

            // 遍历执行任务
            executeScheduledTasks(playerScheduledTasks, player);
        });
    }

    /**
     * 开始任务。
     */
    @Override
    public void startTask() {
        setBukkitTask(Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, 0, 0));
    }

    /**
     * 检查任务列表，如果任务为空但是玩家在许愿池中则将其移除，如果任务不为空但玩家不在许愿池中则将其加入。
     *
     * @param playerScheduledTasks 玩家任务列表
     * @param player 玩家
     */
    private void checkRepairAndRemove(ConcurrentLinkedQueue<String> playerScheduledTasks, Player player) {
        if (playerScheduledTasks.size() > 0 && !WishManager.isPlayerInWishList(player)) {
            WishManager.addPlayerToWishList(player);
        }

        if (playerScheduledTasks.size() == 0 && WishManager.isPlayerInWishList(player)) {
            WishManager.removePlayerWithWishList(player);
        }
    }

    /**
     * 遍历执行任务，如果任务时间戳小于当前时间则将其移除，并执行任务效果。
     *
     * @param playerScheduledTasks 玩家任务列表
     * @param player 玩家
     */
    private void executeScheduledTasks(ConcurrentLinkedQueue<String> playerScheduledTasks, Player player) {
        UUID uuid = player.getUniqueId();

        if (PlayerCacheHandler.isLoadingCache(uuid) || PlayerCacheHandler.isWaitingLoadingCache(uuid)) {
            return;
        }

        Iterator<String> iterator = playerScheduledTasks.iterator();
        while (iterator.hasNext()) {
            String scheduledTask = iterator.next();
            String[] scheduledTaskSplit = scheduledTask.split(";");

            long currentTimeMillis = System.currentTimeMillis();
            long time = QuickUtils.handleLong(scheduledTaskSplit[0], player);

            // 若任务时间戳大于当前时间则跳过
            if (time > currentTimeMillis) {
                continue;
            }

            iterator.remove();

            String fileName = scheduledTaskSplit[1];
            String path = scheduledTaskSplit[2];
            String node = scheduledTaskSplit[4];

            // 发送任务效果
            EffectSendManager.sendEffect(fileName, player, path, node);
        }
    }
}
