package twomillions.plugin.advancedwish.managers;

import com.github.benmanes.caffeine.cache.Cache;
import com.mongodb.client.model.Filters;
import de.leonhard.storage.Json;
import de.leonhard.storage.Yaml;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import twomillions.plugin.advancedwish.Main;
import twomillions.plugin.advancedwish.enums.wish.PlayerWishStatus;
import twomillions.plugin.advancedwish.managers.config.ConfigManager;
import twomillions.plugin.advancedwish.managers.databases.DatabasesManager;
import twomillions.plugin.advancedwish.managers.register.RegisterManager;
import twomillions.plugin.advancedwish.managers.tasks.ScheduledTaskManager;
import twomillions.plugin.advancedwish.tasks.PlayerCacheHandler;
import twomillions.plugin.advancedwish.utils.events.EventUtils;
import twomillions.plugin.advancedwish.utils.exceptions.ExceptionUtils;
import twomillions.plugin.advancedwish.utils.others.CaffeineUtils;
import twomillions.plugin.advancedwish.utils.others.ConstantsUtils;
import twomillions.plugin.advancedwish.utils.others.ItemUtils;
import twomillions.plugin.advancedwish.utils.random.RandomGenerator;
import twomillions.plugin.advancedwish.utils.texts.QuickUtils;
import twomillions.plugin.advancedwish.utils.texts.StringEncrypter;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 许愿管理器，提供许愿功能相关的方法
 *
 * @author 2000000
 * @date 2022/11/24 16:53
 */
@SuppressWarnings({"unused", "BooleanMethodIsAlwaysInverted"})
public class WishManager {
    private static final JavaPlugin plugin = Main.getInstance();

    /**
     * 玩家许愿记录。
     *
     * @see <a href="https://www.mcbbs.net/thread-1429293-1-1.html">[杂谈] Java 容器的线程安全性杂谈</a>
     */
    private static final ConcurrentLinkedQueue<UUID> wishPlayers = new ConcurrentLinkedQueue<>();

    /**
     * 检查是否含有指定的许愿池。
     *
     * @param wishName 许愿池名称
     * @return 若存在则返回 true，否则返回 false
     */
    public static boolean hasWish(String wishName) {
        return RegisterManager.getRegisterWish().contains(wishName);
    }

    /**
     * 检查玩家是否正在许愿。
     *
     * @param player 玩家
     * @return 若该玩家正在许愿，则返回 true，否则返回 false
     */
    public static boolean isPlayerInWishList(Player player) {
        return wishPlayers.contains(player.getUniqueId());
    }

    /**
     * 添加玩家到许愿列表。
     *
     * @param player 玩家
     */
    public static void addPlayerToWishList(Player player) {
        if (!isPlayerInWishList(player)) wishPlayers.add(player.getUniqueId());
    }

    /**
     * 从许愿列表删除玩家。
     *
     * @param player 玩家
     */
    public static void removePlayerWithWishList(Player player) {
        wishPlayers.remove(player.getUniqueId());
    }

    /**
     * 获取指定许愿池 WAIT-SET 计划任务。
     *
     * @param wishName 许愿池名称
     * @return 返回指定许愿池 WAIT-SET 计划任务的列表
     */
    public static List<String> getWishWaitSetScheduledTasks(String wishName) {
        Yaml yaml = ConfigManager.createYaml(wishName, ConstantsUtils.WISH, false, false);
        return yaml.getStringList("WAIT-SET");
    }

    /**
     * 获取指定许愿池的自定义许愿数量增加。
     *
     * <p>获取未进行处理，需要进行处理
     *
     * @param wishName 许愿池名称
     * @return 许愿所需数量的增量
     */
    public static String getWishNeedIncreasedAmount(String wishName) {
        Yaml yaml = ConfigManager.createYaml(wishName, ConstantsUtils.WISH, false, false);
        return yaml.getString("ADVANCED-SETTINGS.INCREASED-WISH-AMOUNT");
    }

    /**
     * 获取许愿池的保底信息。
     * 返回的格式为：保底率;Do-List;增加的保底率;是否清空保底率。
     *
     * <p>获取未进行处理，需要进行处理
     *
     * @param wishName 许愿池名称
     * @return 包含保底信息的列表
     */
    @SuppressWarnings("unused")
    public static List<String> getWishGuaranteedList(String wishName) {
        Yaml yaml = ConfigManager.createYaml(wishName, ConstantsUtils.WISH, false, false);
        return yaml.getStringList("GUARANTEED");
    }

    /**
     * 获取愿望数据同步状态。
     *
     * @param wishName 许愿池名称
     * @return 数据同步状态或许愿池名称
     */
    public static String getWishDataSync(String wishName) {
        Yaml yaml = ConfigManager.createYaml(wishName, ConstantsUtils.WISH, false, false);
        /*
         * 原本使用 getOrDefault 如果值不存在返回为 "" 而不是 null
         * 所以不会正常返回 手动判断是否为 ""
         */
        String dataSync = yaml.getString("ADVANCED-SETTINGS.DATA-SYNC");
        return dataSync.isEmpty() ? wishName : dataSync;
    }

