package com.hibiscusmc.hmccosmetics.listener;

import com.hibiscusmc.hmccosmetics.HMCCosmeticsPlugin;
import com.hibiscusmc.hmccosmetics.config.DatabaseSettings;
import com.hibiscusmc.hmccosmetics.database.Database;
import com.hibiscusmc.hmccosmetics.user.CosmeticUser;
import com.hibiscusmc.hmccosmetics.user.CosmeticUsers;
import com.hibiscusmc.hmccosmetics.user.manager.UserEmoteManager;
import com.hibiscusmc.hmccosmetics.util.MessagesUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerConnectionListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        Runnable run = () -> {
            CosmeticUser user = Database.get(event.getPlayer().getUniqueId());
            CosmeticUsers.addUser(user);
            MessagesUtil.sendDebugMessages("Run User Join");
            Bukkit.getScheduler().runTaskLater(HMCCosmeticsPlugin.getInstance(), () -> user.updateCosmetic(), 4);
        };

        if (DatabaseSettings.isEnabledDelay()) {
            MessagesUtil.sendDebugMessages("Delay Enabled with " + DatabaseSettings.getDelayLength() + " ticks");
            Bukkit.getScheduler().runTaskLater(HMCCosmeticsPlugin.getInstance(), run, DatabaseSettings.getDelayLength());
        } else {
            run.run();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        CosmeticUser user = CosmeticUsers.getUser(event.getPlayer());
        if (user == null) { // Remove any passengers if a user failed to initialize. Bugs can cause this to happen
            if (!event.getPlayer().getPassengers().isEmpty()) {
                for (Entity entity : event.getPlayer().getPassengers()) {
                    if (entity.getType() == EntityType.ARMOR_STAND) {
                        entity.remove();
                    }
                }
            }
            return;
        }
        if (user.isInWardrobe()) user.leaveWardrobe();
        if (user.getUserEmoteManager().isPlayingEmote()) {
            user.getUserEmoteManager().stopEmote(UserEmoteManager.StopEmoteReason.CONNECTION);
            event.getPlayer().setInvisible(false);
        }
        Database.save(user);
        user.destroy();
        CosmeticUsers.removeUser(user.getUniqueId());
    }
}