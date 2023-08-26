package com.hibiscusmc.hmccosmetics.hooks.items;

import com.hibiscusmc.hmccosmetics.cosmetic.Cosmetic;
import com.hibiscusmc.hmccosmetics.cosmetic.Cosmetics;
import com.hibiscusmc.hmccosmetics.hooks.Hook;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * A hook that integrates the plugin {@link com.hibiscusmc.hmccosmetics.HMCCosmeticsPlugin HMCCosmetics} to provide cosmetic items
 */
public class HookSurvivalCosmetics extends Hook {
    public HookSurvivalCosmetics() {
        super("SurvivalCosmetics");
        setEnabledItemHook(true);
    }

    /**
     * Gets a cosmetic {@link ItemStack} that is associated with the provided id from the plugin {@link com.hibiscusmc.hmccosmetics.HMCCosmeticsPlugin HMCCosmetics}
     */
    @Override
    public ItemStack getItem(@NotNull String itemId) {
        Cosmetic cosmetic = Cosmetics.getCosmetic(itemId);
        if (cosmetic == null) return null;
        return cosmetic.getItem();
    }
}