    /**
     * 判断是否开启了许愿池玩家许愿数限制功能。
     *
     * @param wishName 许愿池名称
     * @return 如果开启了许愿数限制，则返回 true;否则返回 false。
     */
    public static boolean isEnabledWishLimit(String wishName) {
        Yaml yaml = ConfigManager.createYaml(wishName, ConstantsUtils.WISH, false, false);
        return !yaml.getString("ADVANCED-SETTINGS.WISH-LIMIT.LIMIT-AMOUNT").equals("0");
    }

    /**
     * 获取许愿池限制的许愿数。
     *
     * @param wishName 许愿池名称
     * @return 许愿池限制的许愿数，如果未设置限制，则返回 0。
     */
    public static String getWishLimitAmount(String wishName) {
        Yaml yaml = ConfigManager.createYaml(wishName, ConstantsUtils.WISH, false, false);
        return yaml.getString("ADVANCED-SETTINGS.WISH-LIMIT.LIMIT-AMOUNT");
    }

    /**
     * 获取许愿池重置限制的开始秒数。
     *
     * @param wishName 许愿池名称
     * @return 许愿池重置限制的开始秒数，如果未设置限制，则返回 0。
     */
    public static String getWishResetLimitStart(String wishName) {
        Yaml yaml = ConfigManager.createYaml(wishName, ConstantsUtils.WISH, false, false);
        return yaml.getString("ADVANCED-SETTINGS.WISH-LIMIT.RESET-LIMIT-START");
    }

    /**
     * 获取许愿池重置完成后循环秒数间隔。
     *
     * @param wishName 许愿池名称
     * @return 许愿池重置完成后循环秒数间隔，如果未设置，则返回 0。
     */
    public static String getWishResetLimitCycle(String wishName) {
        Yaml yaml = ConfigManager.createYaml(wishName, ConstantsUtils.WISH, false, false);
        return yaml.getString("ADVANCED-SETTINGS.WISH-LIMIT.RESET-LIMIT-CYCLE");
    }

    /**
     * 判断许愿卷是否增加限制数。
     *
     * <p>获取未进行处理，需要进行处理
     *
     * @param wishName 许愿池名称
     * @return 如果许愿卷增加限制数，则返回 true;否则返回 false。
     */
    public static String isEnabledCouponLimit(String wishName) {
        Yaml yaml = ConfigManager.createYaml(wishName, ConstantsUtils.WISH, false, false);
        return yaml.getString("ADVANCED-SETTINGS.WISH-LIMIT.COUPON-LIMIT");
    }

    /**
     * 获取增加的许愿限制次数。
     *
     * <p>获取未进行处理，需要进行处理
     *
     * @param wishName 许愿池名称
     * @return 增加的许愿限制次数，如果未设置，则返回 0。
     */
    public static String getWishIncreasedAmount(String wishName) {
        Yaml yaml = ConfigManager.createYaml(wishName, ConstantsUtils.WISH, false, false);
        return yaml.getString("ADVANCED-SETTINGS.WISH-LIMIT.INCREASED-AMOUNT");
    }

    /**
     * 判断是否启用了重置后发送效果。
     *
     * <p>获取未进行处理，不需要进行处理
     *
     * @param wishName 许愿名称
     * @return 启用状态
     */
    public static String isResetCompleteSendEnabled(String wishName) {
        Yaml yaml = ConfigManager.createYaml(wishName, ConstantsUtils.WISH, false, false);
        return yaml.getString("ADVANCED-SETTINGS.WISH-LIMIT.RESET-COMPLETE-SEND");
    }

    /**
     * 判断是否启用了重置后发送控制台消息。
     *
     * <p>获取未进行处理，不需要进行处理
     *
     * @param wishName 许愿名称
     * @return 启用状态
     */
    public static String isResetCompleteSendConsoleEnabled(String wishName) {
        Yaml yaml = ConfigManager.createYaml(wishName, ConstantsUtils.WISH, false, false);
        return yaml.getString("ADVANCED-SETTINGS.WISH-LIMIT.RESET-COMPLETE-SEND-CONSOLE");
    }

