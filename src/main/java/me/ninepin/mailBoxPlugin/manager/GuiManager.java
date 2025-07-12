package me.ninepin.mailBoxPlugin.manager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import me.ninepin.mailBoxPlugin.model.MailItem;
import me.ninepin.mailBoxPlugin.enums.MailboxType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * GUI管理器
 * 负责所有GUI界面的创建和管理
 */
public class GuiManager {

    private final MailboxManager mailboxManager;
    private final SimpleDateFormat dateFormat;

    /**
     * 构造函数
     * @param mailboxManager 信箱管理器
     * @param dateFormat 日期格式化器
     */
    public GuiManager(MailboxManager mailboxManager, SimpleDateFormat dateFormat) {
        this.mailboxManager = mailboxManager;
        this.dateFormat = dateFormat;
    }

    /**
     * 打开玩家信箱GUI
     * @param player 玩家
     */
    public void openMailboxGUI(Player player) {
        UUID playerUUID = player.getUniqueId();
        List<MailItem> mails = mailboxManager.getPlayerMailboxes().getOrDefault(playerUUID, new ArrayList<>());

        int size = Math.min(54, ((mails.size() / 9) + 1) * 9);
        Inventory inv = Bukkit.createInventory(null, size, ChatColor.GOLD + "您的信箱");

        for (int i = 0; i < mails.size() && i < 54; i++) {
            MailItem mail = mails.get(i);
            ItemStack itemDisplay = mail.getItem().clone();
            ItemMeta meta = itemDisplay.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                lore.add("");
                lore.add(ChatColor.GRAY + "收到时间: " + ChatColor.WHITE + dateFormat.format(new Date(mail.getTimestamp())));
                lore.add(ChatColor.YELLOW + "左键点击领取");
                meta.setLore(lore);
                itemDisplay.setItemMeta(meta);
            }
            inv.setItem(i, itemDisplay);
        }

        player.openInventory(inv);
        mailboxManager.getOpenInventories().put(playerUUID, MailboxType.PLAYER_MAILBOX);
    }

    /**
     * 打开管理员发送物品GUI (给所有玩家)
     * @param admin 管理员
     */
    public void openAdminSendAllGUI(Player admin) {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.RED + "发送物品给所有玩家");
        admin.openInventory(inv);
        mailboxManager.getOpenInventories().put(admin.getUniqueId(), MailboxType.ADMIN_SEND_ALL);
    }

    /**
     * 打开管理员发送物品GUI (给指定玩家)
     * @param admin 管理员
     * @param targetPlayerUUID 目标玩家UUID
     */
    public void openAdminSendPlayerGUI(Player admin, UUID targetPlayerUUID) {
        Player targetPlayer = Bukkit.getPlayer(targetPlayerUUID);
        String targetName = targetPlayer != null ? targetPlayer.getName() : "未知玩家";

        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.RED + "发送物品给 " + targetName);
        admin.openInventory(inv);
        mailboxManager.getOpenInventories().put(admin.getUniqueId(), MailboxType.ADMIN_SEND_PLAYER);
        mailboxManager.getTargetPlayers().put(admin.getUniqueId(), targetPlayerUUID);
    }

    /**
     * 打开管理员查看玩家信箱GUI
     * @param admin 管理员
     * @param targetUUID 目标玩家UUID
     * @param targetName 目标玩家名称
     */
    public void openAdminCheckMailboxGUI(Player admin, UUID targetUUID, String targetName) {
        // 确保玩家信箱数据已加载
        mailboxManager.loadPlayerMailbox(targetUUID);

        List<MailItem> mails = mailboxManager.getPlayerMailboxes().getOrDefault(targetUUID, new ArrayList<>());

        int size = Math.min(54, ((mails.size() / 9) + 1) * 9);

        // 根据权限显示不同的标题和功能
        boolean isManager = admin.hasPermission("mailbox.admin");
        String title = isManager
                ? ChatColor.RED + targetName + "的信箱 (管理员模式)"
                : ChatColor.RED + targetName + "的信箱 (只读)";

        Inventory inv = Bukkit.createInventory(null, size, title);

        for (int i = 0; i < mails.size() && i < 54; i++) {
            MailItem mail = mails.get(i);
            ItemStack itemDisplay = mail.getItem().clone();
            ItemMeta meta = itemDisplay.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                lore.add("");
                lore.add(ChatColor.GRAY + "收到时间: " + ChatColor.WHITE + dateFormat.format(new Date(mail.getTimestamp())));
                if (isManager) {
                    lore.add(ChatColor.YELLOW + "左键点击领取物品的复制品");
                    lore.add(ChatColor.RED + "Shift+右键点击删除此物品");
                }
                meta.setLore(lore);
                itemDisplay.setItemMeta(meta);
            }
            inv.setItem(i, itemDisplay);
        }

        admin.openInventory(inv);
        mailboxManager.getOpenInventories().put(admin.getUniqueId(), MailboxType.ADMIN_CHECK_MAILBOX);
        // 储存当前正在查看的目标玩家
        mailboxManager.getTargetPlayers().put(admin.getUniqueId(), targetUUID);
    }
}