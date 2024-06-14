package dev.munky.instantiated.dungeon.room.door;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import dev.munky.instantiated.dungeon.Dungeon;
import dev.munky.instantiated.dungeon.room.InstancedDungeonRoom;
import dev.munky.instantiated.dungeon.room.RegionHolder;
import dev.munky.instantiated.util.ImmutableVector;
import org.bukkit.Material;

public class DungeonDoor implements RegionHolder<CuboidRegion> {
    private CuboidRegion region;
    private Dungeon parent;
    private final String identifier;
    private final ImmutableVector origin;
    private final boolean modelled;
    private final String openAnimation;
    private final Material baseMaterial;
    private final Material openMaterial;
    private final String modelIdentifier;
    public DungeonDoor(
            Dungeon parent,
            String identifier,
            CuboidRegion region,
            BlockVector3 origin,
            boolean modelled,
            String openAnimation,
            Material baseMaterial,
            Material openMaterial,
            String modelIdentifier
    ){
        this.parent = parent;
        this.region = region;
        this.identifier = identifier;
        this.modelled = modelled;
        this.openAnimation = openAnimation;
        this.origin = ImmutableVector.of(origin);
        this.baseMaterial = baseMaterial;
        this.openMaterial = openMaterial;
        this.modelIdentifier = modelIdentifier;
    }
    public Material getBaseMaterial() {
        return baseMaterial;
    }
    public Dungeon getParent(){
        return this.parent;
    }

    public Material getOpenMaterial() {
        return openMaterial;
    }

    public String getModelIdentifier() {
        return modelIdentifier;
    }

    public String getOpenAnimation(){
        return this.openAnimation;
    }
    public boolean isModelled(){
        return this.modelled;
    }
    public String getIdentifier(){
        return this.identifier;
    }
    @Override
    public void setRegion(CuboidRegion newRegion) {
        this.region = newRegion;
    }
    @Override
    public CuboidRegion getRegion() {
        return this.region;
    }
    @Override
    public BlockVector3 getOrigin() {
        return origin.toBlockVector3();
    }
    public InstancedDungeonDoor instance(InstancedDungeonRoom parent){
        return new InstancedDungeonDoor(parent,this);
    }
}
