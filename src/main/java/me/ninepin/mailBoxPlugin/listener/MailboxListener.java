package me.ninepin.mailBoxPlugin.listener;

import me.ninepin.mailBoxPlugin.enums.MailboxType;
import me.ninepin.mailBoxPlugin.manager.MailboxManager;
import me.ninepin.mailBoxPlugin.model.MailItem;
import me.ninepin.mailBoxPlugin.utils.MailboxUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.UUID;

/**
 * 信箱系统事件监听器
 * 处理玩家加入、GUI点击和关闭事件
 */
public class MailboxListener implements Listener {

    private final MailboxManager mailboxManager;

    /**
     * 构造函数
     *
     * @param mailboxManager 信箱管理器
     */
    public MailboxListener(MailboxManager mailboxManager) {
        this.mailboxManager = mailboxManager;
    }

    /**
     * 当玩家登入时载入其信箱数据并通知未读邮件
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        mailboxManager.loadPlayerMailbox(playerUUID);

        // 通知玩家未读邮件数量
        int unreadCount = mailboxManager.getUnreadMailCount(playerUUID);
        if (unreadCount > 0) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    event.getPlayer().sendMessage(ChatColor.GOLD + "[信箱系統] " + ChatColor.YELLOW +
                            "您有 " + unreadCount + " 封未讀郵件! 使用 /mail box 查看您的信箱。"
                    );
                }
            }.runTaskLater(mailboxManager.getPlugin(), 40L); // 延迟2秒通知玩家
        }
    }

    /**
     * 处理GUI点击事件
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        UUID playerUUID = player.getUniqueId();

        // 检查是否为我们的GUI
        MailboxType type = mailboxManager.getOpenInventories().get(playerUUID);
        if (type == null) return;

        switch (type) {
            case PLAYER_MAILBOX:
                handlePlayerMailboxClick(event, player, playerUUID);
                break;
            case ADMIN_SEND_ALL:
            case ADMIN_SEND_PLAYER:
                // 管理员GUI允许放入物品，不取消事件
                break;
            case ADMIN_CHECK_MAILBOX:
                handleAdminCheckMailboxClick(event, player, playerUUID);
                break;
            default:
                break;
        }
    }

    /**
     * 处理玩家信箱GUI点击
     */
    private void handlePlayerMailboxClick(InventoryClickEvent event, Player player, UUID playerUUID) {
        if (event.getView().getTitle().contains("您的信箱")) {
            event.setCancelled(true);

            if (event.getRawSlot() >= 0 && event.getRawSlot() < event.getInventory().getSize()) {
                List<MailItem> mails = mailboxManager.getPlayerMailboxes().get(playerUUID);
                if (mails != null && event.getRawSlot() < mails.size()) {
                    // 左键点击领取物品
                    if (event.isLeftClick()) {
                        MailItem mail = mails.get(event.getRawSlot());
                        ItemStack item = mail.getItem();

                        // 检查背包是否有空间
                        if (MailboxUtils.hasInventorySpace(player.getInventory(), item)) {
                            player.getInventory().addItem(item);
                            mails.remove(event.getRawSlot());
                            player.sendMessage(ChatColor.GOLD + "[信箱系统] " + ChatColor.GREEN + "成功領取物品!");

                            // 保存玩家信箱数据
                            mailboxManager.getDataManager().savePlayerMailbox(playerUUID, mails);

                            // 重新整理信箱界面而不是关闭
                            Bukkit.getScheduler().runTaskLater(mailboxManager.getPlugin(), () -> {
                                mailboxManager.openMailboxGUI(player);
                            }, 1L);
                        } else {
                            player.sendMessage(ChatColor.GOLD + "[信箱系统] " + ChatColor.RED + "您的背包已滿，無法領取物品!");
                        }
                    }
                }
            }
        }
    }

