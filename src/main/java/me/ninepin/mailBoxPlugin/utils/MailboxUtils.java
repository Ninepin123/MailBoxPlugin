package me.ninepin.mailBoxPlugin.utils;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * 信箱系统工具类
 * 提供一些通用的工具方法
 */
public class MailboxUtils {

    /**
     * 检查背包是否有足够空间放置物品
     * @param inventory 背包
     * @param item 要放置的物品
     * @return 是否有足够空间
     */
    public static boolean hasInventorySpace(Inventory inventory, ItemStack item) {
        if (inventory.firstEmpty() != -1) return true;

        int amount = item.getAmount();
        ItemStack similar = item.clone();
        similar.setAmount(1);

        for (ItemStack stack : inventory.getContents()) {
            if (stack != null && stack.isSimilar(similar) && stack.getAmount() < stack.getMaxStackSize()) {
                int canAdd = stack.getMaxStackSize() - stack.getAmount();
                if (canAdd >= amount) return true;
                amount -= canAdd;
            }
        }

        return false;
    }

    /**
     * 获取物品的显示名称
     * @param item 物品
     * @return 显示名称
     */
    public static String getItemDisplayName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return item.getType().toString();
    }

    /**
     * 格式化时间戳
     * @param timestamp 时间戳
     * @return 格式化后的时间字符串
     */
    public static String formatTimestamp(long timestamp) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new java.util.Date(timestamp));
    }

    /**
     * 计算需要的GUI大小
     * @param itemCount 物品数量
     * @return GUI大小 (9的倍数，最大54)
     */
    public static int calculateGuiSize(int itemCount) {
        return Math.min(54, ((itemCount / 9) + 1) * 9);
    }
}