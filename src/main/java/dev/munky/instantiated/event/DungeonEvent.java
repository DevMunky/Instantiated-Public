package dev.munky.instantiated.event;

import dev.munky.instantiated.dungeon.DungeonFormat;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

public abstract class DungeonEvent extends Event implements Cancellable {
    private final DungeonFormat dungeon;
    private boolean cancelled = false;
    public DungeonEvent(DungeonFormat dungeon, boolean async) {
        super(async);
        this.dungeon = dungeon;
    }
    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }
    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
    public DungeonFormat getDungeon(){
        return this.dungeon;
    }
}
