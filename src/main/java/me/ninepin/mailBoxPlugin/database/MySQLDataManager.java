package me.ninepin.mailBoxPlugin.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.ninepin.mailBoxPlugin.manager.IDataManager;
import me.ninepin.mailBoxPlugin.model.MailItem;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.*;
import java.util.*;

public class MySQLDataManager implements IDataManager {

    private final JavaPlugin plugin;
    private final String tablePrefix;
    private HikariDataSource dataSource;

    public MySQLDataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();
        this.tablePrefix = config.getString("mysql.table-prefix", "mailbox_");
    }

    @Override
    public void initialize() {
        FileConfiguration config = plugin.getConfig();

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:mysql://" +
                config.getString("mysql.host", "localhost") + ":" +
                config.getInt("mysql.port", 3306) + "/" +
                config.getString("mysql.database", "minecraft") +
                "?useSSL=" + config.getBoolean("mysql.use-ssl", false) +
                "&serverTimezone=UTC");

        hikariConfig.setUsername(config.getString("mysql.username", "root"));
        hikariConfig.setPassword(config.getString("mysql.password", "password"));

        hikariConfig.setMaximumPoolSize(config.getInt("mysql.connection-pool.maximum-pool-size", 10));
        hikariConfig.setMinimumIdle(config.getInt("mysql.connection-pool.minimum-idle", 2));
        hikariConfig.setConnectionTimeout(config.getLong("mysql.connection-pool.connection-timeout", 30000));
        hikariConfig.setIdleTimeout(config.getLong("mysql.connection-pool.idle-timeout", 600000));
        hikariConfig.setMaxLifetime(config.getLong("mysql.connection-pool.max-lifetime", 1800000));

        dataSource = new HikariDataSource(hikariConfig);
        createTables();
    }

    private void createTables() {
        String sql = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "mails (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "player_uuid VARCHAR(36) NOT NULL," +
                "item_data LONGBLOB NOT NULL," +
                "timestamp BIGINT NOT NULL," +
                "is_read BOOLEAN NOT NULL DEFAULT FALSE," +
                "INDEX idx_player_uuid (player_uuid)" +
                ") ENGINE=InnoDB";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            plugin.getLogger().severe("無法創建資料表: " + e.getMessage());
        }
    }

    @Override
    public Map<UUID, List<MailItem>> loadAllMailboxes() {
        Map<UUID, List<MailItem>> playerMailboxes = new HashMap<>();

        String sql = "SELECT DISTINCT player_uuid FROM " + tablePrefix + "mails";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                UUID playerUUID = UUID.fromString(rs.getString("player_uuid"));
                List<MailItem> mailItems = loadPlayerMailbox(playerUUID);
                playerMailboxes.put(playerUUID, mailItems);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("加載所有信箱資料時出錯: " + e.getMessage());
        }

        return playerMailboxes;
    }

    @Override
    public List<MailItem> loadPlayerMailbox(UUID playerUUID) {
        List<MailItem> mailItems = new ArrayList<>();

        String sql = "SELECT item_data, timestamp, is_read FROM " + tablePrefix +
                "mails WHERE player_uuid = ? ORDER BY timestamp DESC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, playerUUID.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    byte[] itemData = rs.getBytes("item_data");
                    long timestamp = rs.getLong("timestamp");
                    boolean isRead = rs.getBoolean("is_read");

                    ItemStack item = deserializeItemStack(itemData);
                    if (item != null) {
                        mailItems.add(new MailItem(item, timestamp, isRead));
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("加載玩家信箱資料時出錯: " + playerUUID + " - " + e.getMessage());
        }

        return mailItems;
    }

    @Override
    public void savePlayerMailbox(UUID playerUUID, List<MailItem> mailItems) {
        // 刪除舊資料
        String deleteSql = "DELETE FROM " + tablePrefix + "mails WHERE player_uuid = ?";

        // 插入新資料
        String insertSql = "INSERT INTO " + tablePrefix +
                "mails (player_uuid, item_data, timestamp, is_read) VALUES (?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                deleteStmt.setString(1, playerUUID.toString());
                deleteStmt.executeUpdate();
            }

            if (mailItems != null && !mailItems.isEmpty()) {
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    for (MailItem mail : mailItems) {
                        byte[] itemData = serializeItemStack(mail.getItem());
                        if (itemData != null) {
                            insertStmt.setString(1, playerUUID.toString());
                            insertStmt.setBytes(2, itemData);
                            insertStmt.setLong(3, mail.getTimestamp());
                            insertStmt.setBoolean(4, mail.isRead());
                            insertStmt.addBatch();
                        }
                    }
                    insertStmt.executeBatch();
                }
            }

            conn.commit();
        } catch (SQLException e) {
            plugin.getLogger().severe("保存玩家信箱資料時出錯: " + playerUUID + " - " + e.getMessage());
        }
    }

    @Override
    public void saveAllMailboxes(Map<UUID, List<MailItem>> playerMailboxes) {
        for (Map.Entry<UUID, List<MailItem>> entry : playerMailboxes.entrySet()) {
            savePlayerMailbox(entry.getKey(), entry.getValue());
        }
    }

    private byte[] serializeItemStack(ItemStack item) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {

            dataOutput.writeObject(item);
            return outputStream.toByteArray();
        } catch (Exception e) {
            plugin.getLogger().warning("序列化物品時出錯: " + e.getMessage());
            return null;
        }
    }

    private ItemStack deserializeItemStack(byte[] data) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {

            return (ItemStack) dataInput.readObject();
        } catch (Exception e) {
            plugin.getLogger().warning("反序列化物品時出錯: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}