package twomillions.plugin.advancedwish.abstracts;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.scheduler.BukkitTask;

/**
 * 任务抽象类。
 *
 * @author 2000000
 * @date 2023/4/21
 */
@Getter @Setter
public abstract class TasksAbstract {
    private BukkitTask bukkitTask;

    /**
     * 开始任务。
     */
    public abstract void startTask();

    /**
     * 结束任务。
     *
     * @throws IllegalArgumentException 如果 BukkitTask 为空，既该任务并没有开始执行
     */
    public void cancelTask() {
        if (bukkitTask == null) {
            throw new IllegalArgumentException("This task is not start, Bukkit Task is null");
        }

        bukkitTask.cancel();
    }
}
