package me.ninepin.mailBoxPlugin;

import me.ninepin.mailBoxPlugin.command.MailCommand;
import me.ninepin.mailBoxPlugin.expansion.MailboxExpansion;
import me.ninepin.mailBoxPlugin.listener.MailboxListener;
import me.ninepin.mailBoxPlugin.manager.MailboxManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * 信箱系统主插件类
 * 负责插件的启用、停用和基本配置
 */
public class MailboxPlugin extends JavaPlugin {

    private MailboxManager mailboxManager;

    @Override
    public void onEnable() {
        // 初始化管理器
        saveDefaultConfig();
        mailboxManager = new MailboxManager(this);

        // 注册事件监听器
        getServer().getPluginManager().registerEvents(new MailboxListener(mailboxManager), this);

        // 注册命令
        getCommand("mail").setExecutor(new MailCommand(mailboxManager));
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new MailboxExpansion(this).register();
            getLogger().info("成功掛勾到 PlaceholderAPI!");
        }
        // 设置自动保存定时任务 (每5分钟保存一次)
        new BukkitRunnable() {
            @Override
            public void run() {
                mailboxManager.saveAllMailboxes();
                getLogger().info("自动保存所有玩家信箱数据完成");
            }
        }.runTaskTimerAsynchronously(this, 6000L, 6000L); // 5分钟 = 6000 ticks

        getLogger().info("信箱系统插件已启用!");
    }

    @Override
    public void onDisable() {
        // 保存所有玩家的信箱数据
        if (mailboxManager != null) {
            mailboxManager.saveAllMailboxes();
            mailboxManager.getDataManager().close();
        }
        getLogger().info("信箱系統插件已停用!");
    }

    /**
     * 获取信箱管理器
     * @return MailboxManager实例
     */
    public MailboxManager getMailboxManager() {
        return mailboxManager;
    }
}