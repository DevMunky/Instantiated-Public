package dev.munky.instantiated.dungeon.room;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.ConvexPolyhedralRegion;
import dev.munky.instantiated.dungeon.Dungeon;
import dev.munky.instantiated.dungeon.InstancedDungeon;
import dev.munky.instantiated.dungeon.room.door.DungeonDoor;
import dev.munky.instantiated.dungeon.room.mob.DungeonMob;
import dev.munky.instantiated.util.ImmutableVector;
import org.bukkit.*;

import java.util.*;

public class DungeonRoom implements RoomLike<Dungeon,ConvexPolyhedralRegion> {
    private final Dungeon parent;
    private final String identifier;
    private final BlockVector3 origin;
    private Map<String,DungeonMob> entities;
    private final Map<ImmutableVector,DungeonDoor> doors;
    private ConvexPolyhedralRegion polyRegion;
    private final KeyDropMode keyDropMode;
    private final Material keyMaterial;
    public DungeonRoom(
            Dungeon parent,
            String identifier,
            BlockVector3 origin,
            ConvexPolyhedralRegion region,
            Map<String,DungeonMob> entities,
            Map<ImmutableVector, DungeonDoor> doors,
            KeyDropMode keyDropMode,
            Material keyMaterial
    ){
        this.parent = parent;
        this.identifier = identifier;
        this.origin = origin;
        this.polyRegion = region;
        this.entities = entities;
        this.doors = doors;
        this.keyDropMode = keyDropMode;
        this.keyMaterial = keyMaterial;
    }
    @Override
    public void setRegion(ConvexPolyhedralRegion newRegion) {
        this.polyRegion = newRegion;
    }

    @Override
    public ConvexPolyhedralRegion getRegion() {
        return this.polyRegion;
    }
    public InstancedDungeonRoom instance(InstancedDungeon realInstance){
        return new InstancedDungeonRoom(realInstance,this);
    }
    public String toString(){
        return "DungeonRoom[" + origin + ", " + polyRegion + "]";
    }

    @Override
    public Map<String,DungeonMob> getEntities() {
        return this.entities;
    }
    public Map<ImmutableVector, DungeonDoor> getDoors() {
        return this.doors;
    }
    @Override
    public BlockVector3 getOrigin() {
        return this.origin;
    }

    @Override
    public String getIdentifier() {
        return this.identifier;
    }

    @Override
    public Dungeon getParent() {
        return this.parent;
    }

    public KeyDropMode getKeyDropMode(){
        return this.keyDropMode;
    }
    public Material getKeyMaterial(){
        return this.keyMaterial;
    }
    public enum KeyDropMode{
        MARKED_ROOM_MOB_KILL,
        ROOM_MOBS_CLEAR
    }
}
