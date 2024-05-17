package twomillions.plugin.advancedwish.interfaces;

/**
 * 代表一个接受三个参数且有返回值的函数式接口。
 *
 * @param <T> 第一个参数类型
 * @param <U> 第二个参数类型
 * @param <V> 第三个参数类型
 * @param <R> 返回值类型
 *
 * @author 2000000
 * @date 2023/4/29
 */
@FunctionalInterface
public interface TriFunction<T, U, V, R> {
    /**
     * 对给定的参数执行此函数。
     *
     * @param t 第一个参数
     * @param u 第二个参数
     * @param v 第三个参数
     * @return 函数执行的结果
     */
    R apply(T t, U u, V v);
}
