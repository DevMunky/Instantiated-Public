package dev.munky.instantiated.event.room;

import dev.munky.instantiated.dungeon.InstancedDungeonRoom;
import dev.munky.instantiated.event.InstancedDungeonEvent;
import org.bukkit.Bukkit;

public abstract class InstancedDungeonRoomEvent extends InstancedDungeonEvent {
    private final InstancedDungeonRoom room;
    public InstancedDungeonRoomEvent(InstancedDungeonRoom room) {
        super(room.getParent(), !Bukkit.isPrimaryThread());
        this.room = room;
    }
    public InstancedDungeonRoom getRoom(){
        return this.room;
    }
}
