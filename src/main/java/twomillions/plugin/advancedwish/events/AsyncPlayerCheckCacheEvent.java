package twomillions.plugin.advancedwish.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import twomillions.plugin.advancedwish.abstracts.AsyncEventAbstract;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * 该类继承 {@link AsyncEventAbstract} 快捷的异步实现 Advanced Wish 事件。
 * 
 * @author 2000000
 * @date 2023/1/31 19:00
 */
@Getter
@AllArgsConstructor
public class AsyncPlayerCheckCacheEvent extends AsyncEventAbstract {
    private final Player player;
    private final String normalPath;
    private final String doListCachePath;

    @Getter private static final HandlerList HandlerList = new HandlerList();

    @Override
    public @NotNull HandlerList getHandlers() {
        return HandlerList;
    }
}