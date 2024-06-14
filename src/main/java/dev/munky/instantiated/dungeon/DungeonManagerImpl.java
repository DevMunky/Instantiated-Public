package dev.munky.instantiated.dungeon;

import dev.munky.instantiated.Instantiated;
import dev.munky.instantiated.event.InstancedDungeonStartEvent;
import dev.munky.instantiated.event.ListenerFactory;
import dev.munky.instantiated.dungeon.edit.EditMode;
import dev.munky.instantiated.dungeon.room.InstancedDungeonRoom;
import dev.munky.instantiated.dungeon.room.door.InstancedDungeonDoor;
import dev.munky.instantiated.dungeon.room.mob.DungeonMob;
import dev.munky.instantiated.util.ComponentUtil;
import dev.munky.instantiated.util.ImmutableLocation;
import dev.munky.instantiated.world.VoidGenerator;
import net.kyori.adventure.util.TriState;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

public class DungeonManagerImpl implements DungeonManager {
    private static boolean instanced = false;
    private final Map<String, Dungeon> instanceMap = new HashMap<>();
    private final Map<String,List<InstancedDungeon>> realInstanceMap = new HashMap<>();
    private final Map<ImmutableLocation, InstancedDungeon> locationRealInstanceMap = new HashMap<>();
    private ItemStack keyItem = new ItemStack(Material.IRON_INGOT);
    private World instancingWorld = null;
    DungeonManagerImpl(){
        if (instanced) throw new UnsupportedOperationException("Cannot instantiate a new Dungeon Manager. Use Instantiated#getDungeonManager().");
        instanced = true;
        Events: {
            ListenerFactory.registerEvent(PlayerTeleportEvent.class, e -> {
                if (getDungeonWorld() != null && e.getFrom().getWorld() == getDungeonWorld() && e.getTo().getWorld() != getDungeonWorld()) {
                    InstancedDungeon instance = getInstancedDungeon(e.getPlayer());
                    if (instance!=null){
                        Instantiated.logger().debug("Removed player for teleporting outside of instancing world");
                        instance.removePlayer(e.getPlayer());
                    }
                }
            });
            ListenerFactory.registerEvent(PlayerJoinEvent.class, e -> {
                Player player = e.getPlayer();
                InstancedDungeon i;
                if ((i = getInstancedDungeon(player))!=null){
                    i.teleport(player);
                }else if (player.getWorld() == instancingWorld) {
                    player.teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());
                    Instantiated.logger().debug("Moved player out of instanced world because they joined while not in dungeon");
                }
            });
            // removing players when they quit the game
            ListenerFactory.registerEvent(PlayerQuitEvent.class,e->{
                UUID uuid = e.getPlayer().getUniqueId();
                InstancedDungeon dungeon = getInstancedDungeon(e.getPlayer());
                if (dungeon!=null){
                    Bukkit.getScheduler().runTaskLater(Instantiated.instance(),()->{
                        Player player = Bukkit.getPlayer(uuid);
                        if (player==null || !player.isOnline()){
                            dungeon.removePlayer(uuid);
                            Instantiated.logger().debug("Removed player from dungeon due to timeout");
                        }
                    },100); // currently five seconds
                }
            });
            // listening for deaths to count keys and whatnot
            ListenerFactory.registerEvent(EntityDamageEvent.class,e->{
                Entity entity = e.getEntity();
                if (entity instanceof LivingEntity livingEntity) {
                    if (livingEntity.getHealth() - e.getFinalDamage() > 0) return;
                    if (livingEntity.hasMetadata(METADATA_DUNGEON_MOB)) {
                        Pair<InstancedDungeonRoom, DungeonMob> pair = (Pair<InstancedDungeonRoom, DungeonMob>) livingEntity.getMetadata(METADATA_DUNGEON_MOB).getFirst().value();
                        if (pair==null) return;
                        pair.getLeft().registerDungeonMobDeath(pair.getRight());
                    }
                }
            });
            // for door key interactions
            ListenerFactory.registerEvent(PlayerInteractEvent.class, e->{
                if (
                        e.getClickedBlock()!=null &&
                                e.getAction() == Action.RIGHT_CLICK_BLOCK &&
                                e.getHand() == EquipmentSlot.HAND // stops two events from being listened, one for each hand.
                ){
                    Player player = e.getPlayer();
                    Location clickedBlock = e.getClickedBlock().getLocation();
                    InstancedDungeon dungeon = Instantiated.getDungeonManager().getInstancedDungeon(player);
                    if (dungeon==null) return;
                    InstancedDungeonRoom room = dungeon.getClosestRoom(player);
                    if (room==null) return;
                    InstancedDungeonDoor door = room.getDoorAtLocation(clickedBlock);
                    if (door==null) return;
                    e.setCancelled(true);
                    if (dungeon.getDoorKeys()<=0){
                        player.sendActionBar(ComponentUtil.toComponent("<red>You need a <aqua><italic>key</italic></aqua> to open this door!"));
                        return;
                    }
                    door.open(true);
                }
            });
            // if player is in a dungeon, cancel block events
            CancelBlockEventsInDungeon:{
                ListenerFactory.registerEvent(BlockBreakEvent.class, e -> {
                    Player player = e.getPlayer();
                    InstancedDungeon dungeon = Instantiated.getDungeonManager().getInstancedDungeon(player);
                    if (dungeon == null) return;
                    if (!EditMode.playersInEditMode.contains(player)) e.setCancelled(true);
                });
                ListenerFactory.registerEvent(BlockPlaceEvent.class, e -> {
                    Player player = e.getPlayer();
                    InstancedDungeon dungeon = Instantiated.getDungeonManager().getInstancedDungeon(player);
                    if (dungeon == null) return;
                    if (!EditMode.playersInEditMode.contains(player)) e.setCancelled(true);
                });
            }
        }
        Task:{
            // Entity check task
            Bukkit.getScheduler().runTaskTimer(Instantiated.instance(),()->{
                for (Entity entity : getDungeonWorld().getEntities()) {
                    if (entity.getPersistentDataContainer().has(DungeonManager.NSK_OWNED_ENTITY, PersistentDataType.LONG)){
                        Long initTime = entity.getPersistentDataContainer().get(DungeonManager.NSK_OWNED_ENTITY, PersistentDataType.LONG);
                        if (initTime != null && initTime != Instantiated.INIT_TIME){
                            entity.remove();
                            Instantiated.logger().debug("Removed a mob from another session (" + Instant.ofEpochMilli(initTime).atZone(ZoneId.of("EST")) + ")");
                        }
                    }
                    if (entity.hasMetadata(METADATA_PERSISTENT_ENTITY)){
                        entity.setTicksLived(6);
                    }
                    if (entity.hasMetadata(METADATA_KILL_ON_SIGHT)){
                        entity.remove();
                        Instantiated.logger().debug("Removed an entity marked as kill on sight");
                    }
                }
            },0,40);
        }
    }
    public boolean startDungeon(String identifier, Player... players){
        Location location = nextLocation(getInstancedDungeonLocations().size());
        Dungeon dungeon = getDungeons().get(identifier);
        if (dungeon ==null) return false;
        InstancedDungeon realInstance = new InstancedDungeon(dungeon,location);
        InstancedDungeonStartEvent event = new InstancedDungeonStartEvent(realInstance,location,players);
        event.callEvent();
        if (event.isCancelled()) {
            Instantiated.logger().debug("Event was cancelled, so injection was stopped");
            return false;
        }
        realInstance.inject();
        if (realInstance.isInstanced()){
            Instantiated.logger().info("Successfully created a dungeon of " + dungeon.getIdentifier());
            realInstance.addPlayers(Arrays.stream(players).toList());
            return true;
        }else{
            Instantiated.logger().debug("The dungeon could not be instanced, because dungeon#isInstanced() returned false.");
            return false;
        }
    }
    public void createDungeonWorld(){
        World dungeonWorld = Bukkit.getWorld(Instantiated.getMainConfig().DUNGEON_WORLD_NAME);
        if (dungeonWorld==null && !Bukkit.getWorlds().isEmpty()){
            WorldCreator creator = WorldCreator.name(Instantiated.getMainConfig().DUNGEON_WORLD_NAME);
            creator.generator(new VoidGenerator());
            creator.keepSpawnLoaded(TriState.FALSE);
            creator.environment(World.Environment.NORMAL);
            creator.generateStructures(false);
            creator.type(WorldType.FLAT);
            World newWorld = creator.createWorld();
            if (newWorld!=null){
                newWorld.setViewDistance(5);
                newWorld.setAutoSave(false);
                newWorld.setGameRule(GameRule.DO_MOB_SPAWNING,false);
                instancingWorld = newWorld;
                Instantiated.logger().debug("Created new instancing world");
            }else{
                Instantiated.logger().warning("Could not create a new world based on config value 'dungeon.world'");
            }
        }else if (dungeonWorld!=null) {
            for (Entity entity : dungeonWorld.getEntities()) {
                if (entity.hasMetadata(METADATA_DELETE_UPON_LOAD)){
                    entity.remove();
                    Instantiated.logger().debug("Removed a mob marked to be deleted upon load: " + ComponentUtil.toString(entity.customName()));
                }
            }
        }
    }
    public InstancedDungeon getInstancedDungeon(Player player){
        UUID uuid = player.getUniqueId();
        for (List<InstancedDungeon> value : realInstanceMap.values()) {
            for (InstancedDungeon realInstance : value) {
                if (realInstance.getPlayers().contains(uuid))
                    return realInstance;
            }
        }
        return null;
    }
    private Location nextLocation(int index){
        int x = 0;  // center x
        int z = 0;  // center z
        var d = "right";
        int n = 1;
        Location finalLocation = getDungeonWorld().getSpawnLocation();
        // gets the location for this number index, in the spiral. Index 0 would be the first spot at the starting location, and index 1 would be the next spot
        for (int i = 0; i < index; i++) {
            // change the direction
            if (i == Math.pow(n, 2) - n) {
                d = "right";
            } else if (i == Math.pow(n, 2)) {
                d = "down";
            } else if (i == Math.pow(n, 2) + n) {
                d = "left";
            } else if (i == Math.pow(n, 2) + (n * 2 + 1)) {
                d = "up";
                n += 2;
            }
            // get the current x and y.
            switch (d) {
                case "right" -> x += DungeonManager.GRID_SIZE;
                case "left" -> x -= DungeonManager.GRID_SIZE;
                case "down" -> z += DungeonManager.GRID_SIZE;
                default -> z -= DungeonManager.GRID_SIZE;
            }
            finalLocation = new Location(finalLocation.getWorld(),x,finalLocation.getY(),z);
        }
        for (List<InstancedDungeon> value : DungeonManager.INSTANCE.getInstancedDungeons().values()) {
            Location finalLocation1 = finalLocation;
            if (value.stream().map(InstancedDungeon::getLocation).anyMatch(l->l.toLocation().equals(finalLocation1))){
                finalLocation = nextLocation(index - 1);
            }
        }
        return finalLocation;
    }
    public void cleanup(){
        if (Bukkit.getPluginManager().isPluginEnabled("FastAsynchronousWorldEdit")) {
            for (InstancedDungeon instance : getInstancedDungeonLocations().values()) {
                for (UUID uuid : instance.getPlayers()) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline())
                        player.teleport(getDungeonWorld().getSpawnLocation());
                    instance.removePlayer(uuid); // probably fine because i delete and recreate world on startup anyhow
                }
            }
        }
        List<Player> playersInEditMode = new ArrayList<>(EditMode.playersInEditMode);
        for (Player player : playersInEditMode) {
            EditMode.takeOutOfEditMode(player);
        }
        List<InstancedDungeon> instances = new ArrayList<>(locationRealInstanceMap.values());
        for (InstancedDungeon value : instances) {
            value.remove();
        }
        realInstanceMap.clear();
        instanceMap.clear();
        locationRealInstanceMap.clear();
    }
    public World getDungeonWorld(){
        return instancingWorld;
    }
    public Map<String, Dungeon> getDungeons(){
        return instanceMap;
    }
    public Map<String,List<InstancedDungeon>> getInstancedDungeons(){
        return realInstanceMap;
    }
    public Map<ImmutableLocation, InstancedDungeon> getInstancedDungeonLocations(){
        return locationRealInstanceMap;
    }
    public void setDungeonKeyItem(ItemStack key){
        this.keyItem = key;
    }
    public ItemStack getDungeonKeyItem(){
        return this.keyItem;
    }
}
