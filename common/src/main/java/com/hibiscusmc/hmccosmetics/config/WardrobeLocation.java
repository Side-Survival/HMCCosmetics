package com.hibiscusmc.hmccosmetics.config;

import lombok.Setter;
import org.bukkit.Location;

public class WardrobeLocation {

    @Setter
    private Location npcLocation;
    @Setter
    private Location viewerLocation;
    @Setter
    private Location viewerOpenLocation;
    @Setter
    private Location leaveLocation;

    public WardrobeLocation(Location npcLocation, Location viewerLocation, Location viewerOpenLocation, Location leaveLocation) {
        this.npcLocation = npcLocation;
        this.viewerLocation = viewerLocation;
        this.viewerOpenLocation = viewerOpenLocation;
        this.leaveLocation = leaveLocation;
    }

    public Location getNpcLocation() {
        return npcLocation.clone();
    }

    public Location getViewerLocation() {
        return viewerLocation.clone();
    }

    public Location getViewerOpenLocation() {
        return viewerOpenLocation;
    }

    public Location getLeaveLocation() {
        return leaveLocation.clone();
    }

    public boolean hasAllLocations() {
        return npcLocation != null && viewerLocation != null && leaveLocation != null;
    }
}
