package dev.munky.instantiated.event.room;

import dev.munky.instantiated.dungeon.InstancedDungeonRoom;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class InstancedDungeonRoomCompleteEvent extends InstancedDungeonRoomEvent {
    public static HandlerList handlers = new HandlerList();
    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList(){
        return handlers;
    }
    public InstancedDungeonRoomCompleteEvent(InstancedDungeonRoom room) {
        super(room);
    }
}
