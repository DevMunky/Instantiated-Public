package dev.munky.instantiated.event;

import dev.munky.instantiated.dungeon.InstancedDungeon;

public abstract class InstancedDungeonEvent extends DungeonEvent{
    private final InstancedDungeon dungeonInstance;
    public InstancedDungeonEvent(InstancedDungeon dungeonInstance, boolean async) {
        super(dungeonInstance.getMaster(), async);
        this.dungeonInstance = dungeonInstance;
    }
    public InstancedDungeon getInstancedDungeon(){
        return this.dungeonInstance;
    }
}
