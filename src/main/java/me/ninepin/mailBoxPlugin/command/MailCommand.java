package me.ninepin.mailBoxPlugin.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import me.ninepin.mailBoxPlugin.manager.MailboxManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 邮件命令处理器
 * 负责处理所有与邮件相关的命令
 */
public class MailCommand implements CommandExecutor, TabCompleter {

    private final MailboxManager mailboxManager;

    /**
     * 构造函数
     * @param mailboxManager 信箱管理器
     */
    public MailCommand(MailboxManager mailboxManager) {
        this.mailboxManager = mailboxManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player) && !(args.length > 0 && args[0].equalsIgnoreCase("check"))) {
            sender.sendMessage(ChatColor.RED + "此命令只能由玩家使用!");
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "box":
                return handleBoxCommand(sender);
            case "all":
                return handleAllCommand(sender);
            case "give":
                return handleGiveCommand(sender, args);
            case "check":
                return handleCheckCommand(sender, args);
            case "help":
                sendHelpMessage(sender);
                return true;
            default:
                sender.sendMessage(ChatColor.RED + "未知的命令。使用 /mail help 获取帮助。");
                return true;
        }
    }

    /**
     * 处理 /mail box 命令
     */
    private boolean handleBoxCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "此命令只能由玩家使用!");
            return true;
        }

        Player player = (Player) sender;
        mailboxManager.openMailboxGUI(player);
        return true;
    }

    /**
     * 处理 /mail all 命令
     */
    private boolean handleAllCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "此命令只能由玩家使用!");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("mailbox.admin")) {
            player.sendMessage(ChatColor.RED + "您没有权限执行此命令!");
            return true;
        }

        mailboxManager.openAdminSendAllGUI(player);
        return true;
    }

    /**
     * 处理 /mail give <玩家> 命令
     */
    private boolean handleGiveCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "此命令只能由玩家使用!");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("mailbox.admin")) {
            player.sendMessage(ChatColor.RED + "您没有权限执行此命令!");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /mail give <玩家名称>");
            return true;
        }

        String targetName = args[1];
        UUID targetUUID = findPlayerUUID(targetName);

        if (targetUUID != null) {
            mailboxManager.openAdminSendPlayerGUI(player, targetUUID);
        } else {
            player.sendMessage(ChatColor.RED + "找不到指定的玩家: " + targetName);
        }

        return true;
    }

    /**
     * 处理 /mail check <玩家> 命令
     */
    private boolean handleCheckCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mailbox.admin") && !sender.hasPermission("mailbox.check")) {
            sender.sendMessage(ChatColor.RED + "您没有权限执行此命令!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /mail check <玩家名称>");
            return true;
        }

        String targetName = args[1];
        UUID targetUUID = findPlayerUUID(targetName);

        if (targetUUID == null) {
            sender.sendMessage(ChatColor.RED + "找不到指定的玩家: " + targetName);
            return true;
        }

        if (sender instanceof Player) {
            // 如果是玩家执行，打开GUI查看
            mailboxManager.openAdminCheckMailboxGUI((Player) sender, targetUUID, targetName);
        } else {
            // 如果是控制台执行，显示文本信息
            mailboxManager.displayMailboxContents(sender, targetUUID, targetName);
        }

        return true;
    }

    /**
     * 查找玩家UUID
     * @param playerName 玩家名称
     * @return 玩家UUID，如果找不到则返回null
     */
    private UUID findPlayerUUID(String playerName) {
        // 首先尝试在线玩家
        Player onlinePlayer = Bukkit.getPlayer(playerName);
        if (onlinePlayer != null) {
            return onlinePlayer.getUniqueId();
        }

        // 然后检查离线玩家
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            String name = offlinePlayer.getName();
            if (name != null && name.equalsIgnoreCase(playerName)) {
                return offlinePlayer.getUniqueId();
            }
        }

        return null;
    }

    /**
     * 发送帮助信息
     * @param sender 命令发送者
     */
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "===== 信箱系统帮助 =====");

        if (sender instanceof Player) {
            Player player = (Player) sender;
            sender.sendMessage(ChatColor.YELLOW + "/mail box" + ChatColor.WHITE + " - 打开您的信箱");

            if (player.hasPermission("mailbox.admin") || player.hasPermission("mailbox.check")) {
                sender.sendMessage(ChatColor.YELLOW + "/mail check <玩家名称>" + ChatColor.WHITE + " - 查看指定玩家的信箱内容");
            }

            if (player.hasPermission("mailbox.admin")) {
                sender.sendMessage(ChatColor.YELLOW + "/mail all" + ChatColor.WHITE + " - 发送物品给所有玩家");
                sender.sendMessage(ChatColor.YELLOW + "/mail give <玩家名称>" + ChatColor.WHITE + " - 发送物品给指定玩家");
            }
        } else {
            // 控制台命令
            sender.sendMessage(ChatColor.YELLOW + "/mail check <玩家名称>" + ChatColor.WHITE + " - 查看指定玩家的信箱内容");
        }

        sender.sendMessage(ChatColor.YELLOW + "/mail help" + ChatColor.WHITE + " - 显示此帮助信息");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>(Arrays.asList("box", "help"));

            if (sender.hasPermission("mailbox.check") || sender.hasPermission("mailbox.admin")) {
                subCommands.add("check");
            }

            if (sender.hasPermission("mailbox.admin")) {
                subCommands.add("all");
                subCommands.add("give");
            }

            for (String subCommand : subCommands) {
                if (subCommand.startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            if ((args[0].equalsIgnoreCase("give") && sender.hasPermission("mailbox.admin")) ||
                    (args[0].equalsIgnoreCase("check") && (sender.hasPermission("mailbox.admin") || sender.hasPermission("mailbox.check")))) {

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(player.getName());
                    }
                }
            }
        }

        return completions;
    }
}