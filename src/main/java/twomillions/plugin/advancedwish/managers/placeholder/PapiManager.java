package twomillions.plugin.advancedwish.managers.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import twomillions.plugin.advancedwish.Main;
import twomillions.plugin.advancedwish.managers.WishManager;
import twomillions.plugin.advancedwish.managers.register.RegisterManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * 该类继承 {@link PlaceholderExpansion}，用于处理 Advanced Wish 额外变量。
 *
 * @author 2000000
 * @date 2022/12/2 13:16
 */
public class PapiManager extends PlaceholderExpansion {
    private static final JavaPlugin plugin = Main.getInstance();

    /**
     * 获取标识符。
     *
     * @return 插件标识符
     */
    @Override
    public @NotNull String getIdentifier() {
        return "aw";
    }

    /**
     * 获取作者。
     *
     * @return 插件作者
     */
    @Override
    public @NotNull String getAuthor() {
        return "TwoMillions";
    }

    /**
     * 获取版本。
     *
     * @return 插件版本
     */
    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    /**
     * 处理 Papi.
     *
     * @param player 离线玩家
     * @param params 请求参数
     * @return 请求结果
     */
    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        String playerName = player.getName();

        Optional<String> result = RegisterManager.getRegisterWish().stream()
                .filter(wishName -> {
                    String[] wishParams = params.split("_");

                    String wishType = wishParams[0].toLowerCase();
                    String wishName2 = wishParams[1].toLowerCase();

                    if (!wishType.equals("amount") && !wishType.equals("guaranteed") && !wishType.equals("limit")) {
                        return false;
                    }

                    return wishName2.equals(wishName);
                })
                .findFirst()
                .map(wishName -> {
                    String[] wishParams = params.split("_");
                    String player2Name = null;

                    if (wishParams.length >= 3) {
                        player2Name = params.substring(params.indexOf('_', params.indexOf('_') + 1) + 1);
                    }

                    switch (wishParams[0].toLowerCase()) {
                        case "amount":
                            return Integer.toString(WishManager.getPlayerWishAmount(player2Name == null ? playerName : player2Name, wishName));
                        case "guaranteed":
                            return Double.toString(WishManager.getPlayerWishGuaranteed(player2Name == null ? playerName : player2Name, wishName));
                        case "limit":
                            return Integer.toString(WishManager.getPlayerWishLimitAmount(player2Name == null ? playerName : player2Name, wishName));
                        default:
                            return "&7Unknown";
                    }
                });

        return result.orElse("&7Unknown");
    }
}