package twomillions.plugin.advancedwish.managers.databases;

import de.leonhard.storage.Yaml;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import twomillions.plugin.advancedwish.annotations.JsInteropJavaType;
import twomillions.plugin.advancedwish.enums.databases.types.DataStorageType;
import twomillions.plugin.advancedwish.interfaces.DatabasesInterface;
import twomillions.plugin.advancedwish.managers.databases.json.JsonManager;
import twomillions.plugin.advancedwish.managers.databases.mongo.MongoManager;
import twomillions.plugin.advancedwish.managers.databases.mysql.MySQLManager;
import twomillions.plugin.advancedwish.utils.exceptions.ExceptionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 该类实现 {@link DatabasesInterface}，处理总数据操作。
 *
 * @author 2000000
 * @date 2023/3/23
 */
@JsInteropJavaType
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DatabasesManager implements DatabasesInterface {
    /**
     * DataStorageType.
     */
    @Setter @Getter private static DataStorageType dataStorageType;

    /**
     * DatabasesManager.
     */
    @Getter @Setter private static DatabasesManager databasesManager = new DatabasesManager();

    /**
     * DatabasesManager.getMongoManager().
     */
    @Getter @Setter private static MongoManager mongoManager = MongoManager.getMongoManager();

    /**
     * DatabasesManager.getMySQLManager().
     */
    @Getter @Setter private static MySQLManager mySQLManager = MySQLManager.getMySQLManager();

    /**
     * DatabasesManager.getJsonManager().
     */
    @Getter @Setter private static JsonManager jsonManager = JsonManager.getJsonManager();

    /**
     * 根据指定的 YAML 配置，初始化数据库连接。
     *
     * @param yaml 包含数据库连接信息的 YAML 配置
     * @return 数据库连接状态
     */
    @Override
    public Object setup(Yaml yaml) {
        switch (getDataStorageType()) {
            case MongoDB:
                return getMongoManager().setup(yaml);

            case MySQL:
                return getMySQLManager().setup(yaml);

            case Json:
                return getJsonManager().setup(yaml);

            default:
                return ExceptionUtils.throwUnknownDataStoreType();
        }
    }

    /**
     * 根据给定的 UUID、Key 和默认值获取对应的值，若未找到则插入默认值并返回。若找到的值为 null，则更新为默认值并返回。
     *
     * @param uuid 标识符
     * @param key 查询的 Key
     * @param defaultValue 查询的默认值
     * @param databaseCollection 查询的数据集合
     * @return 对应的值
     */
    @Override
    public Object getOrDefault(String uuid, String key, Object defaultValue, String databaseCollection) {
        switch (getDataStorageType()) {
            case MongoDB:
                return getMongoManager().getOrDefault(uuid, key, defaultValue, databaseCollection);

            case MySQL:
                return getMySQLManager().getOrDefault(uuid, key, defaultValue, databaseCollection);

            case Json:
                return getJsonManager().getOrDefault(uuid, key, defaultValue, databaseCollection);

            default:
                return ExceptionUtils.throwUnknownDataStoreType();
        }
    }

    /**
     * 根据给定的 UUID、Key 和默认值获取对应的 List 值，若未找到则插入默认值并返回。若找到的值为 null，则更新为默认值并返回。
     *
     * @param uuid 标识符
     * @param key 查询的 Key
     * @param defaultValue 查询的默认值
     * @param databaseCollection 查询的数据集合
     * @return 对应的 List 值
     */
    @Override
    public ConcurrentLinkedQueue<String> getOrDefaultList(String uuid, String key, ConcurrentLinkedQueue<String> defaultValue, String databaseCollection) {
        switch (getDataStorageType()) {
            case MongoDB:
                return getMongoManager().getOrDefaultList(uuid, key, defaultValue, databaseCollection);

            case MySQL:
                return getMySQLManager().getOrDefaultList(uuid, key, defaultValue, databaseCollection);

            case Json:
                return getJsonManager().getOrDefaultList(uuid, key, defaultValue, databaseCollection);

            default:
                return ExceptionUtils.throwUnknownDataStoreType();
        }
    }

    /**
     * 更新玩家数据。
     *
     * @param uuid 标识符
     * @param key 查询的 Key
     * @param value 数据的值
     * @param databaseCollection 更新的数据集合
     */
    @Override
    public void update(String uuid, String key, Object value, String databaseCollection) {
        switch (getDataStorageType()) {
            case MongoDB:
                getMongoManager().update(uuid, key, value, databaseCollection);
                break;

            case MySQL:
                getMySQLManager().update(uuid, key, value, databaseCollection);
                break;

            case Json:
                getJsonManager().update(uuid, key, value, databaseCollection);
                break;

            default:
                ExceptionUtils.throwUnknownDataStoreType();
        }
    }

    /**
     * 获取数据库中的所有数据集合名称。
     *
     * @return 包含所有集合名称的字符串列表
     */
    @Override
    public List<String> getAllDatabaseCollectionNames() {
        switch (getDataStorageType()) {
            case MongoDB:
                return getMongoManager().getAllDatabaseCollectionNames();

            case MySQL:
                return getMySQLManager().getAllDatabaseCollectionNames();

            case Json:
                return getJsonManager().getAllDatabaseCollectionNames();

            default:
                return ExceptionUtils.throwUnknownDataStoreType();
        }
    }

    /**
     * 获取所有数据集合的所有数据。
     *
     * @param databaseCollection 查询的数据集合
     * @return 以 Map 的形式传递，Map Key 为数据集合名，value Map Key 为 UUID，value 是一个包含键值对的 Map
     */
    @Override
    public Map<String, Map<String, Object>> getAllData(String databaseCollection) {
        switch (getDataStorageType()) {
            case MongoDB:
                return getMongoManager().getAllData(databaseCollection);

            case MySQL:
                return getMySQLManager().getAllData(databaseCollection);

            case Json:
                return getJsonManager().getAllData(databaseCollection);

            default:
                return ExceptionUtils.throwUnknownDataStoreType();
        }
    }

    /**
     * 数据迁移。
     *
     * @param yaml 包含数据库连接信息的 YAML 配置
     * @param from 原存储类型
     * @param to 新存储类型
     * @return 是否成功迁移
     */
    public static boolean dataMigration(Yaml yaml, DataStorageType from, DataStorageType to) {
        try {
            List<Map<String, Map<String, Map<String, Object>>>> dataList = new ArrayList<>();

            switch (from) {
                case MongoDB:
                    getMongoManager().setup(yaml);
                    dataList = getMongoManager().getAllData();
                    break;

                case MySQL:
                    getMySQLManager().setup(yaml);
                    dataList = getMySQLManager().getAllData();
                    break;

                case Json:
                    getJsonManager().setup(yaml);
                    dataList = getJsonManager().getAllData();
                    break;

                default:
                    ExceptionUtils.throwUnknownDataStoreType();
                    break;
            }

            if (dataList.isEmpty()) {
                return false;
            }

            switch (to) {
                case MongoDB:
                    getMongoManager().setup(yaml);
                    getMongoManager().insertAllData(dataList);
                    break;

                case MySQL:
                    getMySQLManager().setup(yaml);
                    getMySQLManager().insertAllData(dataList);
                    break;

                case Json:
                    getJsonManager().setup(yaml);
                    getJsonManager().insertAllData(dataList);
                    break;

                default:
                    ExceptionUtils.throwUnknownDataStoreType();
                    break;
            }

            return true;
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
    }
}
