package me.ninepin.mailBoxPlugin.manager;

import me.ninepin.mailBoxPlugin.model.MailItem;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface IDataManager {
    Map<UUID, List<MailItem>> loadAllMailboxes();
    List<MailItem> loadPlayerMailbox(UUID playerUUID);
    void savePlayerMailbox(UUID playerUUID, List<MailItem> mailItems);
    void saveAllMailboxes(Map<UUID, List<MailItem>> playerMailboxes);
    void initialize();
    void close();
}