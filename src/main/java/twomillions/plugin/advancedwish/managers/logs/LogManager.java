package twomillions.plugin.advancedwish.managers.logs;

import twomillions.plugin.advancedwish.annotations.JsInteropJavaType;
import twomillions.plugin.advancedwish.managers.databases.DatabasesManager;
import twomillions.plugin.advancedwish.utils.others.ConstantsUtils;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 玩家日志处理类。
 *
 * @author 2000000
 * @date 2023/3/26
 */
@JsInteropJavaType
public class LogManager {
    /**
     * 添加玩家许愿日志。
     *
     * @param uuid 玩家 UUID 字符串
     * @param logString 许愿日志
     */
    public static void addPlayerWishLog(String uuid, String logString) {
        ConcurrentLinkedQueue<String> dbLogs = DatabasesManager.getDatabasesManager().getOrDefaultList(uuid, "logs", new ConcurrentLinkedQueue<>(), ConstantsUtils.PLAYER_LOGS_COLLECTION_NAME);
        dbLogs.add(logString);
        DatabasesManager.getDatabasesManager().update(uuid, "logs", dbLogs, ConstantsUtils.PLAYER_LOGS_COLLECTION_NAME);
    }

    /**
     * 获取指定玩家的许愿日志。
     *
     * @param uuid 玩家的 UUID
     * @param findMin 要查询的日志的最小编号
     * @param findMax 要查询的日志的最大编号
     * @return 返回查询出来的日志列表
     */
    public static ConcurrentLinkedQueue<String> getPlayerWishLog(String uuid, int findMin, int findMax) {
        return getLogsInRange(DatabasesManager.getDatabasesManager().getOrDefaultList(uuid, "logs", new ConcurrentLinkedQueue<>(), ConstantsUtils.PLAYER_LOGS_COLLECTION_NAME), findMin, findMax);
    }

    /**
     * 获取指定玩家的所有日志条目数。
     *
     * @param uuid 玩家的 UUID
     * @return 返回日志条目数
     */
    public static int getPlayerWishLogSize(String uuid) {
        return DatabasesManager.getDatabasesManager().getOrDefaultList(uuid, "logs", new ConcurrentLinkedQueue<>(), ConstantsUtils.PLAYER_LOGS_COLLECTION_NAME).size();
    }

    /**
     * 获取给定列表的指定范围内的子列表。
     *
     * @param logs 给定的日志列表
     * @param min 子列表的最小索引（从1开始）
     * @param max 子列表的最大索引
     * @return 给定列表的指定范围内的子列表
     */
    public static ConcurrentLinkedQueue<String> getLogsInRange(ConcurrentLinkedQueue<String> logs, int min, int max) {
        ConcurrentLinkedQueue<String> result = new ConcurrentLinkedQueue<>();

        // 使用 Iterator 来遍历队列实现线程安全
        Iterator<String> iterator = logs.iterator();

        int i = 1;
        while (iterator.hasNext() && i <= max) {
            if (i >= min) {
                result.add(iterator.next());
            }

            i++;
        }

        return result;
    }
}
