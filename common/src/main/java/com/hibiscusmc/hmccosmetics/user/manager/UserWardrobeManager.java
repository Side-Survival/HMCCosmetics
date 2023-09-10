package com.hibiscusmc.hmccosmetics.user.manager;

import com.hibiscusmc.hmccosmetics.HMCCosmeticsPlugin;
import com.hibiscusmc.hmccosmetics.config.Wardrobe;
import com.hibiscusmc.hmccosmetics.config.WardrobeLocation;
import com.hibiscusmc.hmccosmetics.config.WardrobeSettings;
import com.hibiscusmc.hmccosmetics.cosmetic.Cosmetic;
import com.hibiscusmc.hmccosmetics.cosmetic.CosmeticSlot;
import com.hibiscusmc.hmccosmetics.cosmetic.types.CosmeticBalloonType;
import com.hibiscusmc.hmccosmetics.gui.Menu;
import com.hibiscusmc.hmccosmetics.gui.Menus;
import com.hibiscusmc.hmccosmetics.nms.NMSHandlers;
import com.hibiscusmc.hmccosmetics.user.CosmeticUser;
import com.hibiscusmc.hmccosmetics.util.MessagesUtil;
import com.hibiscusmc.hmccosmetics.util.ServerUtils;
import com.hibiscusmc.hmccosmetics.util.packets.PacketManager;
import lombok.Getter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class UserWardrobeManager {

    @Getter
    private final int NPC_ID;
    @Getter
    private final UUID WARDROBE_UUID;
    @Getter
    private String npcName;
    @Getter
    private GameMode originalGamemode;
    @Getter
    private final CosmeticUser user;
    @Getter
    private final Wardrobe wardrobe;
    @Getter
    private final WardrobeLocation wardrobeLocation;
    @Getter
    private final Location viewingLocation;
    @Getter
    private final Location viewingOpenLocation;
    @Getter
    private final Location npcLocation;
    @Getter
    private Location exitLocation;
    @Getter
    private BossBar bossBar;
    @Getter
    private boolean active;
    @Getter
    private WardrobeStatus wardrobeStatus;

    private BukkitRunnable armorStandRotationRunnable = null;
    private float armorStandRotationTarget;
    private float armorStandRotationLast;

    public UserWardrobeManager(CosmeticUser user, Wardrobe wardrobe) {
        NPC_ID = NMSHandlers.getHandler().getNextEntityId();
        WARDROBE_UUID = UUID.randomUUID();
        this.user = user;

        this.wardrobe = wardrobe;
        this.wardrobeLocation = wardrobe.getLocation();

        this.exitLocation = wardrobeLocation.getLeaveLocation();
        this.viewingLocation = wardrobeLocation.getViewerLocation();
        this.viewingOpenLocation = wardrobeLocation.getViewerOpenLocation();
        this.npcLocation = wardrobeLocation.getNpcLocation();

        wardrobeStatus = WardrobeStatus.SETUP;
    }

    @SuppressWarnings("ConstantConditions")
    public void start() {
        if (wardrobeStatus == WardrobeStatus.STARTING)
            return;

        setWardrobeStatus(WardrobeStatus.STARTING);
        Player player = user.getPlayer();

        this.originalGamemode = player.getGameMode();
        if (WardrobeSettings.isReturnLastLocation()) {
            this.exitLocation = player.getLocation().clone();
        }

        armorStandRotationLast = viewingLocation.getYaw();

        List<Player> viewer = Collections.singletonList(player);
        List<Player> outsideViewers = PacketManager.getViewers(viewingLocation);
        outsideViewers.remove(player);

        MessagesUtil.sendMessage(player, "opened-wardrobe");

        Runnable run = () -> {
            // Player
            player.teleport(viewingLocation);

            // NPC
            npcName = "WardrobeNPC-" + NPC_ID;
            while (npcName.length() > 16) {
                npcName = npcName.substring(16);
            }
            PacketManager.sendFakePlayerInfoPacket(player, NPC_ID, WARDROBE_UUID, npcName, viewer);

            // NPC 2
            Bukkit.getScheduler().runTaskLater(HMCCosmeticsPlugin.getInstance(), () -> {
                PacketManager.sendFakePlayerSpawnPacket(npcLocation, WARDROBE_UUID, NPC_ID, viewer);
                PacketManager.sendPlayerOverlayPacket(NPC_ID, viewer);
                MessagesUtil.sendDebugMessages("Spawned Fake Player on " + npcLocation);
                NMSHandlers.getHandler().hideNPCName(player, npcName);
            }, 4);

            // Location
            PacketManager.sendLookPacket(NPC_ID, npcLocation, viewer);
            PacketManager.sendRotationPacket(NPC_ID, npcLocation, true, viewer);

            // Misc
            if (user.hasCosmeticInSlot(CosmeticSlot.BACKPACK)) {
                // Maybe null as backpack maybe despawned before entering
                if (user.getUserBackpackManager() == null) user.respawnBackpack();
                user.getUserBackpackManager().getEntityManager().teleport(npcLocation.clone().add(0.5, 2, 0.5));
                NMSHandlers.getHandler().equipmentSlotUpdate(user.getUserBackpackManager().getFirstArmorStandId(), EquipmentSlot.HEAD, user.getUserCosmeticItem(user.getCosmetic(CosmeticSlot.BACKPACK)), viewer);
                PacketManager.ridingMountPacket(NPC_ID, user.getUserBackpackManager().getFirstArmorStandId(), viewer);
            }

            if (user.hasCosmeticInSlot(CosmeticSlot.BALLOON)) {
                CosmeticBalloonType cosmetic = (CosmeticBalloonType) user.getCosmetic(CosmeticSlot.BALLOON);
                user.getBalloonManager().sendRemoveLeashPacket(viewer);
                user.getBalloonManager().sendLeashPacket(NPC_ID);
                //PacketManager.sendLeashPacket(VIEWER.getBalloonEntity().getModelId(), NPC_ID, viewer);

                Location balloonLocation = npcLocation.clone().add(0, 2, 0);
                PacketManager.sendTeleportPacket(user.getBalloonManager().getPufferfishBalloonId(), balloonLocation , false, viewer);
                user.getBalloonManager().getModelEntity().teleport(balloonLocation);
            }

            if (WardrobeSettings.isEnabledBossbar()) {
                float progress = WardrobeSettings.getBossbarProgress();
                Component message = MessagesUtil.processStringNoKey(WardrobeSettings.getBossbarMessage());

                bossBar = BossBar.bossBar(message, progress, WardrobeSettings.getBossbarColor(), WardrobeSettings.getBossbarOverlay());
                Audience target = BukkitAudiences.create(HMCCosmeticsPlugin.getInstance()).player(player);

                target.showBossBar(bossBar);
            }

            if (WardrobeSettings.isEnterOpenMenu()) {
                Menu menu = Menus.getDefaultMenu();
                if (menu != null) menu.openMenu(user);
            }

            this.active = true;
            update();
            setWardrobeStatus(WardrobeStatus.RUNNING);

            PacketManager.sendLookPacket(NPC_ID, npcLocation, viewer);
        };


        if (WardrobeSettings.isEnabledTransition()) {
            MessagesUtil.sendTitle(
                    user.getPlayer(),
                    WardrobeSettings.getTransitionText(),
                    WardrobeSettings.getTransitionFadeIn(),
                    WardrobeSettings.getTransitionStay(),
                    WardrobeSettings.getTransitionFadeOut()
            );
            Bukkit.getScheduler().runTaskLater(HMCCosmeticsPlugin.getInstance(), run, WardrobeSettings.getTransitionDelay());
        } else {
            run.run();
        }

    }

    @SuppressWarnings("ConstantConditions")
    public void end() {
        setWardrobeStatus(WardrobeStatus.STOPPING);
        Player player = user.getPlayer();

        List<Player> viewer = Collections.singletonList(player);
        List<Player> outsideViewers = PacketManager.getViewers(viewingLocation);
        outsideViewers.remove(player);

        if (player != null) MessagesUtil.sendMessage(player, "closed-wardrobe");

        Runnable run = () -> {
            this.active = false;

            // For Wardrobe Temp Cosmetics
            for (Cosmetic cosmetic : user.getCosmetics()) {
                MessagesUtil.sendDebugMessages("Checking... " + cosmetic.getId());
                if (!user.canEquipCosmetic(cosmetic)) {
                    MessagesUtil.sendDebugMessages("Unable to keep " + cosmetic.getId());
                    user.removeCosmeticSlot(cosmetic.getSlot());
                }
            }

            // NPC
            if (user.hasCosmeticInSlot(CosmeticSlot.BALLOON)) user.getBalloonManager().sendRemoveLeashPacket();
            PacketManager.sendEntityDestroyPacket(NPC_ID, viewer); // Success
            PacketManager.sendRemovePlayerPacket(player, WARDROBE_UUID, viewer); // Success

            if (user.hasCosmeticInSlot(CosmeticSlot.BACKPACK)) {
                user.respawnBackpack();
            }

            if (user.hasCosmeticInSlot(CosmeticSlot.BALLOON)) {
                user.respawnBalloon();
            }

            player.teleport(Objects.requireNonNullElseGet(exitLocation, () -> player.getWorld().getSpawnLocation()));

            if (WardrobeSettings.isEquipPumpkin()) {
                NMSHandlers.getHandler().equipmentSlotUpdate(user.getPlayer().getEntityId(), EquipmentSlot.HEAD, player.getInventory().getHelmet(), viewer);
            }

            if (WardrobeSettings.isEnabledBossbar()) {
                Audience target = BukkitAudiences.create(HMCCosmeticsPlugin.getInstance()).player(player);

                target.hideBossBar(bossBar);
            }

            user.updateCosmetic();
        };
        run.run();
        if (armorStandRotationRunnable != null) {
            armorStandRotationRunnable.cancel();
            armorStandRotationRunnable = null;
        }
    }

    public void update() {
        final AtomicInteger data = new AtomicInteger();
        data.set((int) npcLocation.getYaw());

        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                Player player = user.getPlayer();
                if (!active || player == null) {
                    MessagesUtil.sendDebugMessages("WardrobeEnd[user=" + user.getUniqueId() + ",reason=Active is false]");
                    this.cancel();
                    return;
                }
                MessagesUtil.sendDebugMessages("WardrobeUpdate[user=" + user.getUniqueId() + ",status=" + getWardrobeStatus() + "]");
                List<Player> viewer = Collections.singletonList(player);
                List<Player> outsideViewers = PacketManager.getViewers(viewingLocation);
                outsideViewers.remove(player);

                Location location = npcLocation;
                int yaw = data.get();
                location.setYaw(yaw);

                PacketManager.sendLookPacket(NPC_ID, location, viewer);
                int rotationSpeed = WardrobeSettings.getRotationSpeed();
                location.setYaw(ServerUtils.getNextYaw(yaw - 5, rotationSpeed));
                PacketManager.sendRotationPacket(NPC_ID, location, true, viewer);
                int nextyaw = ServerUtils.getNextYaw(yaw, rotationSpeed);
                data.set(nextyaw);

                for (CosmeticSlot slot : CosmeticSlot.values()) {
                    PacketManager.equipmentSlotUpdate(NPC_ID, user, slot, viewer);
                }

                if (user.hasCosmeticInSlot(CosmeticSlot.BACKPACK)) {
                    PacketManager.sendTeleportPacket(user.getUserBackpackManager().getFirstArmorStandId(), location, false, viewer);
                    PacketManager.ridingMountPacket(NPC_ID, user.getUserBackpackManager().getFirstArmorStandId(), viewer);
                    user.getUserBackpackManager().getEntityManager().setRotation(nextyaw);
                    PacketManager.sendEntityDestroyPacket(user.getUserBackpackManager().getFirstArmorStandId(), outsideViewers);
                }

                if (user.hasCosmeticInSlot(CosmeticSlot.BALLOON)) {
                    // The two lines below broke, solved by listening to PlayerCosmeticPostEquipEvent
                    //PacketManager.sendTeleportPacket(user.getBalloonManager().getPufferfishBalloonId(), npcLocation.add(Settings.getBalloonOffset()), false, viewer);
                    //user.getBalloonManager().getModelEntity().teleport(npcLocation.add(Settings.getBalloonOffset()));
                    user.getBalloonManager().sendRemoveLeashPacket(outsideViewers);
                    PacketManager.sendEntityDestroyPacket(user.getBalloonManager().getModelId(), outsideViewers);
                    user.getBalloonManager().sendLeashPacket(NPC_ID);
                }

                if (WardrobeSettings.isEquipPumpkin()) {
                    NMSHandlers.getHandler().equipmentSlotUpdate(user.getPlayer().getEntityId(), EquipmentSlot.HEAD, new ItemStack(Material.CARVED_PUMPKIN), viewer);
                }
            }
        };

        runnable.runTaskTimer(HMCCosmeticsPlugin.getInstance(), 0, 2);
    }

    public void guiOpened() {
//        if (armorStandRotationRunnable != null)
//            armorStandRotationRunnable.cancel();
//
//        armorStandRotationTarget = viewingOpenLocation.getYaw();
//        updateArmorStandRotationRunnable(viewingOpenLocation);
    }

    public void guiClosed() {
//        if (armorStandRotationRunnable != null)
//            armorStandRotationRunnable.cancel();
//
//        armorStandRotationTarget = viewingLocation.getYaw();
//        updateArmorStandRotationRunnable(viewingLocation);
    }

    private void updateArmorStandRotationRunnable(Location base) {
//        armorStandRotationRunnable = new BukkitRunnable() {
//            @Override
//            public void run() {
//                Player player = user.getPlayer();
//                if (!active || player == null) {
//                    MessagesUtil.sendDebugMessages("WardrobeEnd[user=" + user.getUniqueId() + ",reason=Active is false]");
//                    cancel();
//                    return;
//                }
//                List<Player> viewer = Collections.singletonList(player);
//
//                if (armorStandRotationTarget > armorStandRotationLast) {
//                    armorStandRotationLast += 4f;
//                    if (armorStandRotationLast > armorStandRotationTarget)
//                        armorStandRotationLast = armorStandRotationTarget;
//                } else {
//                    armorStandRotationLast -= 4f;
//                    if (armorStandRotationLast < armorStandRotationTarget)
//                        armorStandRotationLast = armorStandRotationTarget;
//                }
//
//                Location to = base.clone();
//                to.setYaw(armorStandRotationLast);
//
//                stand.teleport(to);
//
//                if (armorStandRotationLast == armorStandRotationTarget) {
//                    armorStandRotationRunnable.cancel();
//                    armorStandRotationRunnable = null;
//                }
//            }
//        };
//
//        armorStandRotationRunnable.runTaskTimer(HMCCosmeticsPlugin.getInstance(), 0, 1);
    }

    public void setWardrobeStatus(WardrobeStatus status) {
        this.wardrobeStatus = status;
    }

    public enum WardrobeStatus {
        SETUP,
        STARTING,
        RUNNING,
        STOPPING,
    }

}