    /**
     * 获取此许愿池的许愿结果。
     *
     * @param wishName 许愿池的名称
     * @param player 玩家
     * @param actualProcessing 实际处理，是否设置玩家的抽奖次数等等，若为 false 则只是返回最终结果
     * @param returnNode 只返回执行节点
     * @return 若没有可随机的奖品，则返回值为 ""，若 actualProcessing 为 true 则只返回执行节点，否则返回全语句
     * @throws IllegalArgumentException 如果没有可随机的奖品
     */
    public static String getFinalWishPrize(Player player, String wishName, boolean actualProcessing, boolean returnNode) {
        Yaml yaml = ConfigManager.createYaml(wishName, ConstantsUtils.WISH, false, false);

        // 获取玩家此奖池的许愿数
        int wishAmount = getPlayerWishAmount(player, wishName);

        // 获取玩家此奖池的保底率进行检查
        double playerWishGuaranteed = getPlayerWishGuaranteed(player, wishName);

        for (String wishGuaranteedString : yaml.singleLayerKeySet("GUARANTEED")) {
            String key = "GUARANTEED." + wishGuaranteedString;

            List<String> effectList = yaml.getStringList(key + ".EFFECT");
            List<String> guaranteedList = yaml.getStringList(key + ".GUARANTEED");
            List<String> addGuaranteedList = yaml.getStringList(key + ".ADD-GUARANTEED");
            List<String> clearGuaranteedList = yaml.getStringList(key + ".CLEAR-GUARANTEED");

            double guaranteed = guaranteedList.stream()
                    .mapToDouble(s -> QuickUtils.handleDouble(s, player))
                    .sum();

            if (guaranteed != playerWishGuaranteed) continue;

            String effect = effectList.stream()
                    .map(s -> QuickUtils.handleString(s, player, "_guaranteed_", guaranteed))
                    .filter(s -> !s.isEmpty())
                    .findFirst()
                    .orElse("");

            double addGuaranteed = addGuaranteedList.stream()
                    .mapToDouble(s -> QuickUtils.handleDouble(s, player, "_guaranteed_", guaranteed, "_effect_", effect))
                    .sum();

            boolean clearGuaranteed = clearGuaranteedList.stream()
                    .map(s -> QuickUtils.handleBoolean(s, player, "_guaranteed_", guaranteed, "_effect_", effect, "_addGuaranteed_", addGuaranteed))
                    .findFirst()
                    .orElse(false);

            if (!effect.isEmpty()) {
                String guaranteedString = guaranteed + ";" + effect + ";" + addGuaranteed + ";" + clearGuaranteed;

                if (actualProcessing) {
                    setPlayerWishGuaranteed(player, wishName, guaranteedString);
                    setPlayerWishAmount(player, wishName, wishAmount + QuickUtils.handleInt(getWishNeedIncreasedAmount(wishName), player));
                }

                if (returnNode) return effect;
                else return guaranteedString;
            }
        }

        // 如果没有触发保底，则进行随机
        RandomGenerator<String> randomUtils = new RandomGenerator<>();
        for (String wishPrizeSetString : yaml.singleLayerKeySet("PRIZE-SET")) {
            String key = "PRIZE-SET." + wishPrizeSetString;

            List<String> effectList = yaml.getStringList(key + ".EFFECT");
            List<String> probabilityList = yaml.getStringList(key + ".PROBABILITY");
            List<String> addGuaranteedList = yaml.getStringList(key + ".ADD-GUARANTEED");
            List<String> clearGuaranteedList = yaml.getStringList(key + ".CLEAR-GUARANTEED");

            int probability = probabilityList.stream()
                    .mapToInt(s -> QuickUtils.handleInt(s, player))
                    .sum();

            String effect = effectList.stream()
                    .map(s -> QuickUtils.handleString(s, player, "_probability_", probability))
                    .filter(s -> !s.isEmpty())
                    .findFirst()
                    .orElse("");

            double addGuaranteed = addGuaranteedList.stream()
                    .mapToDouble(s -> QuickUtils.handleDouble(s, player, "_probability_", probability, "_effect_", effect))
                    .sum();

            boolean clearGuaranteed = clearGuaranteedList.stream()
                    .map(s -> QuickUtils.handleBoolean(s, player, "_probability_", probability, "_effect_", effect, "_addGuaranteed_", addGuaranteed))
                    .findFirst()
                    .orElse(false);

            if (!effect.isEmpty()) {
                String prizeSetString = probability + ";" + effect + ";" + addGuaranteed + ";" + clearGuaranteed;
                randomUtils.addRandomObject(prizeSetString, probability);
            }
        }

        // 随机出结果
        String randomElement;

        // 获取方式
        switch (yaml.getString("GET-RESULT-TYPE").toLowerCase()) {
            case "normal":
                randomElement = randomUtils.getResult();
                break;

            case "securerandom":
                randomElement = randomUtils.getResultWithSecureRandom();
                break;

            case "montecarlo":
                randomElement = randomUtils.getResultWithMonteCarlo();
                break;

            case "shuffle":
                randomElement = randomUtils.getResultWithShuffle();
                break;

            case "gaussian":
                randomElement = randomUtils.getResultWithGaussian();
                break;

            case "mersennetwister":
                randomElement = randomUtils.getResultWithMersenneTwister();
                break;

            case "xorshift":
                randomElement = randomUtils.getResultWithXORShift();
                break;

            default:
                randomElement = randomUtils.getResult();
                QuickUtils.sendConsoleMessage("&c您填入了未知的随机结果获取方式，请许愿池 &e" + wishName + " &c配置文件! 将自动以 Normal 方式进行获取!");
        }

        if (actualProcessing) {
            setPlayerWishGuaranteed(player, wishName, randomElement);
            setPlayerWishAmount(player, wishName, wishAmount + QuickUtils.handleInt(getWishNeedIncreasedAmount(wishName), player));
        }

        if (returnNode) return randomElement.split(";")[1];
        else return randomElement;
    }

    /**
     * 设置玩家的保底值，通过 finalProbabilityWish 返回 wishPrizeSetString / wishGuaranteedString 设置.
     *
     * @param player 玩家
     * @param wishName 许愿池名称
     * @param finalProbabilityWish 许愿结果
     */
    public static void setPlayerWishGuaranteed(Player player, String wishName, String finalProbabilityWish) {
        String[] finalProbabilityWishSplit = QuickUtils.handleStrings(finalProbabilityWish.split(";"), player);

        double addedValue = Double.parseDouble(finalProbabilityWishSplit[2]);
        boolean clearedGuaranteed = Boolean.parseBoolean(finalProbabilityWishSplit[3]);

        if (clearedGuaranteed) setPlayerWishGuaranteed(player, wishName, 0);
        setPlayerWishGuaranteed(player, wishName, getPlayerWishGuaranteed(player, wishName) + addedValue);
    }

