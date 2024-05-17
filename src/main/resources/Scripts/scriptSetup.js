/**
 * 初始化 Script / 开启插件 / 指令插件 时触发 scriptSetup 函数。
 *
 * 卸载时触发 onDisable 函数。
 */
registerListener();

/**
 * 使用 ScriptEventHandler 注册监听器。
 *
 * 除了监听器之外，我们还支持您创建 Placeholder API 拓展、Bukkit 线程调度、指令与补全等。
 * 详细内容请查看: https://gitee.com/A2000000/advanced-wish/wikis/%E6%8B%93%E5%B1%95%E5%BC%80%E5%8F%91/JavaScript%20%E8%84%9A%E6%9C%AC/1.%20JavaScript
 */
function registerListener() {
    var asyncPlayerWishEventScriptEventHandler = ScriptEventHandler
        .builder()
        /**
         * 设置监听事件。
         */
        .setEventClass(Packages.twomillions.plugin.advancedwish.events.AsyncPlayerWishEvent)
        /**
         * 设置监听权重。
         */
        .setEventPriority(Packages.org.bukkit.event.EventPriority.NORMAL)
        /**
         * 设置是否传递已取消的事件。
         */
        .setIgnoreCancelled(false)
        /**
         * 当监听器监听到事件时要执行的代码。
         * 此处代码执行线程与触发事件的线程有关，而非强制同步或异步。
         * 比如此处监听 AsyncPlayerWishEvent，它是异步线程执行的。
         */
        .setExecutor(
            function(event) {
                QuickUtils.sendConsoleMessage("&a这里是 &e脚本监听器&a! 已触发 &eAsyncPlayerWishEvent&a! 这是同步的吗: &e" + Bukkit.isPrimaryThread() + "&a，您可以在 &e" + Main.getScriptPath() + " &a文件夹下看到脚本文件!");
            }
        )
        .build();

    /**
     * 注册。
     */
    asyncPlayerWishEventScriptEventHandler.register();

    QuickUtils.sendConsoleMessage("&a这里是 &e脚本监听器&a! 已注册 &eAsyncPlayerWishEvent&a 事件监听!" + "&a，您可以在 &e" + Main.getScriptPath()  + " &a文件夹下看到脚本文件!");
}