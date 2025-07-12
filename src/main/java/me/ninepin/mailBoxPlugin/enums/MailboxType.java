package me.ninepin.mailBoxPlugin.enums;

/**
 * 信箱GUI类型枚举
 * 定义不同类型的信箱界面
 */
public enum MailboxType {
    /** 玩家个人信箱 */
    PLAYER_MAILBOX,

    /** 管理员发送物品给所有玩家 */
    ADMIN_SEND_ALL,

    /** 管理员发送物品给指定玩家 */
    ADMIN_SEND_PLAYER,

    /** 管理员查看玩家信箱 */
    ADMIN_CHECK_MAILBOX
}