    /**
     * 许愿功能。
     *
     * @param player 玩家
     * @param wishName 许愿的名称
     * @param force 是否强制许愿
     */
    public static void makeWish(Player player, String wishName, boolean force) {
        // 许愿状态
        PlayerWishStatus playerWishStatus = canPlayerWish(player, wishName);
        Yaml yaml = ConfigManager.createYaml(wishName, ConstantsUtils.WISH, false, false);

        // 当玩家许愿一次后没有等待最终奖品发放便尝试二次许愿时
        if (playerWishStatus == PlayerWishStatus.InProgress) {
            // isCancelled
            if (!EventUtils.callAsyncPlayerWishEvent(player, PlayerWishStatus.InProgress, wishName, force).isCancelled()) {
                ScheduledTaskManager.createPlayerScheduledTasks(player, yaml.getStringList("CANT-WISH-AGAIN"));
            }

            return;
        }

        // 当玩家正在处理缓存时尝试许愿
        if (playerWishStatus == PlayerWishStatus.LoadingCache) {
            // isCancelled
            if (!EventUtils.callAsyncPlayerWishEvent(player, PlayerWishStatus.LoadingCache, wishName, force).isCancelled()) {
                ScheduledTaskManager.createPlayerScheduledTasks(player, yaml.getStringList("CANT-WISH-LOADING-CACHE"));
            }

            return;
        }

        // 当玩家正在等待处理缓存时尝试许愿
        if (playerWishStatus == PlayerWishStatus.WaitingLoadingCache) {
            // isCancelled
            if (!EventUtils.callAsyncPlayerWishEvent(player, PlayerWishStatus.WaitingLoadingCache, wishName, force).isCancelled()) {
                ScheduledTaskManager.createPlayerScheduledTasks(player, yaml.getStringList("CANT-WISH-WAITING-LOADING-CACHE"));
            }

            return;
        }

        // 当玩家没有满足许愿条件但是尝试许愿时
        if (playerWishStatus == PlayerWishStatus.RequirementsNotMet && !force) {
            // isCancelled
            if (!EventUtils.callAsyncPlayerWishEvent(player, PlayerWishStatus.RequirementsNotMet, wishName, false).isCancelled()) {
                ScheduledTaskManager.createPlayerScheduledTasks(player, yaml.getStringList("CANT-WISH"));
            }

            return;
        }

        // 开启许愿次数限制并且玩家已经达到了许愿次数极限但是尝试许愿时
        if (playerWishStatus == PlayerWishStatus.ReachLimit && !force) {
            // isCancelled
            if (!EventUtils.callAsyncPlayerWishEvent(player, PlayerWishStatus.ReachLimit, wishName, false).isCancelled()) {
                ScheduledTaskManager.createPlayerScheduledTasks(player, yaml.getStringList("ADVANCED-SETTINGS.WISH-LIMIT.REACH-LIMIT"));
            }

            return;
        }

        // isCancelled
        if (EventUtils.callAsyncPlayerWishEvent(player, PlayerWishStatus.Allow, wishName, force).isCancelled()) return;

        // 设置与为玩家开启计划任务
        String finalWishPrize = getFinalWishPrize(player, wishName, true, true);

        addPlayerToWishList(player);
        ScheduledTaskManager.createPlayerScheduledTasks(player, wishName, finalWishPrize);
    }

    /**
     * 设置玩家指定许愿池的保底率。
     * 如果许愿池名为中文，将会出现乱码问题，因此建议使用 Unicode 编码传入许愿池名。
     *
     * @param player 玩家
     * @param wishName 许愿池名称
     * @param guaranteed 保底率
     */
    public static void setPlayerWishGuaranteed(Player player, String wishName, double guaranteed) {
        String uuid = player.getUniqueId().toString();
        String dataSync = StringEncrypter.encrypt(getWishDataSync(wishName) + "_guaranteed");

        DatabasesManager.getDatabasesManager().update(uuid, dataSync, String.valueOf(guaranteed), ConstantsUtils.PLAYER_GUARANTEED_COLLECTION_NAME);
    }

    /**
     * 设置玩家指定许愿池的保底率。
     * 如果许愿池名为中文，将会出现乱码问题，因此建议使用 Unicode 编码传入许愿池名。
     *
     * @param playerName 玩家名称
     * @param wishName 许愿池名称
     * @param guaranteed 保底率
     */
    public static void setPlayerWishGuaranteed(String playerName, String wishName, double guaranteed) {
        String dataSync = StringEncrypter.encrypt(getWishDataSync(wishName) + "_guaranteed");

        String offlinePlayerUUID = QuickUtils.getPlayerUUID(playerName);

        DatabasesManager.getDatabasesManager().update(offlinePlayerUUID, dataSync, String.valueOf(guaranteed), ConstantsUtils.PLAYER_GUARANTEED_COLLECTION_NAME);
    }

