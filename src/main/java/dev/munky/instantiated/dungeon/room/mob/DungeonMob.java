package dev.munky.instantiated.dungeon.room.mob;

import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.Immutable;
import dev.munky.instantiated.Instantiated;
import dev.munky.instantiated.annotation.Mutable;
import dev.munky.instantiated.util.ImmutableLocation;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class DungeonMob {
    private final String mobIdentifier;
    // Living entity is set at runtime, so no need for persistence
    private LivingEntity livingEntity;
    private final ImmutableLocation spawnLocation;
    private final boolean marked;
    private final Map<String,String> custom;
    public DungeonMob(String identifier, Location spawnLocation, boolean marked, Map<String,String> custom){
        this(identifier,null,spawnLocation,marked,custom);
    }
    public DungeonMob(String identifier, LivingEntity entity, Location spawnLocation, boolean marked, Map<String,String> custom){
        this.mobIdentifier = identifier;
        this.livingEntity = entity;
        this.spawnLocation = ImmutableLocation.of(spawnLocation);
        this.marked = marked;
        this.custom = custom;
    }
    public String getMobIdentifier(){
        return this.mobIdentifier;
    }
    @Nullable
    public LivingEntity getLivingEntity(){
        return this.livingEntity;
    }
    public void setLivingEntity(LivingEntity entity){
        if (entity!=null){
            entity.setMetadata("instantiated-persist", new FixedMetadataValue(Instantiated.instance(), true));
        }
        this.livingEntity = entity;
    }
    public ImmutableLocation getSpawnLocation(){
        return this.spawnLocation;
    }
    public boolean isMarked(){
        return this.marked;
    }
    @Mutable
    public Map<String,String> getCustom(){
        return this.custom;
    }
    /**
     * Returns a new DungeonMob, with a copied LivingEntity that can be spawned again as a new Entity in the world.
     * @return a copy of this object
     */
    @Override
    public DungeonMob clone(){
        DungeonMob result;
        if (livingEntity==null) {
            result = new DungeonMob(mobIdentifier, spawnLocation.toLocation(), marked, custom);
        } else {
            result = new DungeonMob(mobIdentifier, (LivingEntity) livingEntity.copy(), spawnLocation.toLocation(), marked, custom);
        }
        return result;
    }
    public String toString(){
        if (livingEntity!=null){
            return getMobIdentifier() + "@" + getSpawnLocation() + "#" + livingEntity;
        }
        return getMobIdentifier() + "@" + getSpawnLocation() + "#UnconfiguredEntity";
    }
}
