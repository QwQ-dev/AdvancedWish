package twomillions.plugin.advancedwish.tasks;

import lombok.Getter;
import lombok.Setter;
import twomillions.plugin.advancedwish.Main;
import twomillions.plugin.advancedwish.abstracts.TasksAbstract;
import twomillions.plugin.advancedwish.managers.config.ConfigManager;
import twomillions.plugin.advancedwish.utils.texts.QuickUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.URL;
import java.util.Scanner;

/**
 * 该类继承 {@link TasksAbstract}，用于处理插件更新检查。
 *
 * @author 2000000
 * @date 2022/11/24 16:49
 */
@Getter @Setter
public class UpdateHandler extends TasksAbstract {
    private final Runnable runnable;

    private static final JavaPlugin plugin = Main.getInstance();

    /**
     * 是否为最新版
     */
    private boolean isLatestVersion = true;

    /**
     * 获取实例。
     */
    @Getter private static final UpdateHandler updateHandler = new UpdateHandler();

    /**
     * 构造器。
     */
    private UpdateHandler() {
        runnable = () -> {
            if (!ConfigManager.getAdvancedWishYaml().getBoolean("UPDATE-CHECKER")) {
                return;
            }

            String urlString = getURLString().replaceAll(" ", "");

            if (urlString.equals(plugin.getDescription().getVersion())) {
                isLatestVersion = true;

                QuickUtils.sendConsoleMessage("&a自动更新检查完成，您目前正在使用最新版的 &eAdvanced Wish&a! 版本: &e" + plugin.getDescription().getVersion());
            } else if (!urlString.isEmpty()) {
                isLatestVersion = false;

                QuickUtils.sendConsoleMessage("&c您目前正在使用过时的 &eAdvanced Wish&c! 请更新以避免服务器出现问题! 下载链接: &ehttps://gitee.com/A2000000/advanced-wish/releases/");
            }
        };
    }

    /**
     * 检查插件更新，并向控制台输出版本信息。
     * 如果开启了更新检查并且获取最新版本信息失败，会向控制台输出信息。
     */
    @Override
    public void startTask() {
        int cycle = ConfigManager.getAdvancedWishYaml().getInt("CHECK-CYCLE");
        setBukkitTask(Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, 0, (long) cycle * 1200));
    }

    /**
     * 获取指定网址的页面内容。
     * 如果获取失败，会将 isLatestVersion 置为 false，并向控制台输出信息。
     *
     * @return 获取的网页内容，如果获取失败返回空字符串。
     */
    private String getURLString() {
        StringBuilder stringBuilder = new StringBuilder();

        /*
         * 2023.3.25 换为美国服务器
         */
        try (Scanner sc = new Scanner(new URL("http://pluginUpdate.twomillions.top/advancedwishupdate.html").openStream())) {
            while (sc.hasNextLine()) {
                stringBuilder.append(sc.nextLine()).append(' ');
            }
        } catch (IOException exception) {
            isLatestVersion = false;
            QuickUtils.sendConsoleMessage("&cAdvanced Wish 更新检查错误... 请务必手动检查插件是否为最新版。 下载链接: https://gitee.com/A2000000/advanced-wish/releases/");
        }

        return stringBuilder.toString();
    }
}
