package dev.munky.instantiated.event;

import dev.munky.instantiated.dungeon.Dungeon;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

public abstract class DungeonEvent extends Event implements Cancellable {
    private final Dungeon dungeon;
    private boolean cancelled = false;
    public DungeonEvent(Dungeon dungeon) {
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
    public Dungeon getDungeon(){
        return this.dungeon;
    }
}
