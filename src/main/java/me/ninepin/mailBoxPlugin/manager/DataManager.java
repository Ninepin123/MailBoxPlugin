package me.ninepin.mailBoxPlugin.manager;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import me.ninepin.mailBoxPlugin.model.MailItem;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 数据管理类
 * 负责邮件数据的保存和加载
 */
public class DataManager {

    private final JavaPlugin plugin;
    private final File dataFolder;

    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public DataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "mailboxes");

        // 创建数据文件夹
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    /**
     * 加载所有玩家的信箱数据
     * @return 包含所有玩家信箱数据的Map
     */
    public Map<UUID, List<MailItem>> loadAllMailboxes() {
        Map<UUID, List<MailItem>> playerMailboxes = new HashMap<>();

        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                String uuidStr = fileName.substring(0, fileName.length() - 4); // 移除 .yml
                try {
                    UUID playerUUID = UUID.fromString(uuidStr);
                    List<MailItem> mailItems = loadPlayerMailbox(playerUUID);
                    playerMailboxes.put(playerUUID, mailItems);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("无效的UUID文件名: " + fileName);
                }
            }
        }

        return playerMailboxes;
    }

    /**
     * 加载指定玩家的信箱数据
     * @param playerUUID 玩家UUID
     * @return 玩家的邮件列表
     */
    public List<MailItem> loadPlayerMailbox(UUID playerUUID) {
        File playerFile = new File(dataFolder, playerUUID.toString() + ".yml");
        List<MailItem> mailItems = new ArrayList<>();

        if (playerFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            List<Map<?, ?>> mailList = config.getMapList("mails");

            for (Map<?, ?> mailMap : mailList) {
                try {
                    ItemStack item = (ItemStack) mailMap.get("item");
                    long timestamp = (Long) mailMap.get("timestamp");
                    boolean isRead = (Boolean) mailMap.get("isRead");

                    MailItem mailItem = new MailItem(item, timestamp, isRead);
                    mailItems.add(mailItem);
                } catch (Exception e) {
                    plugin.getLogger().warning("加载邮件数据时出错: " + e.getMessage());
                }
            }
        }

        return mailItems;
    }

    /**
     * 保存指定玩家的信箱数据
     * @param playerUUID 玩家UUID
     * @param mailItems 邮件列表
     */
    public void savePlayerMailbox(UUID playerUUID, List<MailItem> mailItems) {
        File playerFile = new File(dataFolder, playerUUID.toString() + ".yml");
        FileConfiguration config = new YamlConfiguration();
        List<Map<String, Object>> mailList = new ArrayList<>();

        if (mailItems != null) {
            for (MailItem mail : mailItems) {
                Map<String, Object> mailMap = new HashMap<>();
                mailMap.put("item", mail.getItem());
                mailMap.put("timestamp", mail.getTimestamp());
                mailMap.put("isRead", mail.isRead());
                mailList.add(mailMap);
            }
        }

        config.set("mails", mailList);

        try {
            config.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存玩家信箱数据: " + playerUUID);
            e.printStackTrace();
        }
    }

    /**
     * 保存所有玩家的信箱数据
     * @param playerMailboxes 所有玩家的信箱数据
     */
    public void saveAllMailboxes(Map<UUID, List<MailItem>> playerMailboxes) {
        for (Map.Entry<UUID, List<MailItem>> entry : playerMailboxes.entrySet()) {
            savePlayerMailbox(entry.getKey(), entry.getValue());
        }
    }
}