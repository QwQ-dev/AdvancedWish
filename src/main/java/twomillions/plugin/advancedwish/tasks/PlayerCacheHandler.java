package twomillions.plugin.advancedwish.tasks;

import com.github.benmanes.caffeine.cache.Cache;
import de.leonhard.storage.Json;
import de.leonhard.storage.Yaml;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import twomillions.plugin.advancedwish.Main;
import twomillions.plugin.advancedwish.abstracts.TasksAbstract;
import twomillions.plugin.advancedwish.managers.config.ConfigManager;
import twomillions.plugin.advancedwish.managers.tasks.ScheduledTaskManager;
import twomillions.plugin.advancedwish.utils.events.EventUtils;
import twomillions.plugin.advancedwish.utils.others.CaffeineUtils;
import twomillions.plugin.advancedwish.utils.others.ConstantsUtils;
import twomillions.plugin.advancedwish.utils.texts.QuickUtils;
import twomillions.plugin.advancedwish.utils.texts.StringEncrypter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 该类继承 {@link TasksAbstract}，用于处理玩家缓存任务。
 *
 * @author 2000000
 * @date 2022/11/24 20:09
 */
@Getter @Setter
@SuppressWarnings("unused")
public class PlayerCacheHandler extends TasksAbstract {
    private final Runnable runnable;

    private static final JavaPlugin plugin = Main.getInstance();

    private static final Cache<UUID, Boolean> loadingCache = CaffeineUtils.buildBukkitCache();
    private static final Cache<UUID, Boolean> waitingLoadingCache = CaffeineUtils.buildBukkitCache();

    /**
     * 构造器。
     *
     * @param player 玩家
     */
    public PlayerCacheHandler(Player player) {
        runnable = () -> {
            UUID uuid = player.getUniqueId();

            String normalPath = Main.getPluginPath() + ConstantsUtils.PLAYER_CACHE;
            String doListCachePath = Main.getDoListCachePath();

            // 遍历缓存文件，判断是否有正常缓存或操作缓存
            boolean hasNormalCache = ConfigManager.getAllFileNames(normalPath).contains(uuid + ConstantsUtils.JSON_FILE_EXTENSION);
            boolean hasDoListCachePath = ConfigManager.getAllFileNames(doListCachePath).contains(uuid + ConstantsUtils.JSON_FILE_EXTENSION);

            // 如果没有相应的缓存，将其赋值为 null
            if (!hasNormalCache) {
                normalPath = null;
            }

            if (!hasDoListCachePath) {
                doListCachePath = null;
            }

            // 触发异步检查缓存事件，并判断是否取消事件
            if (EventUtils.callAsyncPlayerCheckCacheEvent(player, normalPath, doListCachePath).isCancelled()) {
                return;
            }

            // 如果没有缓存文件，直接返回
            if (!hasNormalCache && !hasDoListCachePath) {
                return;
            }

            // 检查缓存
            checkCache(player, normalPath, doListCachePath);
        };
    }

