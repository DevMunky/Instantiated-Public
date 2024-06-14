package dev.munky.instantiated.event.room.key;

import dev.munky.instantiated.event.InstancedDungeonEvent;
import dev.munky.instantiated.dungeon.InstancedDungeon;

public abstract class InstancedDungeonKeyEvent extends InstancedDungeonEvent {
    private final int current;
    public InstancedDungeonKeyEvent(InstancedDungeon dungeonInstance, int currentKeys) {
        super(dungeonInstance);
        this.current = currentKeys;
    }
    public int getCurrentKeys(){
        return this.current;
    }
}
