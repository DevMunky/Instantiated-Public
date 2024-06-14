package dev.munky.instantiated.dungeon;

import com.fastasyncworldedit.core.extent.transform.PatternTransform;
import com.google.common.collect.ImmutableList;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import dev.munky.instantiated.Instantiated;
import dev.munky.instantiated.dungeon.room.DungeonRoom;
import dev.munky.instantiated.dungeon.room.InstancedDungeonRoom;
import dev.munky.instantiated.dungeon.room.door.InstancedDungeonDoor;
import dev.munky.instantiated.dungeon.room.mob.DungeonMob;
import dev.munky.instantiated.util.Debugging;
import dev.munky.instantiated.util.ImmutableLocation;
import dev.munky.instantiated.util.VectorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class InstancedDungeon implements Instantiable<Dungeon>, DungeonLike<InstancedDungeonRoom> {
    public static final double DEFAULT_DIFFICULTY = 1.0d;
    private boolean instanced;
    private final Dungeon master;
    private Clipboard pastedClipboard;
    public final ImmutableLocation location;
    public double difficulty = DEFAULT_DIFFICULTY;
    private final List<UUID> players = new ArrayList<>();
    private final Map<String,InstancedDungeonRoom> rooms = new HashMap<>();
    private int doorKeys = 0; // the number of keys that are left for the dungeon.
    private final Map<DungeonRoom,List<DungeonMob>> activeMobs = new HashMap<>();
    protected InstancedDungeon(@NotNull Dungeon master, @NotNull Location location) {
        this.master = Objects.requireNonNull(master);
        this.location = ImmutableLocation.of(location);
        this.instanced = false;
    }
    public boolean inject() {
        if (this.instanced) throw new UnsupportedOperationException("This dungeon is already instanced, do not try to do it again");
        Instantiated.logger().debug("Trying to inject a InstancedDungeon of '" + getIdentifier() + "'");
        this.pastedClipboard = pasteSchematic();
        try{
            if (this.pastedClipboard != null) {
                for (Map.Entry<String, DungeonRoom> room : master.getRooms().entrySet()) {
                    InstancedDungeonRoom instancedRoom = room.getValue().instance(this);
                    getRooms().put(room.getKey(), instancedRoom);
                    instancedRoom.spawnMobs();
                    instancedRoom.initDoors();
                    this.instanced = true;
                }
                List<InstancedDungeon> dungeons = DungeonManager.INSTANCE.getInstancedDungeons().get(getIdentifier());
                if (dungeons == null)
                    dungeons = new ArrayList<>();
                dungeons.add(this);
                DungeonManager.INSTANCE.getInstancedDungeons().put(getIdentifier(), dungeons);
                DungeonManager.INSTANCE.getInstancedDungeonLocations().put(location, this);
                return true;
            }else
                throw new RuntimeException("Could not paste schematic '" + this.getSchematic().getName() + "'");
        }catch (Exception e){
            Instantiated.logger().severe("Could not inject an dungeon of '" + getIdentifier() + "':\n" + e.getMessage());
        }
        return false;
    }
    private Clipboard pasteSchematic(){
        Chunk chunk = getLocation().getWorld().getChunkAt((int) getLocation().getX(),(int) getLocation().getZ());

        if (chunk.getLoadLevel() == Chunk.LoadLevel.UNLOADED){
            chunk.load();
            Instantiated.logger().debug("Loaded chunk at " + chunk.getX() + " " + chunk.getZ());
        }

        ClipboardFormat clipboardFormat = ClipboardFormats.findByFile(this.getMaster().getSchematic());
        Clipboard clipboard = null;
        BlockVector3 blockVector3 = BlockVector3.at(getLocation().getX(), getLocation().getY(), getLocation().getZ());

        if (clipboardFormat != null) {
            try (ClipboardReader clipboardReader = clipboardFormat.getReader(new FileInputStream(this.master.getSchematic()))) {

                EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder().world(BukkitAdapter.adapt(location.getWorld()))
                        .fastMode(true)
                        .checkMemory(false)
                        .build();

                clipboard = clipboardReader.read();

                BlockVector3 origin = clipboard.getOrigin();

                Operation operation = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .copyEntities(false)
                        .to(blockVector3)
                        .ignoreAirBlocks(false)
                        .build();

                try {
                    Operations.complete(operation);
                    clipboard = editSession.lazyCopy(clipboard.getRegion());
                    clipboard.setOrigin(origin);
                    editSession.close();
                    Instantiated.logger().debug("Injected InstancedDungeon '%s' , modifying ".formatted(this) + clipboard.getVolume() + " blocks");
                } catch (WorldEditException e) {
                    Instantiated.logger().warning("WorldEditException from dungeon '" + super.toString() + "'");
                    e.printStackTrace();
                }
            } catch (IOException e) {
                Instantiated.logger().warning("FileInputStream error from dungeon '" + super.toString() + "'");
                e.printStackTrace();
            }
        }else{
            Instantiated.logger().warning("No schematic found by file name '" + getMaster().getSchematic() + "'");
        }
        return clipboard;
    }
    public void remove(){
        try{
            if (this.instanced && WorldEdit.getInstance().getPlatformManager().isInitialized()) { // second condition stops an unchecked exception while disabling
                try (
                        EditSession session = WorldEdit.getInstance().newEditSessionBuilder()
                                .world(BukkitAdapter.adapt(location.getWorld()))
                                .fastMode(true)
                                .checkMemory(false)
                                .build()
                ) {
                    Operation operation = new ClipboardHolder(pastedClipboard)
                            .createPaste(new PatternTransform(session.extent, BlockTypes.AIR))
                            .to(BlockVector3.at(location.getX(), location.getY(), location.getZ()))
                            .ignoreAirBlocks(false)
                            .copyEntities(false)
                            .build();
                    try {
                        Operations.complete(operation);
                        Instantiated.logger().debug("Removed dungeon of '%s' , modified ".formatted(this.getIdentifier()) + pastedClipboard.getVolume() + " blocks");
                    } catch (WorldEditException e) {
                        e.printStackTrace();
                    }
                    pastedClipboard.close();
                }
            } else {
                Instantiated.logger().warning("Could not remove physical component of dungeon '" + this + "'");
            }
        }catch (Exception e){}
        removeMobs();
        removeDoors();
        removePlayers();
        getActiveMobs().clear();
        if (!Debugging.getCallerName().equals("cleanup"))
            DungeonManager.INSTANCE.getInstancedDungeons().get(getIdentifier()).remove(this);
        DungeonManager.INSTANCE.getInstancedDungeonLocations().remove(getLocation());
        this.players.clear();
    }
    private void removeDoors(){
        for (InstancedDungeonRoom value : getRooms().values()) {
            for (InstancedDungeonDoor instancedDungeonDoor : value.getDoors().values()) {
                instancedDungeonDoor.remove();
            }
        }
    }
    private void removeMobs(){
        for (List<DungeonMob> mobs : getActiveMobs().values()) {
            for (DungeonMob mob : mobs) {
                if (mob.getLivingEntity() != null)
                    mob.getLivingEntity().remove();
            }
        }
    }
    private void removePlayers(){
        for (UUID uuid : getPlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player!=null){
                player.teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());
            }
        }
    }
    public void teleport(@NotNull Player player){
        Location l = getLocation().toLocation().add(getMaster().getSpawnVector());
        double oldY = l.getY();
        l.setY(l.getY()+5.5);
        RayTraceResult result = l.getWorld().rayTrace(
                l,
                new Vector(0,-1,0),
                255,
                FluidCollisionMode.ALWAYS,
                true,
                1,
                null
        );
        if (result!=null){
            l.setY(result.getHitPosition().getY() + 0.25d);
        }else{
            l.setY(oldY + 0.25d);
        }
        player.teleport(l);
    }
    public void addPlayers(@NotNull Collection<Player> players){
        players.forEach(this::addPlayer);
    }
    public void addPlayer(@NotNull Player player){
        InstancedDungeon instancedDungeon = Instantiated.getDungeonManager().getInstancedDungeon(player);;
        if (instancedDungeon!=null && instancedDungeon!=this){
            instancedDungeon.removePlayer(player);
        }
        teleport(player);
        this.players.add(player.getUniqueId());
        Instantiated.logger().debug("Added player " + player.getName() + " to dungeon '" + this + "'");
    }
    public void removePlayer(@NotNull Player player){
        removePlayer(player.getUniqueId());
    }
    public void removePlayer(@NotNull UUID player){
        this.players.remove(player);
        if (getPlayers().isEmpty()){
            remove();
            Instantiated.logger().info("Removed instanced dungeon '%s' , no player's were left".formatted(getIdentifier()));
        }
    }
    public Map<DungeonRoom,List<DungeonMob>> getActiveMobs(){ // should be InstancedDungeonRooms, not this key
        return this.activeMobs;
    }
    public @NotNull List<UUID> getPlayers(){
        return ImmutableList.copyOf(this.players);
    }
    public @NotNull List<Player> getOnlinePlayers(){
        return getPlayers().stream().map(Bukkit::getPlayer).filter(Objects::nonNull).toList();
    }
    public @Nullable InstancedDungeonRoom getClosestRoom(Player player){
        InstancedDungeonRoom closest = getRooms().values().stream().findFirst().orElse(null);
        Vector3 playerVector = Vector3.at(player.getX(), player.getY(), player.getZ());
        if (closest==null) return null;
        for (InstancedDungeonRoom room : getRooms().values()) {
            if (
                    room.getRegion().contains(VectorUtil.toBlockVector(player.getLocation()))
                            || room.getRegion().getCenter().distance(playerVector) < closest.getRegion().getCenter().distance(playerVector)
            ){
                closest = room;
            }
        }
        return closest;
    }
    @Override
    public Dungeon getMaster() {
        return this.master;
    }
    @Override
    public ImmutableLocation getLocation() {
        return this.location;
    }
    public Location toRelative(Location location){
        Vector vector = new Vector(location.getX(), location.getY(), location.getZ());
        Vector origin = new Vector(this.location.getX(),this.location.getY(),this.location.getZ());
        vector.subtract(origin);
        return new Location(location.getWorld(),vector.getX(),vector.getY(),vector.getZ(),location.getYaw(),location.getPitch());
    }
    public int getDoorKeys(){
        return this.doorKeys;
    }
    public void incrementDoorKeys(){
        this.doorKeys++;
    }
    public void decrementDoorKeys(){
        if (doorKeys == 0) throw new IllegalStateException("Current number of keys equals 0, and the number of keys cannot be negative");
        else {
            this.doorKeys--;
        }
    }
    @Override
    public boolean isInstanced() {
        return this.instanced;
    }
    public String toString(){
        return "InstancedDungeon-"+getIdentifier()+"["+ getLocation().getWorld().getName()+","+ getLocation().getX()+","+ getLocation().getY()+","+ getLocation().getZ()+"]@"+hashCode();
    }

    @Override
    public Map<String, InstancedDungeonRoom> getRooms() {
        return this.rooms;
    }

    @Override
    public String getIdentifier() {
        return getMaster().getIdentifier();
    }

    @Override
    public File getSchematic() {
        return getMaster().getSchematic();
    }
    @Override
    public Vector getSpawnVector(){
        return getMaster().getSpawnVector();
    }
}
