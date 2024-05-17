package twomillions.plugin.advancedwish.abstracts;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

/**
 * 该类继承 {@link Event} 实现 {@link Cancellable}，更加快捷的实现异步事件。
 *
 * @author 2000000
 * @date 2023/4/21
 */
@Getter @Setter
public abstract class AsyncEventAbstract extends Event implements Cancellable {
    private boolean cancelled = false;

    /**
     * 构造器，异步事件。
     */
    public AsyncEventAbstract() {
        super(true);
    }
}
