package dev.munky.instantiated.event.room.key;

import dev.munky.instantiated.dungeon.InstancedDungeon;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class InstancedDungeonKeyDropEvent extends InstancedDungeonKeyEvent {
    public static HandlerList handlers = new HandlerList();
    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList(){
        return handlers;
    }
    private final Location dropLocation;
    private final Item keyItem;
    public InstancedDungeonKeyDropEvent(InstancedDungeon dungeonInstance, Item keyItem, Location dropLocation) {
        super(dungeonInstance,dungeonInstance.getDoorKeys());
        this.dropLocation = dropLocation;
        this.keyItem = keyItem;
    }
    public Location getDropLocation() {
        return dropLocation;
    }

    public Item getKeyItem() {
        return keyItem;
    }
}
