package twomillions.plugin.advancedwish.managers.databases.mongo;

import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import de.leonhard.storage.Yaml;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import twomillions.plugin.advancedwish.enums.databases.status.AuthStatus;
import twomillions.plugin.advancedwish.enums.databases.status.ConnectStatus;
import twomillions.plugin.advancedwish.enums.databases.status.CustomUrlStatus;
import twomillions.plugin.advancedwish.interfaces.DatabasesInterface;
import twomillions.plugin.advancedwish.utils.texts.QuickUtils;
import org.bson.Document;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * 该类实现 {@link DatabasesInterface}，处理 Mongo 操作。
 *
 * @author 2000000
 * @date 2023/1/8 21:23
 */
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MongoManager implements DatabasesInterface {
    private volatile MongoClient mongoClient;
    private volatile DBCollection dbCollection;
    private volatile MongoDatabase mongoDatabase;
    private volatile String mongoClientUrlString;
    private volatile MongoClientURI mongoClientUrl;

    private volatile String ip;
    private volatile String port;
    private volatile String username;
    private volatile String password;

    private volatile AuthStatus authStatus;
    private volatile CustomUrlStatus customUrlStatus;
    private volatile ConnectStatus connectStatus = ConnectStatus.TurnOff;

    @Getter private static final MongoManager mongoManager = new MongoManager();

    /**
     * 根据指定的 YAML 配置，初始化数据库连接。
     *
     * @param yaml 包含数据库连接信息的 YAML 配置
     * @return 数据库连接状态
     */
    @Override
    public ConnectStatus setup(Yaml yaml) {
        // 设置MongoDB登录信息
        setIp(yaml.getString("MONGO.IP"));
        setPassword(yaml.getString("MONGO.PORT"));
        setUsername(yaml.getString("MONGO.AUTH.USER"));
        setPassword(yaml.getString("MONGO.AUTH.PASSWORD"));

        // 检查是否使用自定义的MongoDB连接URL
        setCustomUrlStatus(yaml.getString("MONGO.CUSTOM-URL").isEmpty() ? CustomUrlStatus.TurnOff : CustomUrlStatus.TurnOn);

        // 根据是否使用自定义URL设置连接URL
        if (getCustomUrlStatus() == CustomUrlStatus.TurnOn) {
            setMongoClientUrlString(yaml.getString("MONGO.CUSTOM-URL"));
        } else {
            if (getUsername().isEmpty() || getPassword().isEmpty()) {
                setAuthStatus(AuthStatus.TurnOff);
                setMongoClientUrlString("mongodb://" + getIp() + ":" + getPort() + "/AdvancedWish");
            } else {
                setAuthStatus(AuthStatus.UsingAuth);
                setMongoClientUrlString("mongodb://" + getUsername() + ":" + getPassword() + "@" + getIp() + ":" + getPort() + "/AdvancedWish");

                QuickUtils.sendConsoleMessage("&a检查到 &eMongo&a 开启身份验证，已设置身份验证信息!");
            }
        }

        // 检查MongoDB连接状态
        try {
            setMongoClientUrl(new MongoClientURI(getMongoClientUrlString()));
            setMongoClient(new MongoClient(getMongoClientUrl()));
            setMongoDatabase(getMongoClient().getDatabase("AdvancedWish"));

            QuickUtils.sendConsoleMessage("&a已成功建立与 &eMongoDB&a 的连接!");

            setConnectStatus(ConnectStatus.Connected);
        } catch (Exception exception) {
            QuickUtils.sendConsoleMessage("&c您打开了 &eMongoDB&c 数据库选项，但未能正确连接到 &eMongoDB&c，请检查 &eMongoDB&c 服务状态，即将关闭服务器!");

            setConnectStatus(ConnectStatus.CannotConnect);

            Bukkit.shutdown();
        }

        return getConnectStatus();
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
        MongoCollection<Document> collection = getMongoDatabase().getCollection(databaseCollection);

        Document filter = new Document("_id", uuid);
        Document document = collection.find(filter).first();

        // 如果没有找到，则插入默认值
        if (document == null) {
            document = new Document("_id", uuid).append(key, defaultValue);
            collection.insertOne(document);
            return defaultValue;
        }

        Object value = document.get(key);

        // 如果找到的值为 null，则更新为默认值
        if (value == null) {
            collection.updateOne(filter, new Document("$set", new Document(key, defaultValue)));
            return defaultValue;
        }

        return value;
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
        MongoCollection<Document> collection = getMongoDatabase().getCollection(databaseCollection);

        Document filter = new Document("_id", uuid);
        Document document = collection.find(filter).first();

        // 如果没有找到，则插入默认值
        if (document == null) {
            document = new Document("_id", uuid).append(key, defaultValue);
            collection.insertOne(document);
            return defaultValue;
        }

        ConcurrentLinkedQueue<String> value = new ConcurrentLinkedQueue<>(document.getList(key, String.class));

        // 如果找到的值为 null，则更新为默认值
        if (value.size() == 0) {
            collection.updateOne(filter, new Document("$set", new Document(key, defaultValue)));
            return defaultValue;
        }

        return value;
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
        MongoCollection<Document> collection = getMongoDatabase().getCollection(databaseCollection);
        collection.updateOne(new Document("_id", uuid), new Document("$set", new Document(key, value)), new UpdateOptions().upsert(true));
    }

    /**
     * 获取数据库中的所有集合名称。
     *
     * @return 包含所有集合名称的字符串列表
     */
    public List<String> getAllDatabaseCollectionNames() {
        return getMongoDatabase().listCollectionNames().into(new ArrayList<>());
    }

    /**
     * 获取所有数据集合的所有数据。
     *
     * @param databaseCollection 查询的数据集合
     * @return 以 Map 的形式传递，Map Key 为数据集合名，value Map Key 为 UUID，value 是一个包含键值对的 Map
     */
    @Override
    public Map<String, Map<String, Object>> getAllData(String databaseCollection) {
        MongoCollection<Document> collection = getMongoDatabase().getCollection(databaseCollection);
        List<Document> documents = collection.find().into(new ArrayList<>());

        Map<String, Map<String, Object>> result = new HashMap<>();

        for (Document document : documents) {
            String id = document.getString("_id");
            Map<String, Object> data = document.entrySet().stream()
                    .filter(entry -> !entry.getKey().equals("_id"))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            result.put(id, data);
        }

        return result;
    }
}
