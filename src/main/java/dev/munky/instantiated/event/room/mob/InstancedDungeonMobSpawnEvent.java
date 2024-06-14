package dev.munky.instantiated.event.room.mob;

import dev.munky.instantiated.event.room.InstancedDungeonRoomEvent;
import dev.munky.instantiated.dungeon.room.InstancedDungeonRoom;
import dev.munky.instantiated.dungeon.room.mob.DungeonMob;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class InstancedDungeonMobSpawnEvent extends InstancedDungeonRoomEvent {
    public static HandlerList handlers = new HandlerList();
    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList(){
        return handlers;
    }
    private final DungeonMob dungeonMob;
    private Location spawnLocation;
    public InstancedDungeonMobSpawnEvent(InstancedDungeonRoom instancedDungeonRoom, DungeonMob dungeonMob, Location spawnLocation) {
        super(instancedDungeonRoom);
        this.dungeonMob = dungeonMob;
        this.spawnLocation = spawnLocation;
    }
    public Location getSpawnLocation(){
        return this.spawnLocation;
    }
    public void setSpawnLocation(Location location){
        this.spawnLocation = location;
    }
    public DungeonMob getDungeonMob(){
        return this.dungeonMob;
    }
    public void setLivingEntity(LivingEntity entity){
        getDungeonMob().setLivingEntity(entity);
    }
    public LivingEntity getLivingEntity(){
        return getDungeonMob().getLivingEntity();
    }
}