    /**
     * 获取玩家指定许愿池的保底率。
     *
     * @param player 玩家
     * @param wishName 许愿池名称
     * @return 返回玩家在指定许愿池的保底率
     */
    public static double getPlayerWishGuaranteed(Player player, String wishName) {
        String uuid = player.getUniqueId().toString();
        String dataSync = StringEncrypter.encrypt(getWishDataSync(wishName) + "_guaranteed");

        return Double.parseDouble(DatabasesManager.getDatabasesManager().getOrDefault(uuid, dataSync, "0", ConstantsUtils.PLAYER_GUARANTEED_COLLECTION_NAME).toString());
    }

    /**
     * 获取玩家指定许愿池的保底率。
     *
     * @param playerName 玩家名称
     * @param wishName 许愿池名称
     * @return 返回玩家在指定许愿池的保底率
     */
    public static double getPlayerWishGuaranteed(String playerName, String wishName) {
        String dataSync = StringEncrypter.encrypt(getWishDataSync(wishName) + "_guaranteed");

        String offlinePlayerUUID = QuickUtils.getPlayerUUID(playerName);

        return Double.parseDouble(DatabasesManager.getDatabasesManager().getOrDefault(offlinePlayerUUID, dataSync, "0", ConstantsUtils.PLAYER_GUARANTEED_COLLECTION_NAME).toString());
    }

    /**
     * 设置玩家在指定许愿池的许愿次数。
     *
     * @param player 玩家
     * @param wishName 许愿池名称
     * @param amount 许愿次数
     */
    public static void setPlayerWishAmount(Player player, String wishName, int amount) {
        String uuid = player.getUniqueId().toString();
        String dataSync = StringEncrypter.encrypt(getWishDataSync(wishName) + "_amount");

        DatabasesManager.getDatabasesManager().update(uuid, dataSync, String.valueOf(amount), ConstantsUtils.PLAYER_GUARANTEED_COLLECTION_NAME);
    }

    /**
     * 设置玩家在指定许愿池的许愿次数。
     *
     * @param playerName 玩家名称
     * @param wishName 许愿池名称
     * @param amount 许愿次数
     */
    public static void setPlayerWishAmount(String playerName, String wishName, int amount) {
        String dataSync = StringEncrypter.encrypt(getWishDataSync(wishName) + "_amount");

        String offlinePlayerUUID = QuickUtils.getPlayerUUID(playerName);

        DatabasesManager.getDatabasesManager().update(offlinePlayerUUID, dataSync, String.valueOf(amount), ConstantsUtils.PLAYER_GUARANTEED_COLLECTION_NAME);
    }

    /**
     * 获取玩家在指定许愿池中的许愿次数。
     *
     * @param player 玩家
     * @param wishName 许愿池名称
     * @return 玩家在许愿池中的许愿次数
     */
    public static int getPlayerWishAmount(Player player, String wishName) {
        String uuid = player.getUniqueId().toString();
        String dataSync = StringEncrypter.encrypt(getWishDataSync(wishName) + "_amount");

        return Integer.parseInt(DatabasesManager.getDatabasesManager().getOrDefault(uuid, dataSync, "0", ConstantsUtils.PLAYER_GUARANTEED_COLLECTION_NAME).toString());
    }

    /**
     * 获取玩家在指定许愿池中的许愿次数。
     *
     * @param playerName 玩家名称
     * @param wishName 许愿池名称
     * @return 玩家在许愿池中的许愿次数
     */
    public static int getPlayerWishAmount(String playerName, String wishName) {
        String dataSync = StringEncrypter.encrypt(getWishDataSync(wishName) + "_amount");

        String offlinePlayerUUID = QuickUtils.getPlayerUUID(playerName);

        return Integer.parseInt(DatabasesManager.getDatabasesManager().getOrDefault(offlinePlayerUUID, dataSync, "0", ConstantsUtils.PLAYER_GUARANTEED_COLLECTION_NAME).toString());
    }

    /**
     * 设置玩家在指定许愿池中的许愿次数上限。
     *
     * @param player 玩家
     * @param wishName 许愿池名称
     * @param amount 许愿次数上限
     */
    public static void setPlayerWishLimitAmount(Player player, String wishName, int amount) {
        if (!isEnabledWishLimit(wishName)) return;

        String uuid = player.getUniqueId().toString();
        String dataSync = StringEncrypter.encrypt(getWishDataSync(wishName) + "_limit_amount");

        DatabasesManager.getDatabasesManager().update(uuid, dataSync, String.valueOf(amount), ConstantsUtils.PLAYER_GUARANTEED_COLLECTION_NAME);
    }

    /**
     * 设置玩家在指定许愿池中的许愿次数上限。
     *
     * @param playerName 玩家名称
     * @param wishName 许愿池名称
     * @param amount 许愿次数上限
     */
    public static void setPlayerWishLimitAmount(String playerName, String wishName, int amount) {
        if (!isEnabledWishLimit(wishName)) return;

        String dataSync = StringEncrypter.encrypt(getWishDataSync(wishName) + "_limit_amount");

        String offlinePlayerUUID = QuickUtils.getPlayerUUID(playerName);

        DatabasesManager.getDatabasesManager().update(offlinePlayerUUID, dataSync, String.valueOf(amount), ConstantsUtils.PLAYER_GUARANTEED_COLLECTION_NAME);
    }

