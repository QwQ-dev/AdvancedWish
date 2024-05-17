package twomillions.plugin.advancedwish.managers.databases.mysql;

import de.leonhard.storage.Yaml;
import lombok.*;
import twomillions.plugin.advancedwish.enums.databases.status.AuthStatus;
import twomillions.plugin.advancedwish.enums.databases.status.ConnectStatus;
import twomillions.plugin.advancedwish.enums.databases.status.CustomUrlStatus;
import twomillions.plugin.advancedwish.interfaces.DatabasesInterface;
import twomillions.plugin.advancedwish.utils.texts.QuickUtils;
import org.apache.commons.dbcp2.BasicDataSource;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * 该类实现 {@link DatabasesInterface}，处理 MySQL 操作。
 *
 * @author 2000000
 * @date 2023/3/26
 */
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MySQLManager implements DatabasesInterface {
    private volatile String jdbcUrlString;
    private volatile BasicDataSource dataSource = new BasicDataSource();

    private volatile String ip;
    private volatile String port;
    private volatile String username;
    private volatile String password;
    private volatile String driver;

    private volatile AuthStatus authStatus;
    private volatile CustomUrlStatus customUrlStatus;
    private volatile ConnectStatus connectStatus = ConnectStatus.TurnOff;

    @Getter private static final MySQLManager mySQLManager = new MySQLManager();

    /**
     * 根据指定的 YAML 配置，初始化数据库连接。
     *
     * @param yaml 包含数据库连接信息的 YAML 配置
     * @return 数据库连接状态
     */
    @Override
    public ConnectStatus setup(Yaml yaml) {
        // 设置MySQL登录信息
        setIp(yaml.getString("MYSQL.IP"));
        setPort(yaml.getString("MYSQL.PORT"));
        setUsername(yaml.getString("MYSQL.AUTH.USER"));
        setPassword(yaml.getString("MYSQL.AUTH.PASSWORD"));
        setDriver(yaml.getString("MYSQL.DRIVER"));

        // 检查是否使用自定义的MySQL连接URL
        setCustomUrlStatus(yaml.getString("MYSQL.CUSTOM-URL").isEmpty() ? CustomUrlStatus.TurnOff : CustomUrlStatus.TurnOn);

        // 根据是否使用自定义URL设置连接URL
        if (getCustomUrlStatus() == CustomUrlStatus.TurnOn) {
            setJdbcUrlString(yaml.getString("MYSQL.CUSTOM-URL"));
        } else {
            setJdbcUrlString("jdbc:mysql://" + getIp() + ":" + getPort() + "/advancedwish");
        }

        // 检查MySQL连接状态
        try {
            getDataSource().setDriverClassName(getDriver());
            getDataSource().setUrl(getJdbcUrlString());
            getDataSource().setUsername(getUsername());
            getDataSource().setPassword(getPassword());
            getDataSource().setInitialSize(5);

            QuickUtils.sendConsoleMessage("&a已成功建立与 &eMySQL&a 的连接!");

            setConnectStatus(ConnectStatus.Connected);
        } catch (Exception exception) {
            exception.printStackTrace();

            QuickUtils.sendConsoleMessage("&c您打开了 &eMySQL&c 数据库选项，但未能正确连接到 &eMySQL&c，请检查 &eMySQL&c 服务状态，即将关闭服务器!");

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
    @SneakyThrows
    public Object getOrDefault(String uuid, String key, Object defaultValue, String databaseCollection) {
        @Cleanup Connection connection = getDataSource().getConnection();
        
        checkCollectionType(key, databaseCollection);
        checkColumn(key, databaseCollection);

        String query = "SELECT COALESCE((SELECT `" + key + "` FROM `" + databaseCollection + "` WHERE uuid = ?), ?)";

        @Cleanup PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setString(1, uuid);
        preparedStatement.setObject(2, defaultValue);

        @Cleanup ResultSet resultSet = preparedStatement.executeQuery();
        resultSet.next();
        Object value = resultSet.getObject(1);

        if (value == null) {
            value = defaultValue;
            update(uuid, key, defaultValue, databaseCollection);
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
    @SneakyThrows
    public ConcurrentLinkedQueue<String> getOrDefaultList(String uuid, String key, ConcurrentLinkedQueue<String> defaultValue, String databaseCollection) {
        @Cleanup Connection connection = getDataSource().getConnection();

        checkCollectionType(key, databaseCollection);
        checkColumn(key, databaseCollection);

        String query = "SELECT COALESCE((SELECT `" + key + "` FROM `" + databaseCollection + "` WHERE uuid = ?), ?)";

        @Cleanup PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setString(1, uuid);
        preparedStatement.setString(2, String.join(",", defaultValue));

        @Cleanup ResultSet resultSet = preparedStatement.executeQuery();
        resultSet.next();
        String value = resultSet.getString(1);

        if (value == null || value.isEmpty()) {
            update(uuid, key, defaultValue, databaseCollection);
            return defaultValue;
        }

        return QuickUtils.stringToList(value);
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
    @SneakyThrows
    public void update(String uuid, String key, Object value, String databaseCollection) {
        @Cleanup Connection connection = getDataSource().getConnection();

        checkCollectionType(key, databaseCollection);
        checkColumn(key, databaseCollection);

        if (value instanceof Collection<?>) {
            value = QuickUtils.listToString((Collection<?>) value);
        }

        String query = "INSERT INTO `" + databaseCollection + "` (uuid, `" + key + "`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `" + key + "`=?";

        @Cleanup PreparedStatement preparedStatement = connection.prepareStatement(query);

        preparedStatement.setString(1, uuid);
        preparedStatement.setObject(2, value);
        preparedStatement.setObject(3, value);
        preparedStatement.executeUpdate();
    }

    /**
     * 获取数据库中的所有集合名称。
     *
     * @return 包含所有集合名称的字符串列表
     */
    @SneakyThrows
    public List<String> getAllDatabaseCollectionNames() {
        @Cleanup Connection connection = getDataSource().getConnection();
        @Cleanup ResultSet tables = connection.getMetaData().getTables(null, null, null, new String[] { "TABLE" });

        List<String> tableNames = new ArrayList<>();

        while (tables.next()) {
            String tableName = tables.getString("TABLE_NAME");
            tableNames.add(tableName);
        }

        return tableNames;
    }

    /**
     * 获取所有数据集合的所有数据。
     *
     * @param databaseCollection 查询的数据集合
     * @return 以 Map 的形式传递，Map Key 为数据集合名，value Map Key 为 UUID，value 是一个包含键值对的 Map
     */
    @Override
    @SneakyThrows
    public Map<String, Map<String, Object>> getAllData(String databaseCollection) {
        @Cleanup Connection connection = getDataSource().getConnection();
        @Cleanup ResultSet columns = connection.getMetaData().getColumns(null, null, databaseCollection, null);

        List<String> columnNames = new ArrayList<>();

        while (columns.next()) {
            columnNames.add(columns.getString("COLUMN_NAME"));
        }

        if (columnNames.isEmpty()) {
            return new HashMap<>();
        }

        String query = "SELECT uuid, `" + String.join("`, `", columnNames) + "` FROM `" + databaseCollection + "`";

        @Cleanup ResultSet resultSet = connection.prepareStatement(query).executeQuery();

        ResultSetMetaData metadata = resultSet.getMetaData();

        List<String> dataColumnNames = new ArrayList<>();
        for (int i = 2; i <= metadata.getColumnCount(); i++) {
            dataColumnNames.add(metadata.getColumnName(i));
        }

        Map<String, Map<String, Object>> data = new HashMap<>();

        while (resultSet.next()) {
            Map<String, Object> rowData = new HashMap<>();

            for (String columnName : dataColumnNames) {
                rowData.put(columnName, resultSet.getObject(columnName));
            }

            data.put(resultSet.getString("uuid"), rowData);
        }

        return data.entrySet().stream()
                .filter(entry -> !entry.getKey().equals("uuid"))
                .collect(Collectors.toMap(
                        Map.Entry::getKey, entry -> entry.getValue().entrySet().stream()
                                .filter(columnEntry -> columnEntry.getValue() != null)
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))));
    }

    /**
     * 执行 SQL 语句。
     *
     * @param sql 要执行的 SQL 语句。
     */
    @SneakyThrows
    public void executeStatement(String sql) {
        @Cleanup Statement statement = getDataSource().getConnection().createStatement();
        statement.executeUpdate(sql);
    }

    /**
     * 检查是否存在指定集合。
     * 如果不存在，则创建新集合，其中包含一个主键 uuid 和一个指定的列。
     *
     * @param key 查询的 Key
     * @param databaseCollection 查询的数据集合
     */
    @SneakyThrows
    private void checkCollectionType(String key, String databaseCollection) {
        @Cleanup Connection connection = getDataSource().getConnection();
        @Cleanup ResultSet tables = connection.getMetaData().getTables(null, null, databaseCollection, null);

        if (!tables.next()) {
            String createTableQuery = "CREATE TABLE `" + databaseCollection + "` (`uuid` VARCHAR(36) NOT NULL, `" + key + "` TEXT DEFAULT NULL, PRIMARY KEY (`uuid`))";
            @Cleanup PreparedStatement createTableStatement = connection.prepareStatement(createTableQuery);
            createTableStatement.executeUpdate();
        }
    }

    /**
     * 检查是否存在指定的列。如果不存在，则在表格中添加新列。
     *
     * @param key 查询的 Key
     * @param databaseCollection 查询的数据集合
     */
    @SneakyThrows
    private void checkColumn(String key, String databaseCollection) {
        @Cleanup Connection connection = getDataSource().getConnection();
        @Cleanup ResultSet columns = connection.getMetaData().getColumns(null, null, databaseCollection, key);

        if (!columns.next()) {
            String alterTableQuery = "ALTER TABLE `" + databaseCollection + "` ADD `" + key + "` TEXT DEFAULT NULL";
            @Cleanup PreparedStatement alterTableStatement = connection.prepareStatement(alterTableQuery);
            alterTableStatement.executeUpdate();
        }
    }
}
