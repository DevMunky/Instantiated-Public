package dev.munky.instantiated.event.room.key;

import dev.munky.instantiated.dungeon.InstancedDungeon;
import dev.munky.instantiated.dungeon.sstatic.StaticInstancedDungeonDoor;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class InstancedDungeonKeyUsedEvent extends InstancedDungeonKeyEvent {
    public static HandlerList handlers = new HandlerList();
    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList(){
        return handlers;
    }
    private final StaticInstancedDungeonDoor openedDoor;

    /**
     * Only called if a door is opened with {@link InstancedDungeonDoor#open(boolean)} and useKeys is true
     * @param dungeonInstance the instanced dungeon in which a key was used
     * @param openedDoor the instanced door that was opened with a key.
     */
    public InstancedDungeonKeyUsedEvent(InstancedDungeon dungeonInstance, StaticInstancedDungeonDoor openedDoor) {
        super(dungeonInstance,dungeonInstance.getDoorKeys());
        this.openedDoor = openedDoor;
    }
    public StaticInstancedDungeonDoor getOpenedDoor(){
        return this.openedDoor;
    }
}
