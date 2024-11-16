package com.hibiscusmc.hmccosmetics.user.manager;

import com.hibiscusmc.hmccosmetics.user.CosmeticUser;
import com.hibiscusmc.hmccosmetics.user.CosmeticUsers;
import com.hibiscusmc.hmccosmetics.util.HMCCPlayerUtils;
import com.hibiscusmc.hmccosmetics.util.packets.HMCCPacketManager;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserBalloonPufferfish extends UserEntity {

    @Getter
    private final int pufferFishEntityId;
    @Getter
    private final UUID uuid;
    private boolean destroyed = false;

    public UserBalloonPufferfish(UUID owner, int pufferFishEntityId, UUID uuid) {
        super(owner);
        this.pufferFishEntityId = pufferFishEntityId;
        this.uuid = uuid;
    }

    public void hidePufferfish() {
        HMCCPacketManager.sendEntityDestroyPacket(pufferFishEntityId, getViewers());
        getViewers().clear();
    }

    public void spawnPufferfish(Location location, List<Player> sendTo) {
        HMCCPacketManager.sendEntitySpawnPacket(location, pufferFishEntityId, EntityType.PUFFERFISH, uuid, sendTo);
        HMCCPacketManager.sendInvisibilityPacket(pufferFishEntityId, sendTo);
    }

    public void destroyPufferfish() {
        HMCCPacketManager.sendEntityDestroyPacket(pufferFishEntityId, getViewers());
        getViewers().clear();
        destroyed = true;
    }

    @Override
    public List<Player> refreshViewers(Location location) {
        if (destroyed) return List.of(); //Prevents refreshing a destroyed entity
        if (System.currentTimeMillis() - getViewerLastUpdate() <= 1000) return List.of(); //Prevents mass refreshes
        ArrayList<Player> newPlayers = new ArrayList<>();
        ArrayList<Player> removePlayers = new ArrayList<>();
        List<Player> players = HMCCPlayerUtils.getNearbyPlayers(location);

        for (Player player : players) {
            CosmeticUser user = CosmeticUsers.getUser(player);
            if (user != null && getOwner() != user.getUniqueId() && user.isInWardrobe()) { // Fixes issue where players in wardrobe would see other players cosmetics if they were not in wardrobe
                removePlayers.add(player);
                HMCCPacketManager.sendEntityDestroyPacket(getPufferFishEntityId(), List.of(player));
                continue;
            }
            if (!getViewers().contains(player)) {
                getViewers().add(player);
                newPlayers.add(player);
                continue;
            }
            // bad loopdy loops
            for (Player viewerPlayer : getViewers()) {
                if (!players.contains(viewerPlayer)) {
                    removePlayers.add(viewerPlayer);
                    HMCCPacketManager.sendEntityDestroyPacket(getPufferFishEntityId(), List.of(viewerPlayer));
                }
            }
        }
        getViewers().removeAll(removePlayers);
        setViewerLastUpdate(System.currentTimeMillis());
        return newPlayers;
    }
}
