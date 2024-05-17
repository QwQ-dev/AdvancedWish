package twomillions.plugin.advancedwish.interfaces;

/**
 * 代表一个接受四个参数且没有返回值的函数式接口。
 *
 * @param <T> 第一个参数类型
 * @param <U> 第二个参数类型
 * @param <V> 第三个参数类型
 *
 * @author 2000000
 * @date 2023/4/29
 */
@FunctionalInterface
public interface QuadConsumer<T, U, V, W> {
    /**
     * 对给定的参数执行此函数。
     *
     * @param t 第一个参数
     * @param u 第二个参数
     * @param v 第三个参数
     * @param w 第四个参数
     */
    void accept(T t, U u, V v, W w);
}