package dev.munky.instantiated.event.room.key;

import dev.munky.instantiated.dungeon.InstancedDungeon;
import dev.munky.instantiated.dungeon.room.door.InstancedDungeonDoor;
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
    private final InstancedDungeonDoor openedDoor;

    /**
     * Only called if a door is opened with {@link InstancedDungeonDoor#open(boolean)} and useKeys is true
     * @param dungeonInstance the instanced dungeon that contains players in which a key was used
     * @param openedDoor the instanced door that was opened with a key.
     */
    public InstancedDungeonKeyUsedEvent(InstancedDungeon dungeonInstance, InstancedDungeonDoor openedDoor) {
        super(dungeonInstance,dungeonInstance.getDoorKeys());
        this.openedDoor = openedDoor;
    }
    public InstancedDungeonDoor getOpenedDoor(){
        return this.openedDoor;
    }
}
