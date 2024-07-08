package dev.munky.instantiated.event.room;

import dev.munky.instantiated.dungeon.InstancedDungeonRoom;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class InstancedDungeonRoomPlayerEnterEvent extends InstancedDungeonRoomEvent{
    public static HandlerList handlers = new HandlerList();
    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList(){
        return handlers;
    }
    final Player player;
    public InstancedDungeonRoomPlayerEnterEvent(InstancedDungeonRoom room, Player player) {
        super(room);
        this.player = player;
    }
    public Player getPlayer(){
        return this.player;
    }
}