    /**
     * 异步检查玩家缓存数据并触发对应事件。
     *
     * <p>自 0.0.3.4-SNAPSHOT 后，此方法将记录每次玩家的状态，以防止安全问题。
     */
    @Override
    public void startTask() {
        setBukkitTask(Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable));
    }

    /**
     * 检查玩家缓存数据，并执行相关操作。
     *
     * @param player 玩家
     * @param normalPath 正常缓存数据的文件路径
     * @param doListCachePath 任务缓存数据的文件路径
     */
    private void checkCache(Player player, String normalPath, String doListCachePath) {
        UUID uuid = player.getUniqueId();

        setLoadingCache(uuid, true);

        // 处理正常缓存数据
        if (normalPath != null) {
            handleOpCache(player, uuid, normalPath);
        }

        // 处理任务缓存数据
        if (doListCachePath != null) {
            handleDoListCache(player, uuid, doListCachePath);
        }

        setLoadingCache(uuid, false);
    }

    /**
     * 处理玩家 Op 缓存。
     *
     * @param player 玩家
     * @param uuid UUID
     * @param normalPath 正常缓存数据的文件路径
     */
    private void handleOpCache(Player player, UUID uuid, String normalPath) {
        Json normalJson = ConfigManager.createJson(uuid.toString(), normalPath, true, false);

        // 处理安全问题 - Op 执行指令
        if (normalJson.getBoolean("DO-OP-COMMAND")) {
            if (player.isOnline()) {
                player.setOp(false);
                normalJson.set("DO-OP-COMMAND", null);
            }
        }
    }

    /**
     * 处理玩家任务缓存。
     *
     * @param player 玩家
     * @param uuid UUID
     * @param doListCachePath 任务缓存数据的文件路径
     */
    @SneakyThrows
    private void handleDoListCache(Player player, UUID uuid, String doListCachePath) {
        Json doListCacheJson = ConfigManager.createJson(uuid.toString(), doListCachePath, true, false);

        // 获取玩家任务缓存列表并克隆
        List<String> playerDoListCache = doListCacheJson.getStringList("CACHE");
        ConcurrentLinkedQueue<String> playerDoListCacheClone = new ConcurrentLinkedQueue<>(playerDoListCache);

        // 如果没有缓存项则退出
        if (playerDoListCache.size() == 0) {
            setLoadingCache(uuid, false);
            return;
        }

        // 是否发送任务执行效果
        boolean firstSentEffect = true;

        // 遍历缓存执行项
        for (String playerWishDoListString : playerDoListCache) {
            // 解析缓存执行项
            playerWishDoListString = StringEncrypter.decrypt(playerWishDoListString);
            String[] playerWishDoListStringSplit = playerWishDoListString.split(";");

            String doList = playerWishDoListStringSplit[4];
            String wishName = playerWishDoListStringSplit[1];

            // 获取任务配置文件
            Yaml yaml = ConfigManager.createYaml(wishName, ConstantsUtils.WISH, false, false);

            // 发送任务执行效果
            if (firstSentEffect) {
                // 创建玩家任务
                ScheduledTaskManager.createPlayerScheduledTasks(player, yaml.getStringList("CACHE-SETTINGS.WISH-CACHE"));

                // 等待一定时间再执行下一个任务
                Thread.sleep(QuickUtils.handleLong(yaml.getString("CACHE-SETTINGS.WAIT-RECOVERY"), player) * 1000L);

                // 不二次发送
                firstSentEffect = false;
            }

            // 如果玩家在线则重新添加任务
            if (player.isOnline()) {
                long nowTime = System.currentTimeMillis();
                long quitTime = getPlayerQuitTime(player);
                long oldTime = Long.parseLong(playerWishDoListStringSplit[0]);

                ScheduledTaskManager.addPlayerScheduledTask(player, oldTime - quitTime + nowTime, wishName, ConstantsUtils.WISH, false, doList);
                playerDoListCacheClone.remove(StringEncrypter.encrypt(playerWishDoListString));
            }

            // 更新任务缓存数据
            doListCacheJson.set("CACHE", playerDoListCacheClone.size() == 0 ? null : playerDoListCacheClone);

            if (!player.isOnline()) {
                break;
            }
        }
    }

    /**
     * 设置指定玩家的缓存加载状态。
     *
     * @param uuid 玩家 UUID
     * @param isLoadingCache 缓存加载状态
     */
    public static void setLoadingCache(UUID uuid, boolean isLoadingCache) {
        loadingCache.put(uuid, isLoadingCache);
    }

    /**
     * 检查指定玩家的缓存加载状态。
     *
     * @param uuid 玩家UUID
     * @return 缓存加载状态，如果玩家未在缓存中，则返回false
     */
    public static boolean isLoadingCache(UUID uuid) {
        return Boolean.TRUE.equals(loadingCache.get(uuid, k -> false));
    }

    /**
     * 设置指定玩家的等待缓存加载状态。
     *
     * @param uuid 玩家 UUID
     * @param isWaitingLoadingCache 等待缓存加载状态
     */
    public static void setWaitingLoadingCache(UUID uuid, boolean isWaitingLoadingCache) {
        waitingLoadingCache.put(uuid, isWaitingLoadingCache);
    }

    /**
     * 检查指定玩家的等待缓存加载状态。
     *
     * @param uuid 玩家UUID
     * @return 等待缓存加载状态，如果玩家未在缓存中，则返回false
     */
    public static boolean isWaitingLoadingCache(UUID uuid) {
        return Boolean.TRUE.equals(waitingLoadingCache.get(uuid, k -> false));
    }

    /**
     * 设置指定玩家的退出时间戳。
     *
     * @param player 玩家
     * @param time 退出时间戳
     */
    public static void setPlayerQuitTime(Player player, long time) {
        ConfigManager.createJson(player.getUniqueId().toString(), Main.getDoListCachePath(), true, false).set("QUIT-CACHE", time);
    }

    /**
     * 设置指定玩家的退出时间戳为当前系统时间戳。
     *
     * @param player 玩家
     */
    public static void setPlayerQuitTime(Player player) {
        setPlayerQuitTime(player, System.currentTimeMillis());
    }

    /**
     * 获取指定玩家的退出时间戳。
     *
     * @param player 玩家
     * @return 退出时间戳
     */
    public static long getPlayerQuitTime(Player player) {
        return ConfigManager.createJson(player.getUniqueId().toString(), Main.getDoListCachePath(), true, false).getLong("QUIT-CACHE");
    }
}