    /**
     * 处理管理员查看信箱GUI点击
     */
    private void handleAdminCheckMailboxClick(InventoryClickEvent event, Player player, UUID playerUUID) {
        event.setCancelled(true);

        // 检查是否为管理员权限
        if (player.hasPermission("mailbox.admin")) {
            int slot = event.getRawSlot();
            UUID targetUUID = mailboxManager.getTargetPlayers().get(playerUUID);

            if (targetUUID != null && slot >= 0 && slot < event.getInventory().getSize()) {
                List<MailItem> mails = mailboxManager.getPlayerMailboxes().get(targetUUID);
                if (mails != null && slot < mails.size()) {
                    if (event.isShiftClick() && event.isRightClick()) {
                        // Shift+右键 - 删除物品
                        ItemStack removedItem = mails.get(slot).getItem();
                        String itemName = MailboxUtils.getItemDisplayName(removedItem);

                        // 从列表中移除物品
                        mails.remove(slot);

                        // 保存玩家信箱数据
                        mailboxManager.getDataManager().savePlayerMailbox(targetUUID, mails);

                        // 通知管理员
                        String targetName = Bukkit.getOfflinePlayer(targetUUID).getName();
                        player.sendMessage(ChatColor.GOLD + "[信箱系统] " + ChatColor.GREEN +
                                "成功從 " + targetName + " 的信箱中刪除物品: " + itemName);

                        // 记录到服务器日志
                        mailboxManager.getPlugin().getLogger().info("管理员 " + player.getName() + " 从玩家 " + targetName + " 的信箱中删除了物品: " + itemName);

                        // 重新整理GUI
                        Bukkit.getScheduler().runTaskLater(mailboxManager.getPlugin(), () -> {
                            mailboxManager.openAdminCheckMailboxGUI(player, targetUUID, targetName);
                        }, 1L);
                    } else if (event.isLeftClick()) {
                        // 左键 - 将物品给予自己
                        ItemStack item = mails.get(slot).getItem().clone();

                        // 检查管理员背包是否有空间
                        if (MailboxUtils.hasInventorySpace(player.getInventory(), item)) {
                            player.getInventory().addItem(item);

                            String targetName = Bukkit.getOfflinePlayer(targetUUID).getName();
                            player.sendMessage(ChatColor.GOLD + "[信箱系统] " + ChatColor.GREEN +
                                    "成功從 " + targetName + " 的信箱中取出物品 (不會從信箱中移除)");
                        } else {
                            player.sendMessage(ChatColor.GOLD + "[信箱系统] " + ChatColor.RED +
                                    "您的背包已滿，無法取出物品!");
                        }
                    }
                }
            }
        }
    }

    /**
     * 处理GUI关闭事件
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        MailboxType type = mailboxManager.getOpenInventories().get(playerUUID);
        if (type == null) return;

        switch (type) {
            case ADMIN_SEND_ALL:
                handleAdminSendAllClose(event, player);
                break;
            case ADMIN_SEND_PLAYER:
                handleAdminSendPlayerClose(event, player, playerUUID);
                break;
            default:
                break;
        }

        mailboxManager.getOpenInventories().remove(playerUUID);
    }

    /**
     * 处理管理员发送给所有玩家GUI关闭
     */
    private void handleAdminSendAllClose(InventoryCloseEvent event, Player player) {
        boolean itemsSent = false;
        for (ItemStack item : event.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                itemsSent = true;
                // 发送给所有在线玩家
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    mailboxManager.addMailToPlayer(onlinePlayer.getUniqueId(), item.clone());
                }

                // 给离线玩家也发送物品
                for (UUID uuid : mailboxManager.getPlayerMailboxes().keySet()) {
                    if (Bukkit.getPlayer(uuid) == null) {
                        mailboxManager.addMailToPlayer(uuid, item.clone());
                    }
                }
            }
        }

        if (itemsSent) {
            player.sendMessage(ChatColor.GOLD + "[信箱系统] " + ChatColor.GREEN + "成功發送物品給所有玩家!");
            mailboxManager.getPlugin().getLogger().info("管理员 " + player.getName() + " 向所有玩家发送了物品");
        }
    }

    /**
     * 处理管理员发送给指定玩家GUI关闭
     */
    private void handleAdminSendPlayerClose(InventoryCloseEvent event, Player player, UUID playerUUID) {
        UUID targetUUID = mailboxManager.getTargetPlayers().get(playerUUID);
        if (targetUUID != null) {
            boolean sentToTarget = false;
            String targetName = Bukkit.getOfflinePlayer(targetUUID).getName();

            for (ItemStack item : event.getInventory().getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    sentToTarget = true;
                    // 修改后：总是发送到信箱，不考虑玩家是否在线或背包空间
                    mailboxManager.addMailToPlayer(targetUUID, item.clone());

                    // 如果玩家在线，额外发送通知
                    Player targetPlayer = Bukkit.getPlayer(targetUUID);
                    if (targetPlayer != null && targetPlayer.isOnline()) {
                        targetPlayer.sendMessage(ChatColor.GOLD + "[信箱系统] " + ChatColor.GREEN +
                                "您收到了管理員發送的物品! 請使用 /mail box 查看您的信箱。");
                    }

                    // 记录到服务器日志
                    String itemName = MailboxUtils.getItemDisplayName(item);
                    mailboxManager.getPlugin().getLogger().info("管理员 " + player.getName() +
                            " 向玩家 " + targetName + " 发送了物品: " + itemName);
                }
            }

            if (sentToTarget) {
                player.sendMessage(ChatColor.GOLD + "[信箱系统] " + ChatColor.GREEN +
                        "成功發送物品給玩家 " + targetName + "!");
            }

            mailboxManager.getTargetPlayers().remove(playerUUID);
        }
    }
}