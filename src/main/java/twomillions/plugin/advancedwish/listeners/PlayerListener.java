package twomillions.plugin.advancedwish.listeners;

import com.github.benmanes.caffeine.cache.Cache;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import twomillions.plugin.advancedwish.Main;
import twomillions.plugin.advancedwish.managers.WishManager;
import twomillions.plugin.advancedwish.managers.config.ConfigManager;
import twomillions.plugin.advancedwish.managers.effects.EffectSendManager;
import twomillions.plugin.advancedwish.tasks.PlayerCacheHandler;
import twomillions.plugin.advancedwish.tasks.UpdateHandler;
import twomillions.plugin.advancedwish.utils.texts.QuickUtils;

import java.util.UUID;

/**
 * 该类实现 {@link Listener}，处理玩家监听。
 *
 * @author 2000000
 * @date 2022/11/24 16:58
 */
public class PlayerListener implements Listener {
    private static final JavaPlugin plugin = Main.getInstance();
    private static final Cache<Player, String> opSentCommand = EffectSendManager.getOpSentCommand();

    /**
     * 玩家登录事件处理方法，用于取消特殊情况下的玩家登录。
     *
     * @param event 玩家登录事件
     */
    @EventHandler
    public void onPlayerLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();

        if (Boolean.TRUE.equals(WishManager.getSavingCache().get(uuid, k -> false))) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, QuickUtils.handleString(ConfigManager.getAdvancedWishYaml().getString("CANCEL-LOGIN-REASONS.SAVING-CACHE")));
            return;
        }

        if (PlayerCacheHandler.isLoadingCache(uuid)) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, QuickUtils.handleString(ConfigManager.getAdvancedWishYaml().getString("CANCEL-LOGIN-REASONS.LOADING-CACHE")));
            return;
        }

        if (PlayerCacheHandler.isWaitingLoadingCache(uuid)) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, QuickUtils.handleString(ConfigManager.getAdvancedWishYaml().getString("CANCEL-LOGIN-REASONS.WAITING-LOADING-CACHE")));
        }
    }

    /**
     * 玩家加入事件处理方法，用于处理玩家进入时的缓存并开始玩家时间戳检查。
     *
     * @param event 玩家加入事件
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // 延时等待玩家缓存写入
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerCacheHandler.setWaitingLoadingCache(uuid, true);

            try {
                Thread.sleep(ConfigManager.getAdvancedWishYaml().getLong("WAIT-LOADING") * 1000L);
            } catch (Exception ignore) { }

            // 玩家已经离线，取消等待
            if (!player.isOnline()) {
                PlayerCacheHandler.setWaitingLoadingCache(uuid, false);
                return;
            }

            PlayerCacheHandler.setWaitingLoadingCache(uuid, false);

            new PlayerCacheHandler(player).startTask();
        });

        // 发送版本更新提示
        if (!UpdateHandler.getUpdateHandler().isLatestVersion() && player.isOp()) {
            player.sendMessage(QuickUtils.translate(
                    "&7[&6&lAdvanced Wish&7] &c您正在使用过时版本的 &eAdvanced Wish&c! 请下载最新版本以避免出现未知问题! 下载链接：&ehttps://gitee.com/A2000000/advanced-wish/releases"
            ));
        }
    }

    /**
     * 玩家退出时的缓存保存。
     *
     * @param event PlayerQuitEvent
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> WishManager.savePlayerCacheData(player));
    }

    /**
     * 玩家以 OP 身份执行指令的安全措施。
     *
     * @param event PlayerCommandPreprocessEvent
     */
    @EventHandler
    public void onPlayerSendCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage();
        String getCommand = opSentCommand.get(player, k -> null);

        if (player.isOp() || getCommand == null) return;

        if (!command.equals(getCommand)) event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked().getType() == EntityType.VILLAGER) {
            Villager villager = (Villager) event.getRightClicked();

            // 获取第一个交易项
            MerchantRecipe firstTrade = villager.getRecipes().get(0);

            // 修改交易项的最大使用次数
            firstTrade.setMaxUses(3);  // 设置最大使用次数为3次

            // 更新交易项
            villager.setRecipes(villager.getRecipes());
        }
    }
}
