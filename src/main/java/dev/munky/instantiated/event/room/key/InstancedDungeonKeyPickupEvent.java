package dev.munky.instantiated.event.room.key;

import dev.munky.instantiated.dungeon.InstancedDungeon;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class InstancedDungeonKeyPickupEvent extends InstancedDungeonKeyEvent {
    public static HandlerList handlers = new HandlerList();
    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList(){
        return handlers;
    }
    public final Player player;
    public InstancedDungeonKeyPickupEvent(InstancedDungeon dungeonInstance, Player player) {
        super(dungeonInstance,dungeonInstance.getDoorKeys());
        this.player = player;
    }
    public Player getPlayer(){
        return this.player;
    }
}
