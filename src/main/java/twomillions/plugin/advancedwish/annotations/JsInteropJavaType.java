package twomillions.plugin.advancedwish.annotations;

import twomillions.plugin.advancedwish.Main;
import twomillions.plugin.advancedwish.utils.scripts.ScriptUtils;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.annotation.*;
import java.util.Set;

/**
 * JsInteropJavaType 注解。
 * 用于在 {@link ScriptUtils} 中加载 Java 类到引擎。
 *
 * @author 2000000
 * @date 2023/4/27
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsInteropJavaType {
    class Processor {
        /**
         * 使用 Reflections 获取所有使用 JsInteropJavaType 注解的类。
         *
         * @return 所有使用 JsInteropJavaType 注解的类
         */
        public static Set<Class<?>> getClasses() {
            return new Reflections(new ConfigurationBuilder()
                    .setUrls(ClasspathHelper.forPackage(Main.getPackageName())))
                    .getTypesAnnotatedWith(JsInteropJavaType.class);
        }
    }
}