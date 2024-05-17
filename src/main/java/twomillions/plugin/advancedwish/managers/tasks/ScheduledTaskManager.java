package twomillions.plugin.advancedwish.managers.tasks;

import com.github.benmanes.caffeine.cache.Cache;
import twomillions.plugin.advancedwish.Main;
import twomillions.plugin.advancedwish.managers.WishManager;
import twomillions.plugin.advancedwish.tasks.ScheduledTaskHandler;
import twomillions.plugin.advancedwish.utils.others.CaffeineUtils;
import twomillions.plugin.advancedwish.utils.scripts.ScriptUtils;
import twomillions.plugin.advancedwish.utils.texts.QuickUtils;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 提供计划任务的增删查操作等等。
 *
 * @author 2000000
 * @date 2023/2/18
 */
@SuppressWarnings("unused")
public class ScheduledTaskManager {
    private static final JavaPlugin plugin = Main.getInstance();

    /**
     * 用 Map 存储每个玩家的计划任务，Key 为玩家的 UUID，value 为 List，存储每个计划任务的字符串表示。
     */
    private static final Cache<UUID, ConcurrentLinkedQueue<String>> playerScheduledTasks = CaffeineUtils.buildBukkitCache();

    /**
     * 将计划任务的各项信息转换为字符串。
     *
     * @param time 计划任务执行时间
     * @param fileName 计划任务文件名
     * @param path 计划任务执行路径
     * @param originalPath 计划任务路径是否为原始路径
     * @param node 计划任务执行节点
     * @return 计划任务字符串
     */
    public static String toScheduledTask(long time, String fileName, String path, boolean originalPath, String node) {
        if (!originalPath) path = Main.getPluginPath() + path;
        return String.format("%d;%s;%s;%b;%s", time, fileName, path, originalPath, node);
    }

    /**
     * 添加玩家指定计划任务。
     *
     * @param player 玩家
     * @param scheduledTask 计划任务字符串
     */
    public static void addPlayerScheduledTask(Player player, String scheduledTask) {
        UUID uuid = player.getUniqueId();

        // 使用 Caffeine 的 get 方法替代 ConcurrentHashMap 的 computeIfAbsent 方法，key 不存在时，使用 ValueLoader 加载值
        ConcurrentLinkedQueue<String> tasks = playerScheduledTasks.get(uuid, k -> new ConcurrentLinkedQueue<>());

        if (tasks != null && !tasks.contains(scheduledTask)) tasks.add(scheduledTask);
    }

    /**
     * 添加玩家指定计划任务。
     *
     * @param player 玩家
     * @param time 计划任务执行时间
     * @param fileName 计划任务文件名
     * @param path 计划任务执行路径
     * @param originalPath 计划任务路径是否为原始路径
     * @param node 计划任务执行节点
     */
    public static void addPlayerScheduledTask(Player player, long time, String fileName, String path, boolean originalPath, String node) {
        String scheduledTask = toScheduledTask(time, fileName, path, originalPath, node);
        addPlayerScheduledTask(player, scheduledTask);
    }

    /**
     * 解析 List JavaScript 语句，将 "_node_" 转换为 finalWishPrizeDoNode。
     *
     * @param player 玩家
     * @param fileName 许愿池文件名
     * @param finalWishPrizeDoNode 最终许愿奖品
     */
    public static void createPlayerScheduledTasks(Player player, String fileName, String finalWishPrizeDoNode) {
        List<String> scheduledTasks = WishManager.getWishWaitSetScheduledTasks(fileName);

        for (String scheduledTask : scheduledTasks) {
            ScriptUtils.eval(scheduledTask, player, "_node_", finalWishPrizeDoNode);
        }
    }

    /**
     * 解析 List JavaScript 语句。
     *
     * @param player 玩家
     * @param list 计划任务列表
     */
    public static void createPlayerScheduledTasks(Player player, List<String> list) {
        for (String scheduledTask : list) {
            QuickUtils.handleString(scheduledTask, player);
        }
    }

    /**
     * 删除指定计划任务。
     *
     * <p>在 {@link ScheduledTaskHandler#startTask()}
     * 等并发环境下直接在循环中修改集合的元素操作请不要使用此方法，而是使用 {@link Iterator#remove()}，否则会引发并发修改异常
     *
     * @param player 玩家
     * @param wishScheduledTaskString 待删除的计划任务的字符串表示，格式为 "时间;文件名;文件路径;true/false;node"
     */
    public static void removePlayerScheduledTask(Player player, String wishScheduledTaskString) {
        ConcurrentLinkedQueue<String> scheduledTasks = playerScheduledTasks.get(player.getUniqueId(), k -> null);
        if (scheduledTasks != null) scheduledTasks.remove(wishScheduledTaskString);
    }

    /**
     * 删除指定玩家的所有计划任务。
     *
     * @param player 玩家
     */
    public static void removePlayerScheduledTasks(Player player) {
        playerScheduledTasks.invalidate(player.getUniqueId());
    }

    /**
     * 获取指定玩家的所有计划任务。
     *
     * @param player 玩家
     * @return 指定玩家的所有计划任务的字符串表示列表
     */
    public static ConcurrentLinkedQueue<String> getPlayerScheduledTasks(Player player) {
        return playerScheduledTasks.get(player.getUniqueId(), k -> new ConcurrentLinkedQueue<>());
    }
}
