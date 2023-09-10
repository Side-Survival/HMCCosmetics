package com.hibiscusmc.hmccosmetics.cosmetic.types;

import com.hibiscusmc.hmccosmetics.cosmetic.Cosmetic;
import com.hibiscusmc.hmccosmetics.user.CosmeticUser;
import com.hibiscusmc.hmccosmetics.util.PlayerUtils;
import com.hibiscusmc.hmccosmetics.util.packets.PacketManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;

public class CosmeticMainhandType extends Cosmetic {

    public CosmeticMainhandType(String id, ConfigurationNode config) {
        super(id, config);
    }

    @Override
    public void update(@NotNull CosmeticUser user) {
        Player player = user.getPlayer();
        if (user.isInWardrobe()) return;

        PacketManager.equipmentSlotUpdate(player.getEntityId(), user, getSlot(), PlayerUtils.getNearbyPlayers(player));
    }
}
