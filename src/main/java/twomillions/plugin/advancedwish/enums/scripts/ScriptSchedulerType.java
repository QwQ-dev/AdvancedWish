package twomillions.plugin.advancedwish.enums.scripts;

import twomillions.plugin.advancedwish.annotations.JsInteropJavaType;

/**
 * @author 2000000
 * @date 2023/4/28
 */
@JsInteropJavaType
public enum ScriptSchedulerType {
    /**
     * 同步 / 异步。
     */
    runTask,
    runTaskAsync,

    /**
     * 同步循环 / 异步循环。
     */
    runTaskTimer,
    runTaskTimerAsync,

    /**
     * 同步延迟 / 异步延迟。
     */
    runTaskLater,
    runTaskLaterAsync
}
