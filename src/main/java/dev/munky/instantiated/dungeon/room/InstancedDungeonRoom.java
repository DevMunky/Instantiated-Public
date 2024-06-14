package dev.munky.instantiated.dungeon.room;

import com.fastasyncworldedit.core.FaweAPI;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.ConvexPolyhedralRegion;
import com.sk89q.worldedit.regions.CuboidRegion;
import dev.munky.instantiated.Instantiated;
import dev.munky.instantiated.event.ListenerFactory;
import dev.munky.instantiated.event.room.mob.InstancedDungeonMobSpawnEvent;
import dev.munky.instantiated.dungeon.DungeonManager;
import dev.munky.instantiated.dungeon.InstancedDungeon;
import dev.munky.instantiated.dungeon.Instantiable;
import dev.munky.instantiated.dungeon.room.door.DungeonDoor;
import dev.munky.instantiated.dungeon.room.door.InstancedDungeonDoor;
import dev.munky.instantiated.dungeon.room.mob.DungeonMob;
import dev.munky.instantiated.util.ComponentUtil;
import dev.munky.instantiated.util.ImmutableLocation;
import dev.munky.instantiated.util.ImmutableVector;
import dev.munky.instantiated.util.VectorUtil;
import io.papermc.paper.util.Tick;
import net.kyori.adventure.title.Title;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class InstancedDungeonRoom implements RoomLike<InstancedDungeon,ConvexPolyhedralRegion>, Instantiable<DungeonRoom> {
    private ConvexPolyhedralRegion polyRegion;
    private final boolean instanced;
    final InstancedDungeon parent;
    final DungeonRoom master;
    final Map<ImmutableVector,InstancedDungeonDoor> doors = new HashMap<>();
    public InstancedDungeonRoom(@NotNull InstancedDungeon parent, @NotNull DungeonRoom master) {
        this.parent = Objects.requireNonNull(parent);
        this.master = Objects.requireNonNull(master);
        ImmutableLocation realInstanceLocation = parent.getLocation();
        BlockVector3 vec = BlockVector3.at(realInstanceLocation.getX(), realInstanceLocation.getY(), realInstanceLocation.getZ());
        vec = vec.add(this.getOrigin());
        ConvexPolyhedralRegion realRegion = new ConvexPolyhedralRegion(master.getRegion());
        realRegion.shift(vec);
        realRegion.setWorld(FaweAPI.getWorld(realInstanceLocation.getWorld().getName()));
        this.instanced = true;
        this.polyRegion = realRegion;
    }
    public void initDoors(){
        Instantiated.logger().debug("Initiating doors for dungeon room '" + getIdentifier() + "'...");
        try(
                EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder().world(FaweAPI.getWorld(
                        getLocation().getWorld().getName())).build()
        ){
            for (Map.Entry<ImmutableVector, InstancedDungeonDoor> entry : getDoors().entrySet()) {
                InstancedDungeonDoor door = entry.getValue();
                try{
                    door.inject(editSession);
                }catch (Exception e){
                    throw new Exception("Error initiating door '" + door.getMaster().getIdentifier() + "' in dungeon '" + getIdentifier() + "': " + e.getMessage());
                }
            }
        }catch (Exception e){
            Instantiated.logger().severe(e.getMessage());
        }
    }
    public void spawnMobs(){
        Instantiated.logger().debug("Spawning mobs for dungeon room '" + getIdentifier() + "'...");
        try{
            for (Map.Entry<String, DungeonMob> mobEntry : getEntities().entrySet()) {
                Location mobLocation = mobEntry.getValue().getSpawnLocation().toLocation();
                ImmutableLocation thisLocation = this.getLocation();
                Location realLocation = new Location(thisLocation.getWorld(), thisLocation.getX(),thisLocation.getY(),thisLocation.getZ(),mobLocation.getYaw(),mobLocation.getPitch());
                realLocation.add(mobLocation);
                InstancedDungeonMobSpawnEvent event = new InstancedDungeonMobSpawnEvent(this, mobEntry.getValue(), realLocation);
                event.callEvent();
                if (event.isCancelled()) continue;
                LivingEntity toSpawn = event.getLivingEntity();
                LivingEntity spawnedEntity;
                if (toSpawn == null) {
                    spawnedEntity = event.getSpawnLocation().getWorld().spawn(event.getSpawnLocation(), Zombie.class,zomb->{
                        zomb.customName(ComponentUtil.toComponent("<red>Im fucked and need configured"));
                        zomb.setCustomNameVisible(true);
                        zomb.setAI(false);
                        zomb.setSilent(true);
                        Instantiated.logger().warning(
                                "Spawned an un-configured mob with mob id '" +
                                        event.getDungeonMob().getMobIdentifier() + "' in room '" +
                                        getMaster().getIdentifier() + "' in dungeon '" +
                                        getMaster().getParent().getIdentifier() + "' at " +
                                        event.getSpawnLocation()
                        );
                    });
                } else {
                    toSpawn.setPersistent(true);
                    toSpawn.setMetadata("instantiated-persist", new FixedMetadataValue(Instantiated.instance(), true));
                    spawnedEntity = toSpawn;
                }
                // clone so we don't unintentionally mutate the dungeon mob inside the master mob list
                DungeonMob newDungeonMob = event.getDungeonMob().clone();
                newDungeonMob.setLivingEntity(spawnedEntity);
                spawnedEntity.setMetadata(DungeonManager.METADATA_DUNGEON_MOB,new FixedMetadataValue(Instantiated.instance(), Pair.of(this,newDungeonMob)));
                // if the init time doesnt match, then we know the entity is from a different session!
                spawnedEntity.getPersistentDataContainer().set(DungeonManager.NSK_OWNED_ENTITY, PersistentDataType.LONG,Instantiated.INIT_TIME);
                getParent().getActiveMobs().putIfAbsent(getMaster(), new ArrayList<>());
                getParent().getActiveMobs().get(getMaster()).add(newDungeonMob);
            }
        }catch (Exception e){
            Instantiated.logger().severe("Error spawning mobs for dungeon dungeon '" + getIdentifier() + "': " + e.getMessage());
        }
    }
    @Override
    public DungeonRoom getMaster() {
        return this.master;
    }
    @Override
    public ImmutableLocation getLocation() {
        BlockVector3 dungeonLocation = BlockVector3.at(parent.getLocation().getX(),parent.getLocation().getY(),parent.getLocation().getZ());
        BlockVector3 myLocation = dungeonLocation.add(getOrigin());
        return new ImmutableLocation(parent.getLocation().getWorld(),myLocation.getX(),myLocation.getY(),myLocation.getZ());
    }
    @Override
    public Location toRelative(Location location){
        Location worldLocation = new Location(location.getWorld(),location.getX(), location.getY(), location.getZ());
        worldLocation.subtract(getLocation().toLocation());
        return new Location(location.getWorld(),worldLocation.getX(),worldLocation.getY(),worldLocation.getZ(),location.getYaw(),location.getPitch());
    }

    @Override
    public boolean isInstanced() {
        return this.instanced;
    }

    @Override
    public void setRegion(ConvexPolyhedralRegion newRegion) {
        this.polyRegion = newRegion;
    }
    @Override
    public ConvexPolyhedralRegion getRegion() {
        return this.polyRegion;
    }

    @Override
    public Map<String, DungeonMob> getEntities() {
        return getMaster().getEntities();
    }
    public Map<ImmutableVector, InstancedDungeonDoor> getDoors() {
        if (this.doors.isEmpty()){
            for (DungeonDoor value : getMaster().getDoors().values()) {
                InstancedDungeonDoor instanced = value.instance(this);
                this.doors.put(instanced.getLocation().toImmutableVector(),instanced);
            }
        }
        return this.doors;
    }

    @Override
    public BlockVector3 getOrigin() {
        return getMaster().getOrigin();
    }

    @Override
    public String getIdentifier() {
        return getMaster().getIdentifier();
    }

    @Override
    public InstancedDungeon getParent() {
        return this.parent;
    }
    public InstancedDungeonDoor getDoorAtLocation(Location blockLocation){
        for (InstancedDungeonDoor value : getDoors().values()) {
            CuboidRegion reg = value.getRegion().clone();
            reg.contract(BlockVector3.at(-1,-1,-1));
            if (reg.contains(blockLocation.getBlockX(),blockLocation.getBlockY(),blockLocation.getBlockZ())){
                return value;
            }
        }
        return null;
    }

    /**
     * Drops a key at the given location, that is not really a real key and cannot be picked up.
     * @param location real location, not relative
     * @return the spawned item
     */
    public void dropKey(Location location){
        ItemStack keyItem = new ItemStack(getMaster().getKeyMaterial());
        ItemMeta meta = keyItem.getItemMeta();
        meta.displayName(ComponentUtil.toComponent("<black><obfuscated>YouShouldNotSeeThis"));
        keyItem.setItemMeta(meta);
        LivingEntity keyEntity = (LivingEntity) location.getWorld().spawn(location,Item.class, e->{
            e.setCustomNameVisible(true);
            e.customName(ComponentUtil.toComponent("<rainbow>Door key!"));
            e.setCanMobPickup(false);
            e.setUnlimitedLifetime(true);
            e.setMetadata(DungeonManager.METADATA_PERSISTENT_ENTITY,new FixedMetadataValue(Instantiated.instance(),true));
            e.setMetadata(DungeonManager.METADATA_KEY_ENTITY,new FixedMetadataValue(Instantiated.instance(),true));
            e.getPersistentDataContainer().set(DungeonManager.NSK_OWNED_ENTITY, PersistentDataType.LONG,Instantiated.INIT_TIME);
            e.setItemStack(keyItem);
            if (Instantiated.getMainConfig().DROPPED_KEYS_USE_BUKKIT_GLOW){
                e.setGlowing(true);
                Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
                Team team = board.getTeam("instantiated-dropped-key-team");
                if (team==null){
                    team = board.registerNewTeam("instantiated-dropped-key-team");
                }
                // defaults to BLUE
                team.color(Instantiated.getMainConfig().DROPPED_KEYS_GLOW_COLOR);
                team.addEntity(e);
            }
        });
        Bukkit.getScheduler().runTaskLater(Instantiated.instance(),()->{
            if (keyEntity.isInWorld() || !keyEntity.isDead() || keyEntity.isTicking()){
                keyEntity.remove();
                this.getParent().incrementDoorKeys();
                Title keyPickupTitle = Title.title(
                        ComponentUtil.toComponent("<gradient:blue:green:blue>Key picked up!"),
                        ComponentUtil.toComponent("<gray><italic>Current keys: " + this.getParent().getDoorKeys()),
                        Title.Times.times(
                                Duration.of(500, ChronoUnit.MILLIS),
                                Duration.of(2, ChronoUnit.SECONDS),
                                Duration.of(500, ChronoUnit.MILLIS)
                        )
                );
                for (Player player : this.getParent().getOnlinePlayers()) {
                    player.showTitle(keyPickupTitle);
                }
                Instantiated.logger().debug("Key automatically picked up after 15 seconds! Current keys = " + this.getParent().getDoorKeys());
            }
        }, Tick.tick().fromDuration(Duration.of(15, ChronoUnit.SECONDS)));
        Title keyDropTitle = Title.title(
                ComponentUtil.toComponent("<gradient:red:blue:red>Key dropped!"),
                ComponentUtil.toComponent("<gray><italic>Current keys: " + this.getParent().getDoorKeys()),
                Title.Times.times(
                        Duration.of(500, ChronoUnit.MILLIS),
                        Duration.of(2, ChronoUnit.SECONDS),
                        Duration.of(500, ChronoUnit.MILLIS)
                )
        );
        for (Player player : this.getParent().getOnlinePlayers()) {
            player.showTitle(keyDropTitle);
        }
        ListenerFactory.registerEvent(PlayerAttemptPickupItemEvent.class,(e,l)->{
            if (e.getItem().getItemStack().equals(keyItem) && e.getItem().hasMetadata(DungeonManager.METADATA_KEY_ENTITY)){
                e.setCancelled(true);
                e.setFlyAtPlayer(true);
                e.getItem().remove();
                this.getParent().incrementDoorKeys();
                Title keyPickupTitle = Title.title(
                        ComponentUtil.toComponent("<gradient:blue:green:blue>Key picked up!"),
                        ComponentUtil.toComponent("<gray><italic>Current keys: " + this.getParent().getDoorKeys()),
                        Title.Times.times(
                                Duration.of(500, ChronoUnit.MILLIS),
                                Duration.of(2, ChronoUnit.SECONDS),
                                Duration.of(500, ChronoUnit.MILLIS)
                        )
                );
                for (Player player : this.getParent().getOnlinePlayers()) {
                    player.showTitle(keyPickupTitle);
                }
                Instantiated.logger().debug("Player '" + e.getPlayer().getName() + "' picked up a key! Current keys = " + this.getParent().getDoorKeys());
                HandlerList.unregisterAll(l);
            }
        });
        Instantiated.logger().debug("Spawned key at " + location);
    }
    public boolean registerDungeonMobDeath(DungeonMob mob){
        if (mob.getLivingEntity()==null) return false;
        getParent().getActiveMobs().get(getMaster()).remove(mob);
        Instantiated.logger().debug("Registered dungeon mob death '" + mob.getMobIdentifier() + "' which is a '" + mob.getLivingEntity().getClass().getSimpleName() + "'");
        if (getMaster().getKeyDropMode() == DungeonRoom.KeyDropMode.MARKED_ROOM_MOB_KILL && mob.isMarked()){
            // if the mob is marked, drop a key...
            dropKey(mob.getLivingEntity().getLocation());
            return true;
        }else if (getMaster().getKeyDropMode() == DungeonRoom.KeyDropMode.ROOM_MOBS_CLEAR && getParent().getActiveMobs().get(getMaster()).isEmpty()){
            // or if there are no more mobs left, drop a key
            dropKey(mob.getLivingEntity().getLocation());
            return true;
        }
        return false;
    }

    /**
     * Can be a little fishy because world edit only does math with integer precision for regions
     * @param location the location to test
     * @return true if this room contains the given location
     */
    public boolean containsLocation(Location location){
        return getRegion().contains(VectorUtil.toBlockVector(location));
    }
}
