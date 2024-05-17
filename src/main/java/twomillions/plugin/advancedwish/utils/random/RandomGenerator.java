package twomillions.plugin.advancedwish.utils.random;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import twomillions.plugin.advancedwish.annotations.JsInteropJavaType;
import twomillions.plugin.advancedwish.utils.texts.QuickUtils;
import org.apache.commons.math3.random.MersenneTwister;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 一个简单的工具类，用于根据指定的概率随机返回一个对象。
 *
 * @author 2000000
 * @date 2023/2/8
 *
 * @param <T> 随机对象类型
 */
@NoArgsConstructor
public class RandomGenerator<T> {
    /**
     * 存储所有随机对象及其对应的概率。
     */
    private final ConcurrentLinkedQueue<RandomObject<T>> randomObjects = new ConcurrentLinkedQueue<>();

    /**
     * 所有随机对象的总概率。
     */
    private int totalProbability;

    /**
     * 随机数生成器 ThreadLocalRandom。
     */
    private final ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();

    /**
     * 随机数生成器 SecureRandom。
     */
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 通过可选参数创建一个 RandomGenerator 实例。
     *
     * @param values 由对象和概率值组成的数组，不能为空，长度必须为偶数
     * @throws IllegalArgumentException 如果概率值不是整数或缺少概率值，则抛出该异常
     */
    @SafeVarargs
    public RandomGenerator(T... values) {
        for (int i = 0; i < values.length; i += 2) {
            if (i + 1 >= values.length) {
                throw new IllegalArgumentException("Missing probability value for object: " + values[i]);
            }

            T object = values[i];
            Object probabilityValue = values[i + 1];

            if (!QuickUtils.isInt(probabilityValue.toString())) {
                throw new IllegalArgumentException("Probability value for object " + values[i] + " is not an integer.");
            }

            addRandomObject(object, (int) Double.parseDouble(probabilityValue.toString()));
        }
    }

    /**
     * 向工具类中添加一个随机对象及其对应的概率。
     *
     * @param object 随机对象
     * @param probability 对应的概率
     */
    public void addRandomObject(T object, int probability) {
        if (probability <= 0) {
            return;
        }

        RandomObject<T> randomObject = new RandomObject<>(object, probability);

        randomObjects.add(randomObject);
        totalProbability += probability;
    }

    /**
     * 对随机进行检查。
     *
     * @throws IllegalArgumentException 如果没有可供随机的项目或总概率为 0
     */
    private void doSecurityCheck() {
        if (randomObjects.size() == 0) {
            throw new IllegalArgumentException("No objects to randomize!");
        }

        if (totalProbability <= 0) {
            throw new IllegalArgumentException("Random probability of error, totalProbability: " + totalProbability);
        }
    }

    /**
     * 根据当前所有随机对象的概率，随机返回其中的一个对象。
     *
     * <p>此方法使用普通的伪随机数生成器 (PRNG) 生成随机数，它生成的随机数是不可预测的，但不是真正的随机数。这是最常用的。
     *
     * <p>总的来说: 使用普通的伪随机数生成器 (PRNG)，效率较高，随机性也不错。适合用于一般场合。
     *
     * @return 随机对象，若没有随机对象则返回 null
     */
    public T getResult() {
        doSecurityCheck();

        int randomNumber = threadLocalRandom.nextInt(totalProbability);
        int cumulativeProbability = 0;

        for (RandomObject<T> randomObject : randomObjects) {
            cumulativeProbability += randomObject.getProbability();
            if (randomNumber < cumulativeProbability) {
                return randomObject.getObject();
            }
        }

        return null;
    }

    /**
     * 根据当前所有随机对象的概率，随机返回其中的一个对象。
     *
     * <p>此方法使用更加安全的随机数生成器，提供更高的随机性和安全性。
     * 使用更多的随机源 (例如系统噪音、硬件事件等) 生成随机数，生成的结果更加随机化。
     *
     * <p>总的来说: 使用安全的随机数生成器，能够生成高质量的随机数。适合用于需要高度安全性和随机性的场合。
     *
     * @return 随机对象，若没有随机对象则返回 null
     */
    public T getResultWithSecureRandom() {
        doSecurityCheck();

        int randomNumber = secureRandom.nextInt(totalProbability);
        int cumulativeProbability = 0;

        for (RandomObject<T> randomObject : randomObjects) {
            cumulativeProbability += randomObject.getProbability();
            if (randomNumber < cumulativeProbability) {
                return randomObject.getObject();
            }
        }

        return null;
    }

    /**
     * 根据当前所有随机对象的概率，随机返回其中的一个对象。
     *
     * <p>此方法使用蒙特卡罗方法生成随机结果，该方法使用的随机性更加均匀和公平。
     * 因为每个对象的概率与它们在队列中出现的次数成正比。这种方法也能够处理大量的对象和概率，性能较低，生成的结果也更加公平。
     *
     * <p>总的来说: 使用蒙特卡罗方法，能够处理大量的对象和概率，性能较低，生成的结果更加公平。适合用于需要高度公平的场合。
     *
     * @return 随机对象
     */
    public T getResultWithMonteCarlo() {
        doSecurityCheck();

        ConcurrentLinkedQueue<T> objects = new ConcurrentLinkedQueue<>();

        for (RandomObject<T> randomObject : randomObjects) {
            for (int i = 0; i < randomObject.getProbability(); i++) {
                objects.add(randomObject.getObject());
            }
        }

        int index = threadLocalRandom.nextInt(objects.size());
        return new ArrayList<>(objects).get(index);
    }

