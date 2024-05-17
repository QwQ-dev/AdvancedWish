package twomillions.plugin.advancedwish.managers.databases.json;

import de.leonhard.storage.Json;
import de.leonhard.storage.Yaml;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import twomillions.plugin.advancedwish.Main;
import twomillions.plugin.advancedwish.interfaces.DatabasesInterface;
import twomillions.plugin.advancedwish.managers.config.ConfigManager;
import twomillions.plugin.advancedwish.utils.others.ConstantsUtils;
import twomillions.plugin.advancedwish.utils.texts.QuickUtils;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 该类实现 {@link DatabasesInterface}，处理 Json 操作。
 *
 * @author 2000000
 * @date 2023/4/1
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JsonManager implements DatabasesInterface {
    @Getter private static final JsonManager jsonManager = new JsonManager();

    /**
     * 根据指定的 YAML 配置，初始化。
     *
     * @param yaml YAML
     * @return 无论如何都为 true
     */
    @Override
    public Object setup(Yaml yaml) {
        return true;
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
        String path = getPath(databaseCollection);
        return ConfigManager.createJson(uuid, path, true, false).getOrDefault(key, defaultValue);
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
        String path = getPath(databaseCollection);
        return new ConcurrentLinkedQueue<>(ConfigManager.createJson(uuid, path, true, false).getOrDefault(key, defaultValue));
    }

    /**
     * 根据给定的 UUID、Key 更新数据值，若未找到则插入数据值并返回。
     *
     * @param uuid 标识符
     * @param key 查询的 Key
     * @param value 数据值
     * @param databaseCollection 更新的数据集合
     */
    @Override
    public void update(String uuid, String key, Object value, String databaseCollection) {
        String path = getPath(databaseCollection);
        ConfigManager.createJson(uuid, path, true, false).set(key, value);
    }

    /**
     * 获取数据库中的所有集合名称。
     *
     * @return 包含所有集合名称的字符串列表
     */
    public List<String> getAllDatabaseCollectionNames() {
        return new ArrayList<>(ConfigManager.getAllFolderNames(Main.getOtherDataPath()));
    }

    /**
     * 获取所有数据集合的所有数据。
     *
     * @param databaseCollection 查询的数据集合
     * @return 以 Map 的形式传递，Map Key 为数据集合名，value Map Key 为 UUID，value 是一个包含键值对的 Map
     */
    public Map<String, Map<String, Object>> getAllData(String databaseCollection) {
        String path = getPath(databaseCollection);

        Map<String, Map<String, Object>> result = new HashMap<>();

        ConfigManager.getAllFileNames(path).forEach(fileName -> {
            Json json = ConfigManager.createJson(fileName, path, true, false);

            Set<String> jsonKeySet = json.keySet();
            Map<String, Object> subMap = new HashMap<>();

            for (String key : jsonKeySet) {
                Object value = json.get(key);
                subMap.put(key, value);
            }

            result.put(fileName.split(ConstantsUtils.JSON_FILE_EXTENSION)[0], subMap);
        });


        return result;
    }

    /**
     * 获取数据集合对应的文件路径。
     *
     * @param databaseCollection 集合
     * @return 路径
     */
    public String getPath(String databaseCollection) {
        switch (databaseCollection) {
            case ConstantsUtils.PLAYER_LOGS_COLLECTION_NAME:
                return Main.getLogsPath();

            case ConstantsUtils.PLAYER_GUARANTEED_COLLECTION_NAME:
                return Main.getGuaranteedPath();

            default:
                return QuickUtils.handleString(Main.getOtherDataPath() + "/" + databaseCollection);
        }
    }
}
