package twomillions.plugin.advancedwish.utils.scripts.interop;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import twomillions.plugin.advancedwish.Main;
import twomillions.plugin.advancedwish.annotations.JsInteropJavaType;
import twomillions.plugin.advancedwish.interfaces.ScriptInteropInterface;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * 允许 JavaScript 处理 Bukkit 事件。
 *
 * @author 2000000
 * @date 2023/4/28
 */
@Getter @Setter
@JsInteropJavaType
@SuppressWarnings("unused")
@Builder(setterPrefix = "set")
public class ScriptEventHandler implements ScriptInteropInterface {
    private static final JavaPlugin plugin = Main.getInstance();

    /**
     * ScriptEventHandler 监听器列表。
     */
    @Getter private static final ConcurrentLinkedQueue<ScriptEventHandler> scriptListeners = new ConcurrentLinkedQueue<>();

    /**
     * 是否传递已取消的事件。
     */
    private final boolean ignoreCancelled;

    /**
     * 当监听器监听到事件时要执行的代码。
     */
    private final Consumer<Event> executor;

    /**
     * 事件优先级。
     */
    private final EventPriority eventPriority;

    /**
     * 监听事件类。
     */
    private final Class<? extends Event> eventClass;

    /**
     * 实例。
     */
    @Builder.Default
    private final Listener listener = new Listener() { };

    /**
     * 注册事件监听。
     */
    @Override
    public void register() {
        scriptListeners.add(this);
        Bukkit.getPluginManager().registerEvent(eventClass, listener, eventPriority, (listener, event) -> executor.accept(event), plugin);
    }

    /**
     * 注销事件监听。
     */
    @Override
    public void unregister() {
        HandlerList.unregisterAll(listener);
        scriptListeners.remove(this);
    }
}