    /**
     * 根据当前所有随机对象的概率，随机返回其中的一个对象。
     *
     * <p>此方法使用随机排序方法将对象列表打乱，洗牌方法。选择第一个对象作为随机结果。
     * 这种方法不太公平，因为它只返回列表中的第一个对象，其他对象的概率更低。此外，此方法的效率也不够高，因为它需要将整个列表随机排序。
     *
     * <p>总的来说: 进行打乱，可以生成相对均匀的分布，效率较低，随机性高，不太公平。适合用于需要高度随机性的场合。
     *
     * @return 随机对象
     */
    public T getResultWithShuffle() {
        doSecurityCheck();

        ConcurrentLinkedQueue<T> objects = new ConcurrentLinkedQueue<>();

        for (RandomObject<T> randomObject : randomObjects) {
            for (int i = 0; i < randomObject.getProbability(); i++) {
                objects.add(randomObject.getObject());
            }
        }

        ArrayList<T> list = new ArrayList<>(objects);

        Collections.shuffle(list);
        return list.get(0);
    }

    /**
     * 根据当前所有随机对象的概率，随机返回其中的一个对象。
     *
     * <p>此方法使用高斯分布随机数生成器，生成的随机数是具有高斯分布的。。
     *
     * <p>总的来说: 使用高斯分布，高斯分布会将随机结果偏向均值，可以在一定程度上避免随机数集中在中间值的问题。适合用于需要正态分布随机数的场合。
     *
     * @return 随机对象，若没有随机对象则返回 null
     */
    public T getResultWithGaussian() {
        doSecurityCheck();

        double mean = totalProbability / 2.0;
        double standardDeviation = totalProbability / 6.0;

        int randomNumber = (int) Math.round(threadLocalRandom.nextGaussian() * standardDeviation + mean);

        randomNumber = Math.max(0, randomNumber);
        randomNumber = Math.min(totalProbability - 1, randomNumber);

        int cumulativeProbability = 0;

        for (RandomObject<T> randomObject : randomObjects) {
            cumulativeProbability += randomObject.getProbability();
            if (randomNumber < cumulativeProbability) {
                return randomObject.getObject();
            }
        }

        return null;
    }

    /**
     * 根据当前所有随机对象的概率，随机返回其中的一个对象。
     *
     * <p>此方法使用 Mersenne Twister 伪随机数生成器生成随机数。该算法具有良好的随机性和周期性。
     * 在所有伪随机数生成算法中，Mersenne Twister 的周期最长。使用此方法生成的随机数具有很高的随机性。
     *
     * <p>总的来说: 使用 Mersenne Twister 随机数生成器，具有良好的随机性和速度。适合用于需要高质量随机数的场合。
     *
     * @return 随机对象，若没有随机对象则返回 null
     */
    public T getResultWithMersenneTwister() {
        doSecurityCheck();

        org.apache.commons.math3.random.RandomGenerator randomGenerator = new MersenneTwister();

        int randomNumber = randomGenerator.nextInt(totalProbability);

        int cumulativeProbability = 0;

        for (RandomObject<T> randomObject : randomObjects) {
            cumulativeProbability += randomObject.getProbability();
            if (randomNumber < cumulativeProbability) {
                return randomObject.getObject();
            }
        }

        return null;
    }

    /**
     * 根据当前所有随机对象的概率，随机返回其中的一个对象。
     *
     * <p>此方法使用 XORShift 伪随机数生成器生成随机数。该算法具有良好的随机性和速度。
     * 在所有伪随机数生成算法中，XORShift 是速度最快的之一，但随机性不如其他算法。
     *
     * <p>总的来说: 使用 XORShift 随机数生成器，速度较快。适合用于需要高效率并且对于随机性要求不高的场合。
     *
     * @return 随机对象，若没有随机对象则返回 null
     */
    public T getResultWithXORShift() {
        doSecurityCheck();

        int y = (int) System.nanoTime();

        y ^= (y << 6);
        y ^= (y >>> 21);
        y ^= (y << 7);

        int randomNumber = Math.abs(y) % totalProbability;

        int cumulativeProbability = 0;

        for (RandomObject<T> randomObject : randomObjects) {
            cumulativeProbability += randomObject.getProbability();
            if (randomNumber < cumulativeProbability) {
                return randomObject.getObject();
            }
        }

        return null;
    }

    /**
     * 表示一个随机对象及其对应的概率。
     *
     * @param <T> 随机对象类型
     */
    @AllArgsConstructor
    private static class RandomObject<T> {
        /**
         * 随机对象。
         */
        private final T object;

        /**
         * 随机对象的概率。
         */
        private final int probability;

        /**
         * 获取随机对象。
         *
         * @return 随机对象
         */
        public T getObject() {
            return object;
        }

        /**
         * 获取随机对象的概率。
         *
         * @return 随机对象的概率
         */
        public int getProbability() {
            return probability;
        }
    }
}
