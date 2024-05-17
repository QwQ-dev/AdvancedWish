package twomillions.plugin.advancedwish.utils.events;

import lombok.experimental.UtilityClass;
import twomillions.plugin.advancedwish.annotations.JsInteropJavaType;
import twomillions.plugin.advancedwish.events.*;
import twomillions.plugin.advancedwish.enums.wish.PlayerWishStatus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import twomillions.plugin.advancedwish.events.*;

/**
 * 事件工具类。
 *
 * @author 2000000
 * @date 2023/3/26
 */
@UtilityClass
@JsInteropJavaType
public class EventUtils {
    /**
     * call AsyncPlayerWishEvent
     *
     * @param player player
     * @param playerWishStatus playerWishStatus
     * @param wishName wishName
     * @param isForce isForce
     * @return AsyncPlayerWishEvent
     */
    public static AsyncPlayerWishEvent callAsyncPlayerWishEvent(Player player, PlayerWishStatus playerWishStatus, String wishName, boolean isForce) {
        AsyncPlayerWishEvent asyncPlayerWishEvent = new AsyncPlayerWishEvent(player, wishName, isForce, playerWishStatus);
        Bukkit.getPluginManager().callEvent(asyncPlayerWishEvent);

        return asyncPlayerWishEvent;
    }

    /**
     * call AsyncEffectSendEvent
     *
     * @param fileName fileName
     * @param player player
     * @param path path
     * @param pathPrefix pathPrefix
     * @return AsyncEffectSendEvent
     */
    public static AsyncEffectSendEvent callAsyncEffectSendEvent(String fileName, Player player, String path, String pathPrefix) {
        AsyncEffectSendEvent asyncEffectSendEvent = new AsyncEffectSendEvent(player, fileName, path, pathPrefix);
        Bukkit.getPluginManager().callEvent(asyncEffectSendEvent);

        return asyncEffectSendEvent;
    }

    /**
     * call AsyncWishLimitResetEvent
     *
     * @param wishName wishName
     * @param storeMode storeMode
     * @param isEnabledResetCompleteSend isEnabledResetCompleteSend
     * @param isEnabledResetCompleteSendConsole isEnabledResetCompleteSendConsole
     * @return AsyncWishLimitResetEvent
     */
    public static AsyncWishLimitResetEvent callAsyncWishLimitResetEvent(String wishName, String storeMode
            , boolean isEnabledResetCompleteSend, boolean isEnabledResetCompleteSendConsole) {

        AsyncWishLimitResetEvent asyncWishLimitResetEvent = new AsyncWishLimitResetEvent(wishName, storeMode
                , isEnabledResetCompleteSend, isEnabledResetCompleteSendConsole);

        Bukkit.getPluginManager().callEvent(asyncWishLimitResetEvent);

        return asyncWishLimitResetEvent;
    }

    /**
     * call AsyncPlayerCheckCacheEvent
     *
     * @param player player
     * @param normalPath path
     * @param doListCachePath doListCachePath
     * @return AsyncPlayerCheckCacheEvent
     */
    public static AsyncPlayerCheckCacheEvent callAsyncPlayerCheckCacheEvent(Player player, String normalPath, String doListCachePath) {
        AsyncPlayerCheckCacheEvent asyncPlayerCheckCacheEvent = new AsyncPlayerCheckCacheEvent(player, normalPath, doListCachePath);
        Bukkit.getPluginManager().callEvent(asyncPlayerCheckCacheEvent);

        return asyncPlayerCheckCacheEvent;
    }

    /**
     * call AsyncRecordEffectSendEvent
     *
     * @param player player
     * @param fileName fileName
     * @param path path
     * @param pathPrefix pathPrefix
     * @return AsyncRecordEffectSendEvent
     */
    public static AsyncRecordEffectSendEvent callAsyncRecordEffectSendEvent(Player player, String fileName, String path, String pathPrefix) {
        AsyncRecordEffectSendEvent asyncRecordEffectSendEvent = new AsyncRecordEffectSendEvent(player, fileName, path, pathPrefix);
        Bukkit.getPluginManager().callEvent(asyncRecordEffectSendEvent);

        return asyncRecordEffectSendEvent;
    }
}
