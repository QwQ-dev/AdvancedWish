package twomillions.plugin.advancedwish.utils.others;

import lombok.experimental.UtilityClass;
import twomillions.plugin.advancedwish.annotations.JsInteropJavaType;
import org.bukkit.entity.Player;

/**
 * 玩家经验管理工具，由 2000000 进行修改。
 * 参考实现: <a href="https://gist.github.com/Jikoo/30ec040443a4701b8980">Jikoo</a>
 */
@UtilityClass
@JsInteropJavaType
public final class ExpUtils {
    /**
     * 计算玩家总经验。
     *
     * @param player 玩家
     * @return 玩家总经验
     *
     * @see <a href=http://minecraft.gamepedia.com/Experience#Leveling_up>经验值#升级</a>
     */
    public static int getExp(Player player) {
        return levelToExp(player.getLevel()) + Math.round(getNextLevelExp(player.getLevel()) * player.getExp());
    }

    /**
     * 根据等级计算经验值。
     *
     * @param level 等级
     * @return 总经验值
     *
     * @see <a href=http://minecraft.gamepedia.com/Experience#Leveling_up>经验值#升级</a>
     */
    public static int levelToExp(int level) {
        if (level <= 16) {
            return level * level + 6 * level;
        }

        if (level <= 31) {
            return (int) (2.5 * level * level - 40.5 * level + 360);
        }

        return (int) (4.5 * level * level - 162.5 * level + 2220);
    }

    /**
     * 根据经验值计算等级和升级进度。
     *
     * @param exp 经验值
     * @return 玩家等级和升级进度
     */
    public static double getLevelFromExp(long exp) {
        int level = expToLevel(exp);

        float remainder = exp - (float) levelToExp(level);

        float progress = remainder / getNextLevelExp(level);

        return ((double) level) + progress;
    }

    /**
     * 根据经验值计算等级。
     *
     * @param exp 经验值
     * @return 等级
     *
     * @see <a href=http://minecraft.gamepedia.com/Experience#Leveling_up>经验值#升级</a>
     */
    public static int expToLevel(long exp) {
        if (exp > 1508) {
            return (int) ((Math.sqrt(72 * exp - 54215) + 325) / 18);
        }

        if (exp > 353) {
            return (int) (Math.sqrt(40 * exp - 7839) / 10 + 8.1);
        }

        return (int) (Math.sqrt(exp + 9) - 3);
    }

    /**
     * 根据等级获取升级所需经验值。
     *
     * @param level 等级
     * @return 升级所需经验值
     * @see <a href=http://minecraft.gamepedia.com/Experience#Leveling_up>Experience#Leveling_up</a>
     */
    private static int getNextLevelExp(int level) {
        if (level >= 31) {
            return 9 * level - 158;
        }

        if (level >= 16) {
            return 5 * level - 38;
        }

        return level * 2 + 7;
    }

    /**
     * 添加玩家经验
     *
     * <p>这种方法优于 {@link Player#giveExp(int)}
     * 在旧版本中，该方法不考虑每个等级的 exp 差异。这会导致在给予玩家大量经验时过度升级
     * 在新版本中，虽然每个级别的经验量不同，但使用的方法是循环繁重的，需要大量的计算，这使得它非常缓慢
     *
     * @param player player
     * @param exp the amount of experience to add or remove
     */
    public static void addExp(Player player, int exp) {
        exp += getExp(player);

        if (exp < 0) {
            exp = 0;
        }

        double levelAndExp = getLevelFromExp(exp);
        int level = (int) levelAndExp;

        player.setLevel(level);
        player.setExp((float) (levelAndExp - level));
    }
}
