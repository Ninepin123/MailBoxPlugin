package me.ninepin.mailBoxPlugin.expansion;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.ninepin.mailBoxPlugin.MailboxPlugin;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class MailboxExpansion extends PlaceholderExpansion {

    private final MailboxPlugin plugin;

    public MailboxExpansion(MailboxPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        // 這是您佔位符的前綴，例如 %mailbox_unread_count%
        return "mailbox";
    }

    @Override
    public @NotNull String getAuthor() {
        return "NinePin"; // 請替換成您的名字
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true; // 讓 PlaceholderAPI 保持這個擴展的載入狀態
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        // 處理 %mailbox_unread_count%
        if (params.equalsIgnoreCase("unread_count")) {
            // 直接調用 MailboxManager 中的現有方法
            int count = plugin.getMailboxManager().getUnreadMailCount(player.getUniqueId());
            return String.valueOf(count);
        }

        return null; // 表示這個佔位符無法被解析
    }
}