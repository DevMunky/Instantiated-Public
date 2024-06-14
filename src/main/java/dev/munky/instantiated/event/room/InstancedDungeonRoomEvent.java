package dev.munky.instantiated.event.room;

import dev.munky.instantiated.event.InstancedDungeonEvent;
import dev.munky.instantiated.dungeon.room.InstancedDungeonRoom;

public abstract class InstancedDungeonRoomEvent extends InstancedDungeonEvent {
    private final InstancedDungeonRoom room;
    public InstancedDungeonRoomEvent(InstancedDungeonRoom room) {
        super(room.getParent());
        this.room = room;
    }
    public InstancedDungeonRoom getRoom(){
        return this.room;
    }
}