    /**
     * 获取玩家在指定许愿池中的许愿次数上限。
     *
     * @param player 玩家
     * @param wishName 许愿池名称
     * @return 玩家在许愿池中的许愿次数上限
     */
    public static int getPlayerWishLimitAmount(Player player, String wishName) {
        if (!isEnabledWishLimit(wishName)) return 0;

        String uuid = player.getUniqueId().toString();
        String dataSync = StringEncrypter.encrypt(getWishDataSync(wishName) + "_limit_amount");

        return Integer.parseInt(DatabasesManager.getDatabasesManager().getOrDefault(uuid, dataSync, "0", ConstantsUtils.PLAYER_GUARANTEED_COLLECTION_NAME).toString());
    }

    /**
     * 获取玩家在指定许愿池中的许愿次数上限。
     *
     * @param playerName 玩家名称
     * @param wishName 许愿池名称
     * @return 玩家在许愿池中的许愿次数上限
     */
    public static int getPlayerWishLimitAmount(String playerName, String wishName) {
        if (!isEnabledWishLimit(wishName)) return 0;

        String dataSync = StringEncrypter.encrypt(getWishDataSync(wishName) + "_limit_amount");

        String offlinePlayerUUID = QuickUtils.getPlayerUUID(playerName);

        return Integer.parseInt(DatabasesManager.getDatabasesManager().getOrDefault(offlinePlayerUUID, dataSync, "0", ConstantsUtils.PLAYER_GUARANTEED_COLLECTION_NAME).toString());
    }

    /**
     * 重置指定许愿池所有玩家的许愿次数上限。
     *
     * @param wishName 许愿池名称
     */
    public static void resetWishLimitAmount(String wishName) {
        String dataSync = StringEncrypter.encrypt(getWishDataSync(wishName) + "_limit_amount");

        switch (DatabasesManager.getDataStorageType()) {
            case MongoDB:
                DatabasesManager.getMongoManager().getMongoDatabase().getCollection("PlayerGuaranteed").deleteMany(Filters.gte(dataSync, "0"));
                break;

            case MySQL:
                DatabasesManager.getMySQLManager().executeStatement("DELETE FROM PlayerGuaranteed WHERE dataSync >= '0'");
                break;

            case Json:
                String path = Main.getGuaranteedPath();
                for (String fileName : ConfigManager.getAllFileNames(path)) {
                    Json json = ConfigManager.createJson(fileName, path, true, false);
                    json.remove(dataSync);
                }
                break;

            default:
                ExceptionUtils.throwUnknownDataStoreType();
                break;
        }
    }

