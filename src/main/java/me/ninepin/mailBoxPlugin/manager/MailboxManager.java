package me.ninepin.mailBoxPlugin.manager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import me.ninepin.mailBoxPlugin.model.MailItem;
import me.ninepin.mailBoxPlugin.enums.MailboxType;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 信箱管理器
 * 负责信箱的核心业务逻辑
 */
public class MailboxManager {

    private final JavaPlugin plugin;
    private final DataManager dataManager;
    private final GuiManager guiManager;

    private Map<UUID, List<MailItem>> playerMailboxes = new HashMap<>();
    private Map<UUID, MailboxType> openInventories = new HashMap<>();
    private Map<UUID, UUID> targetPlayers = new HashMap<>();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public MailboxManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataManager = new DataManager(plugin);
        this.guiManager = new GuiManager(this, dateFormat);

        // 加载所有玩家的信箱数据
        this.playerMailboxes = dataManager.loadAllMailboxes();
    }

    /**
     * 获取未读邮件数量
     * @param playerUUID 玩家UUID
     * @return 未读邮件数量
     */
    public int getUnreadMailCount(UUID playerUUID) {
        List<MailItem> mails = playerMailboxes.getOrDefault(playerUUID, new ArrayList<>());
        int count = 0;
        for (MailItem mail : mails) {
            if (!mail.isRead()) {
                count++;
            }
        }
        return count;
    }

    /**
     * 添加邮件到玩家信箱
     * @param playerUUID 玩家UUID
     * @param item 物品
     */
    public void addMailToPlayer(UUID playerUUID, ItemStack item) {
        List<MailItem> mails = playerMailboxes.getOrDefault(playerUUID, new ArrayList<>());
        MailItem mailItem = new MailItem(item, System.currentTimeMillis(), false);
        mails.add(mailItem);
        playerMailboxes.put(playerUUID, mails);

        // 如果玩家在线，发送通知
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null && player.isOnline()) {
            player.sendMessage(ChatColor.GOLD + "[信箱系统] " + ChatColor.YELLOW + "您收到了一封新邮件! 使用 /mail box 查看。");
        }

        // 保存玩家信箱数据
        dataManager.savePlayerMailbox(playerUUID, mails);
    }

    /**
     * 处理来自命令或其他插件的物品发送
     * @param targetPlayer 目标玩家
     * @param item 物品
     */
    public void handleItemFromCommand(Player targetPlayer, ItemStack item) {
        addMailToPlayer(targetPlayer.getUniqueId(), item);

        if (targetPlayer.isOnline()) {
            targetPlayer.sendMessage(ChatColor.GOLD + "[信箱系统] " + ChatColor.YELLOW +
                    "您收到了一份新物品! 使用 /mail box 查看。");
        }
    }

    /**
     * 处理物品无法放入背包时自动放入信箱
     * @param player 玩家
     * @param item 物品
     */
    public void handleItemToMailbox(Player player, ItemStack item) {
        if (item != null && item.getType() != org.bukkit.Material.AIR) {
            addMailToPlayer(player.getUniqueId(), item);
            player.sendMessage(ChatColor.GOLD + "[信箱系统] " + ChatColor.YELLOW +
                    "您的背包已满，物品已自动存入信箱! 使用 /mail box 查看。");
        }
    }

    /**
     * 加载玩家信箱数据
     * @param playerUUID 玩家UUID
     */
    public void loadPlayerMailbox(UUID playerUUID) {
        if (!playerMailboxes.containsKey(playerUUID)) {
            List<MailItem> mailItems = dataManager.loadPlayerMailbox(playerUUID);
            playerMailboxes.put(playerUUID, mailItems);
        }
    }

    /**
     * 保存所有玩家的信箱数据
     */
    public void saveAllMailboxes() {
        dataManager.saveAllMailboxes(playerMailboxes);
    }

    /**
     * 在控制台显示玩家信箱内容
     * @param sender 命令发送者
     * @param targetUUID 目标玩家UUID
     * @param targetName 目标玩家名称
     */
    public void displayMailboxContents(CommandSender sender, UUID targetUUID, String targetName) {
        loadPlayerMailbox(targetUUID);
        List<MailItem> mails = playerMailboxes.getOrDefault(targetUUID, new ArrayList<>());

        sender.sendMessage(ChatColor.GOLD + "===== " + targetName + "的信箱内容 =====");

        if (mails.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "信箱为空");
        } else {
            for (int i = 0; i < mails.size(); i++) {
                MailItem mail = mails.get(i);
                ItemStack item = mail.getItem();
                String itemName = (item.hasItemMeta() && item.getItemMeta().hasDisplayName())
                        ? item.getItemMeta().getDisplayName()
                        : item.getType().toString();

                sender.sendMessage(ChatColor.YELLOW.toString() + (i + 1) + ". " + ChatColor.WHITE +
                        itemName + " x" + item.getAmount() + ChatColor.GRAY +
                        " (收到时间: " + dateFormat.format(new Date(mail.getTimestamp())) + ")");
            }
        }
    }

    // GUI相关方法委托给GuiManager
    public void openMailboxGUI(Player player) {
        guiManager.openMailboxGUI(player);
    }

    public void openAdminSendAllGUI(Player admin) {
        guiManager.openAdminSendAllGUI(admin);
    }

    public void openAdminSendPlayerGUI(Player admin, UUID targetPlayerUUID) {
        guiManager.openAdminSendPlayerGUI(admin, targetPlayerUUID);
    }

    public void openAdminCheckMailboxGUI(Player admin, UUID targetUUID, String targetName) {
        guiManager.openAdminCheckMailboxGUI(admin, targetUUID, targetName);
    }

    // Getter方法
    public Map<UUID, List<MailItem>> getPlayerMailboxes() {
        return playerMailboxes;
    }

    public Map<UUID, MailboxType> getOpenInventories() {
        return openInventories;
    }

    public Map<UUID, UUID> getTargetPlayers() {
        return targetPlayers;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }
}