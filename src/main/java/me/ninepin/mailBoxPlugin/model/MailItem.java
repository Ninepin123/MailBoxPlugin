package me.ninepin.mailBoxPlugin.model;

import org.bukkit.inventory.ItemStack;

/**
 * 邮件项目实体类
 * 封装邮件的基本信息
 */
public class MailItem {
    private ItemStack item;
    private long timestamp;
    private boolean isRead;

    /**
     * 构造函数
     * @param item 物品
     * @param timestamp 时间戳
     * @param isRead 是否已读
     */
    public MailItem(ItemStack item, long timestamp, boolean isRead) {
        this.item = item;
        this.timestamp = timestamp;
        this.isRead = isRead;
    }

    /**
     * 获取物品
     * @return ItemStack物品
     */
    public ItemStack getItem() {
        return item;
    }

    /**
     * 获取时间戳
     * @return 时间戳
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * 检查是否已读
     * @return 是否已读
     */
    public boolean isRead() {
        return isRead;
    }

    /**
     * 设置已读状态
     * @param read 已读状态
     */
    public void setRead(boolean read) {
        isRead = read;
    }
}