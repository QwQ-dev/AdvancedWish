package twomillions.plugin.advancedwish.utils.others;

import lombok.experimental.UtilityClass;
import twomillions.plugin.advancedwish.annotations.JsInteropJavaType;

/**
 * 常量类。
 *
 * @author 2000000
 * @date 2023/3/4
 */
@UtilityClass
@JsInteropJavaType
@SuppressWarnings("unused")
public class ConstantsUtils {
    /**
     * 常用资源路径。
     */
    public static final String WISH = "/Wish";
    public static final String PLAYER_CACHE = "/PlayerCache";
    public static final String PLAYER_GUARANTEED = "/PlayerGuaranteed";
    public static final String PLAYER_LOGS = "/PlayerLogs";
    public static final String OTHER_DATA = "/OtherData";
    public static final String SCRIPT_FILE = "/Scripts";

    /**
     * 常用文件扩展名。
     */
    public static final String YAML_FILE_EXTENSION = ".yml";
    public static final String JSON_FILE_EXTENSION = ".json";
    public static final String JAVA_SCRIPT_FILE_EXTENSION = ".js";

    /**
     * 常用数据库通用名称。
     */
    public static final String JSON_DB_TYPE = "json";
    public static final String MONGO_DB_TYPE = "mongo";
    public static final String MONGODB_DB_TYPE = "mongodb";
    public static final String MYSQL_DB_TYPE = "mysql";

    /**
     * 常用数据库集合名称。
     */
    public static final String PLAYER_GUARANTEED_COLLECTION_NAME = "PlayerGuaranteed";
    public static final String PLAYER_LOGS_COLLECTION_NAME = "PlayerLogs";
}
