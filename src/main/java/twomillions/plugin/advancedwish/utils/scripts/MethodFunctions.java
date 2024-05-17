package twomillions.plugin.advancedwish.utils.scripts;

import lombok.AllArgsConstructor;
import lombok.Getter;
import twomillions.plugin.advancedwish.managers.WishManager;
import twomillions.plugin.advancedwish.managers.tasks.ScheduledTaskManager;
import twomillions.plugin.advancedwish.utils.others.ConstantsUtils;
import twomillions.plugin.advancedwish.utils.random.RandomGenerator;
import org.bukkit.entity.Player;

/**
 * 提供了一些用于 JavaScript 的方法函数。
 *
 * @author 2000000
 * @date 2023/3/2
 */
@AllArgsConstructor
@SuppressWarnings("UnusedDeclaration")
public class MethodFunctions {
    @Getter final Player player;

    /**
     * 添加玩家的定时任务。
     *
     * @param fileName 文件名
     * @param path 文件路径
     * @param pathPrefix 计划任务执行的前缀节点
     */
    public void addPlayerScheduledTask(String fileName, String path, String pathPrefix) {
        long time = System.currentTimeMillis();
        ScheduledTaskManager.addPlayerScheduledTask(player, time, fileName, path, !path.equals(ConstantsUtils.WISH), pathPrefix);
    }

    /**
     * 添加玩家的定时任务。
     *
     * @param fileName 文件名
     * @param path 文件路径
     * @param pathPrefix 计划任务执行的前缀节点
     * @param delay 延迟毫秒
     */
    public void addPlayerScheduledTask(String fileName, String path, String pathPrefix, long delay) {
        long time = System.currentTimeMillis() + delay;
        ScheduledTaskManager.addPlayerScheduledTask(player, time, fileName, path, !path.equals(ConstantsUtils.WISH), pathPrefix);
    }

    /**
     * 获取此许愿池的许愿结果。
     *
     * @param wishName 许愿池的名称
     * @param actualProcessing 实际处理，是否设置玩家的抽奖次数等等，若为 false 则只是返回最终结果
     * @param returnNode 只返回执行节点
     * @return 若没有可随机的奖品，则返回值为 ""，若 actualProcessing 为 true 则只返回执行节点，否则返回全语句
     */
    public String getFinalWishPrize(String wishName, boolean actualProcessing, boolean returnNode) {
        return WishManager.getFinalWishPrize(player, wishName, actualProcessing, returnNode);
    }

    /**
     * 随机返回一个对象。
     *
     * @param values 由对象和概率值组成的数组，不能为空，长度必须为偶数
     * @return {@link RandomGenerator#getResult()} 随机返回的对象
     */
    public Object randomSentence(Object... values) {
        return new RandomGenerator<>(values).getResult();
    }

    /**
     * 随机返回一个对象。
     *
     * @param values 由对象和概率值组成的数组，不能为空，长度必须为偶数
     * @return {@link RandomGenerator#getResultWithSecureRandom()} 随机返回的对象
     */
    public Object randomSentenceWithSecureRandom(Object... values) {
        return new RandomGenerator<>(values).getResultWithSecureRandom();
    }

    /**
     * 随机返回一个对象。
     *
     * @param values 由对象和概率值组成的数组，不能为空，长度必须为偶数
     * @return {@link RandomGenerator#getResultWithMonteCarlo()} 随机返回的对象
     */
    public Object randomSentenceWithMonteCarlo(Object... values) {
        return new RandomGenerator<>(values).getResultWithMonteCarlo();
    }

    /**
     * 随机返回一个对象。
     *
     * @param values 由对象和概率值组成的数组，不能为空，长度必须为偶数
     * @return {@link RandomGenerator#getResultWithShuffle()} 随机返回的对象
     */
    public Object randomSentenceWithShuffle(Object... values) {
        return new RandomGenerator<>(values).getResultWithShuffle();
    }

    /**
     * 随机返回一个对象。
     *
     * @param values 由对象和概率值组成的数组，不能为空，长度必须为偶数
     * @return {@link RandomGenerator#getResultWithGaussian()} 随机返回的对象
     */
    public Object randomSentenceWithGaussian(Object... values) {
        return new RandomGenerator<>(values).getResultWithGaussian();
    }

    /**
     * 随机返回一个对象。
     *
     * @param values 由对象和概率值组成的数组，不能为空，长度必须为偶数
     * @return {@link RandomGenerator#getResultWithMersenneTwister()} 随机返回的对象
     */
    public Object randomSentenceWithMersenneTwister(Object... values) {
        return new RandomGenerator<>(values).getResultWithMersenneTwister();
    }

    /**
     * 随机返回一个对象。
     *
     * @param values 由对象和概率值组成的数组，不能为空，长度必须为偶数
     * @return {@link RandomGenerator#getResultWithXORShift()} 随机返回的对象
     */
    public Object randomSentenceWithXORShift(Object... values) {
        return new RandomGenerator<>(values).getResultWithXORShift();
    }
}
