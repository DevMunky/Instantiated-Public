package dev.munky.instantiated.event.room.mob;

import dev.munky.instantiated.dungeon.InstancedDungeon;
import dev.munky.instantiated.event.InstancedDungeonEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class InstancedDungeonMobKillEvent extends InstancedDungeonEvent {
    public static HandlerList handlers = new HandlerList();
    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList(){
        return handlers;
    }
    public final LivingEntity victim;
    public InstancedDungeonMobKillEvent(InstancedDungeon dungeon, LivingEntity victim) {
        super(dungeon, false);
        this.victim = victim;
    }
}