    /**
     * 检查玩家是否满足许愿条件。
     *
     * @param player player
     * @param wishName wishName
     * @return PlayerWishStatus
     */
    public static PlayerWishStatus canPlayerWish(Player player, String wishName) {
        UUID uuid = player.getUniqueId();

        // 检查玩家是否正在许愿
        if (isPlayerInWishList(player)) {
            return PlayerWishStatus.InProgress;
        }

        // 检查玩家是否正在处理缓存
        if (PlayerCacheHandler.isLoadingCache(uuid)) {
            return PlayerWishStatus.LoadingCache;
        }

        // 检查玩家是否正在等待处理缓存
        if (PlayerCacheHandler.isWaitingLoadingCache(uuid)) {
            return PlayerWishStatus.WaitingLoadingCache;
        }

        Yaml yaml = ConfigManager.createYaml(wishName, ConstantsUtils.WISH, false, false);
        yaml.setPathPrefix("CONDITION");

        String permission = QuickUtils.handleString(yaml.getString("PERM"), player);

        int level = QuickUtils.handleInt(yaml.getString("LEVEL"), player);
        int point = QuickUtils.handleInt(yaml.getString("POINT"), player);
        double money = QuickUtils.handleDouble(yaml.getString("MONEY"));

        boolean isEnabledCouponLimit = QuickUtils.handleBoolean(WishManager.isEnabledCouponLimit(wishName));

        // 许愿券检查
        yaml.setPathPrefix("ADVANCED-SETTINGS");

        for (String coupon : yaml.getStringList("COUPON")) {
            if (coupon.isEmpty()) {
                break;
            }

            String[] couponSplit = QuickUtils.stripColor(QuickUtils.handleStrings(coupon.split(";"), player));

            int removeAmount = Integer.parseInt(couponSplit[0]);
            String itemLoreContains = couponSplit[1];

            ConcurrentLinkedQueue<ItemStack> toRemove = Arrays.stream(player.getInventory().getContents())
                    .filter(itemStack -> itemStack != null && itemStack.getType() != Material.AIR)
                    .filter(itemStack -> {
                        ItemMeta itemMeta = itemStack.getItemMeta();
                        if (itemMeta == null) return false;
                        List<String> lore = itemMeta.getLore();
                        return itemLoreContains.isEmpty() || (lore != null && lore.stream().anyMatch(line -> QuickUtils.stripColor(line).contains(itemLoreContains)));
                    })
                    .collect(Collectors.toCollection(ConcurrentLinkedQueue::new));

            // 数量检查
            if (toRemove.isEmpty() || toRemove.stream().mapToInt(ItemStack::getAmount).sum() < removeAmount) {
                break;
            }

            // 限制检查
            if (isEnabledWishLimit(wishName) && isEnabledCouponLimit && !handleWishIncreasedAmount(wishName, player)) {
                return PlayerWishStatus.ReachLimit;
            }

            // 物品移除
            if (removeAmount > 0) {
                if (toRemove.stream()
                        .anyMatch(itemStack -> ItemUtils.removeItems(player, itemStack, removeAmount))) {
                    return PlayerWishStatus.Allow;
                }
            }
        }

        yaml.setPathPrefix("CONDITION");

        // 权限检查
        if (!permission.isEmpty() && !player.hasPermission(permission)) {
            return PlayerWishStatus.RequirementsNotMet;
        }

        // 等级检查
        if (player.getLevel() < level) {
            return PlayerWishStatus.RequirementsNotMet;
        }

        // 如果开启了许愿次数限制
        if (isEnabledWishLimit(wishName) && !handleWishIncreasedAmount(wishName, player)) {
            return PlayerWishStatus.ReachLimit;
        }

        // 背包物品检查
        Cache<ItemStack, Integer> inventoryHaveRemovedItems = CaffeineUtils.buildBukkitCache();

        for (String configInventoryHave : yaml.getStringList("INVENTORY-HAVE")) {
            if (configInventoryHave.isEmpty()) {
                continue;
            }

            String[] configInventoryHaveCustomSplit = QuickUtils.stripColor(QuickUtils.handleStrings(configInventoryHave.split(";"), player));

            int checkAmount = Integer.parseInt(configInventoryHaveCustomSplit[1]);
            int removeAmount = Integer.parseInt(configInventoryHaveCustomSplit[2]);
            Material material = ItemUtils.materialValueOf(configInventoryHaveCustomSplit[0], wishName);

            // 物品数据
            ConcurrentLinkedQueue<ItemStack> toRemove = Arrays.stream(player.getInventory().getContents())
                    .filter(Objects::nonNull)
                    .filter(itemStack -> itemStack.getType() == material)
                    .collect(Collectors.toCollection(ConcurrentLinkedQueue::new));

            if (toRemove.isEmpty() || toRemove.stream().mapToInt(ItemStack::getAmount).sum() < checkAmount) {
                return PlayerWishStatus.RequirementsNotMet;
            }

            // 物品移除
            if (removeAmount > 0) {
                int removedAmount = toRemove.stream()
                        .limit(removeAmount)
                        .mapToInt(ItemStack::getAmount)
                        .sum();

                if (removedAmount < removeAmount) {
                    return PlayerWishStatus.RequirementsNotMet;
                }

                toRemove.forEach(item -> inventoryHaveRemovedItems.put(item, removeAmount));
            }
        }

        // 背包物品检查 - 自定义物品
        Cache<ItemStack, Integer> inventoryHaveCustomRemovedItems = CaffeineUtils.buildBukkitCache();

        for (String configInventoryHaveCustom : yaml.getStringList("INVENTORY-HAVE-CUSTOM")) {
            if (configInventoryHaveCustom.isEmpty()) {
                continue;
            }

            String[] configInventoryHaveCustomSplit = QuickUtils.stripColor(QuickUtils.handleStrings(configInventoryHaveCustom.split(";"), player));

            String itemName = configInventoryHaveCustomSplit[0];
            String itemLoreContains = configInventoryHaveCustomSplit.length > 1 ? configInventoryHaveCustomSplit[1] : "";
            int checkAmount = Integer.parseInt(configInventoryHaveCustomSplit[2]);
            int removeAmount = Integer.parseInt(configInventoryHaveCustomSplit[3]);

            // 物品数据
            ConcurrentLinkedQueue<ItemStack> toRemove = Arrays.stream(player.getInventory().getContents())
                    .filter(Objects::nonNull)
                    .filter(itemStack -> {
                        ItemMeta itemMeta = itemStack.getItemMeta();

                        return itemMeta != null && itemMeta.hasDisplayName() && QuickUtils.stripColor(itemMeta.getDisplayName()).equals(itemName)
                                && (itemLoreContains.isEmpty() || Optional.ofNullable(itemMeta.getLore())
                                .map(lore -> lore.stream().anyMatch(line -> QuickUtils.stripColor(line).contains(itemLoreContains)))
                                .orElse(false));
                    })
                    .collect(Collectors.toCollection(ConcurrentLinkedQueue::new));

            if (toRemove.isEmpty() || toRemove.stream().mapToInt(ItemStack::getAmount).sum() < checkAmount) {
                return PlayerWishStatus.RequirementsNotMet;
            }

            // 物品移除
            if (removeAmount > 0) {
                int removedAmount = toRemove.stream()
                        .limit(removeAmount)
                        .mapToInt(ItemStack::getAmount)
                        .sum();

                if (removedAmount < removeAmount) {
                    return PlayerWishStatus.RequirementsNotMet;
                }

                toRemove.stream()
                        .limit(removeAmount)
                        .forEach(item -> inventoryHaveCustomRemovedItems.put(item, removeAmount));
            }
        }

        // 检查玩家是否拥有指定的药水效果
        if (yaml.getStringList("PLAYER-HAVE-EFFECTS").stream()
                .filter(effect -> !effect.isEmpty())
                .map(effect -> QuickUtils.handleStrings(effect.split(";"), player))
                .map(effectInfo -> {
                    String effectType = effectInfo[0].toUpperCase(Locale.ROOT);
                    PotionEffectType potionEffectType = PotionEffectType.getByName(effectType);

                    int effectAmplifier = Integer.parseInt(effectInfo[1]);

                    if (potionEffectType == null) {
                        ExceptionUtils.sendUnknownWarn("药水效果", wishName, effectType);
                        return false;
                    }

                    return player.hasPotionEffect(potionEffectType) && player.getPotionEffect(potionEffectType).getAmplifier() >= effectAmplifier;
                })
                .anyMatch(hasEffect -> !hasEffect)) {
            return PlayerWishStatus.RequirementsNotMet;
        }

        // 检查自定义条件
        List<String> configCustomConditions = yaml.getStringList("CUSTOM");
        if (!configCustomConditions.isEmpty() && configCustomConditions.stream()
                .anyMatch(condition -> !condition.isEmpty() && !QuickUtils.handleBoolean(condition, player))) {
            return PlayerWishStatus.RequirementsNotMet;
        }

        // 扣除
        Economy economy = RegisterManager.getEconomy();
        PlayerPointsAPI playerPointsAPI = RegisterManager.getPlayerPointsAPI();

        if (economy != null && money > 0 && !economy.has(player, money)) {
            return PlayerWishStatus.RequirementsNotMet;
        }

        if (playerPointsAPI != null && point > 0 && playerPointsAPI.look(player.getUniqueId()) < point) {
            return PlayerWishStatus.RequirementsNotMet;
        }

        if (economy != null && money > 0) {
            economy.withdrawPlayer(player, money);
        }

        if (playerPointsAPI != null && point > 0) {
            playerPointsAPI.take(player.getUniqueId(), point);
        }

        if (!Stream.concat(inventoryHaveRemovedItems.asMap().entrySet().stream(), inventoryHaveCustomRemovedItems.asMap().entrySet().stream())
                .allMatch(entry -> ItemUtils.removeItems(player, entry.getKey(), entry.getValue()))) {
            return PlayerWishStatus.RequirementsNotMet;
        }

        return PlayerWishStatus.Allow;
    }

