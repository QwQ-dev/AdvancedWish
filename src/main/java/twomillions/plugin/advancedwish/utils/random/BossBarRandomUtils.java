package twomillions.plugin.advancedwish.utils.random;

import lombok.experimental.UtilityClass;
import twomillions.plugin.advancedwish.annotations.JsInteropJavaType;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;

/**
 * 随机获取 BossBar 的颜色和样式。
 *
 * @author 2000000
 * @date 2023/2/8
 */
@UtilityClass
@JsInteropJavaType
public class BossBarRandomUtils {
    /**
     * 随机获取一个 {@link BarColor} 颜色
     *
     * @return 随机颜色
     */
    public static BarColor randomColor() {
        RandomGenerator<BarColor> randomUtils = new RandomGenerator<>();

        for (BarColor value : BarColor.values()) {
            randomUtils.addRandomObject(value, 1);
        }

        return randomUtils.getResult();
    }

    /**
     * 随机获取一个 {@link BarStyle} 样式
     *
     * @return 随机样式
     */
    public static BarStyle randomStyle() {
        RandomGenerator<BarStyle> randomUtils = new RandomGenerator<>();

        for (BarStyle value : BarStyle.values()) {
            randomUtils.addRandomObject(value, 1);
        }

        return randomUtils.getResult();
    }
}