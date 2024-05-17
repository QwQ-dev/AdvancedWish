package twomillions.plugin.advancedwish.utils.others;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.experimental.UtilityClass;
import twomillions.plugin.advancedwish.Main;
import twomillions.plugin.advancedwish.annotations.JsInteropJavaType;
import org.bukkit.Bukkit;

/**
 * Caffeine 缓存工具类。
 *
 * @see Caffeine
 *
 * @author 2000000
 * @date 2023/2/20
 */
@UtilityClass
@JsInteropJavaType
public class CaffeineUtils {
    /**
     * 构建一个 Caffeine 缓存，使用 Bukkit 的异步任务线程池来执行缓存操作。
     *
     * @param <K> 缓存键的类型
     * @param <V> 缓存值的类型
     * @return 一个配置好的 Caffeine 缓存
     */
    public static <K, V> Cache<K, V> buildBukkitCache() {
        return Caffeine.newBuilder().executor(runnable -> Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), runnable)).build();
    }
}