    /**
     * 处理许愿限制。
     *
     * <p>是否开启判断应在使用此方法前进行
     *
     * @param wishName 许愿池名称
     * @param player 玩家
     * @return 若处理成功则返回 true，若达到极限则为 false
     */
    private static boolean handleWishIncreasedAmount(String wishName, Player player) {
        int wishLimitAmount = QuickUtils.handleInt(getWishLimitAmount(wishName), player);
        int wishIncreasedAmount = QuickUtils.handleInt(getWishIncreasedAmount(wishName), player);
        int playerWishLimitAmount = getPlayerWishLimitAmount(player, wishName) + wishIncreasedAmount;

        // 如果增加许愿次数但增加后的许愿次数到达极限，那么返回并不增加限制次数
        if (playerWishLimitAmount > wishLimitAmount) return false;

        // 增加限制次数
        setPlayerWishLimitAmount(player, wishName, playerWishLimitAmount);

        return true;
    }

    /**
     * 用于保存玩家的许愿缓存数据。
     *
     * <p>不再使用 ConcurrentHashMap，Caffeine 提供了一个高性能的线程安全哈希表实现，它比 ConcurrentHashMap 更快
     * 并且使用的内存更少，Caffeine 通过使用非常快的 Hash 函数，以及高效的数据结构和算法，来实现快速地并发访问，是一个非常强大的缓存库
     *
     * @see <a href="https://www.mcbbs.net/thread-1429293-1-1.html">[杂谈] Java 容器的线程安全性杂谈</a>
     */
    @Getter private static final Cache<UUID, Boolean> savingCache = CaffeineUtils.buildBukkitCache();

    /**
     * 保存玩家缓存数据。
     *
     * @param player 玩家
     */
    public static void savePlayerCacheData(Player player) {
        UUID uuid = player.getUniqueId();

        savingCache.put(uuid, true);

        PlayerCacheHandler.setPlayerQuitTime(player);

        ConcurrentLinkedQueue<String> playerDoList = ScheduledTaskManager.getPlayerScheduledTasks(player);

        if (playerDoList.isEmpty()) {
            savingCache.put(uuid, false);
            return;
        }

        List<String> newPlayerDoList = playerDoList.stream()
                .map(StringEncrypter::encrypt)
                .collect(Collectors.toList());

        ConfigManager.createJson(uuid.toString(), Main.getDoListCachePath(), true, false).set("CACHE", newPlayerDoList);

        ScheduledTaskManager.removePlayerScheduledTasks(player);

        savingCache.put(uuid, false);
    }

    /**
     * 设置玩家的 OP 指令缓存数据。
     *
     * @param player 玩家
     * @param doOpCommand 是否允许使用 OP 指令
     */
    public static void setPlayerCacheOpData(Player player, boolean doOpCommand) {
        ConfigManager.createJson(player.getUniqueId().toString(), ConstantsUtils.PLAYER_CACHE, false, false)
                .set("DO-OP-COMMAND", doOpCommand);
    }
}
