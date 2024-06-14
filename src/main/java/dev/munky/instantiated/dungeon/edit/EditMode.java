package dev.munky.instantiated.dungeon.edit;

import com.fastasyncworldedit.core.FaweAPI;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.ConvexPolyhedralRegion;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.world.World;
import dev.munky.instantiated.Instantiated;
import dev.munky.instantiated.event.ListenerFactory;
import dev.munky.instantiated.dungeon.DungeonManager;
import dev.munky.instantiated.dungeon.InstancedDungeon;
import dev.munky.instantiated.dungeon.room.InstancedDungeonRoom;
import dev.munky.instantiated.dungeon.room.door.DungeonDoor;
import dev.munky.instantiated.dungeon.room.door.InstancedDungeonDoor;
import dev.munky.instantiated.dungeon.room.mob.DungeonMob;
import dev.munky.instantiated.util.ComponentUtil;
import dev.munky.instantiated.util.ImmutableVector;
import dev.munky.instantiated.util.VectorUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class EditMode {
    public static List<Player> playersInEditMode = new ArrayList<>();
    public static Map<UUID, ItemStack[]> previousInventory = new HashMap<>();
    public static boolean unsavedChanges = false;
    static{
        Events: {
            if (Instantiated.instance().isEnabled()){
                ListenerFactory.registerEvent(PlayerInteractEvent.class, EventPriority.LOWEST, e->{
                    if (e.getHand() != EquipmentSlot.HAND) return;
                    InstancedDungeon dungeon = DungeonManager.INSTANCE.getInstancedDungeon(e.getPlayer());
                    if (dungeon==null) return;
                    InstancedDungeonRoom room = dungeon.getClosestRoom(e.getPlayer());
                    if (room==null) return;
                    Location point = e.getInteractionPoint();
                    if (point==null && e.getClickedBlock()!=null) point = e.getClickedBlock().getLocation();
                    ToolInteraction interaction = new ToolInteraction(e,dungeon,room,point);
                    ToolFunctionality.execute(interaction);
                });
            }
        }
    }
    private record ToolInteraction(
            PlayerInteractEvent event,
            InstancedDungeon instancedDungeon,
            InstancedDungeonRoom instancedRoom,
            Location interactionLocation
    ){}
    private enum ToolFunctionality{
        DOOR(
                (cond)->{
                    // none
                    return true;
                },
                (rightClick)->{
                    Actor actor = BukkitAdapter.adapt(rightClick.event.getPlayer());
                    LocalSession session = WorldEdit.getInstance().getSessionManager().get(actor);
                    World selectionWorld = session.getSelectionWorld();
                    Region region;
                    try {
                        if (selectionWorld == null) return;
                        region = session.getSelection(selectionWorld);
                        if (!(region instanceof CuboidRegion)){
                            rightClick.event.getPlayer().sendMessage(ComponentUtil.toComponent("<red>Please only use cuboid regions for doors"));
                            return;
                        }
                    } catch (Exception ex) {
                        actor.printError(TextComponent.of("Please make a region selection first."));
                        return;
                    }
                    Instantiated.logger().debug("selected region " + region);
                    rightClick.event.getPlayer().sendMessage(ComponentUtil.toComponent("<green>What should the id be for this door? </green><red><italic>(send 'cancel' or 'n' to cancel)"));
                    ListenerFactory.registerEvent(AsyncChatEvent.class,(e,l)->{
                        if (e.getPlayer() != rightClick.event.getPlayer()) return;
                        e.setCancelled(true);
                        String identifier = ComponentUtil.toString(e.message());
                        if (identifier.equals("cancel") || identifier.equals("n")){
                            e.getPlayer().sendMessage(ComponentUtil.toComponent("<red>Cancelled door creation"));
                            return;
                        }
                        rightClick.event.getPlayer().sendMessage(ComponentUtil.toComponent("<green>What should the base material be for this door? </green><red><italic>(send 'cancel' or 'n' to cancel)"));
                        ListenerFactory.registerEvent(AsyncChatEvent.class,(ev,li)->{
                            if (ev.getPlayer() != rightClick.event.getPlayer()) return;
                            ev.setCancelled(true);
                            String sBaseMaterial = ComponentUtil.toString(ev.message());
                            if (sBaseMaterial.equals("cancel") || sBaseMaterial.equals("n")){
                                ev.getPlayer().sendMessage(ComponentUtil.toComponent("<red>Cancelled door creation"));
                                return;
                            }
                            Material baseMaterial = Material.matchMaterial(sBaseMaterial);
                            if (baseMaterial==null){
                                ev.getPlayer().sendMessage(ComponentUtil.toComponent("<red>Material '" + sBaseMaterial + "' not found"));
                            }else{
                                region.shift(rightClick.instancedRoom.getLocation().toImmutableVector().toBlockVector3().multiply(-1));
                                region.expand(BlockVector3.at(1,1,1));
                                DungeonDoor door = new DungeonDoor(
                                        rightClick.instancedDungeon.getMaster(),
                                        identifier,
                                        (CuboidRegion) region, // safe cast because of check earlier
                                        BlockVector3.ZERO,
                                        false,
                                        "created-through-in-game-editor",
                                        baseMaterial,
                                        Material.AIR,
                                        "created-through-in-game-editor"
                                );
                                InstancedDungeonDoor newDoor = door.instance(rightClick.instancedRoom);
                                try (
                                        EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder().world(FaweAPI.getWorld(
                                                rightClick.event.getPlayer().getWorld().getName())).build()
                                ) {
                                    newDoor.inject(editSession);
                                }
                                rightClick.instancedRoom.getDoors().put(new ImmutableVector(0.0,0.0,0.0),newDoor);
                                rightClick.instancedRoom.getMaster().getDoors().put(new ImmutableVector(0.0,0.0,0.0),door);
                                e.getPlayer().sendMessage(ComponentUtil.toComponent("<green>Created new door!"));
                            }
                            HandlerList.unregisterAll(li);
                        });
                        HandlerList.unregisterAll(l);
                    });
                },
                (leftClick)->{
                    if (leftClick.event.getClickedBlock()==null){
                        leftClick.event.getPlayer().sendMessage(ComponentUtil.toComponent("<red>Please click a block"));
                        return;
                    }
                    Location interaction = leftClick.event.getClickedBlock().getLocation();
                    InstancedDungeonDoor door = leftClick.instancedRoom.getDoorAtLocation(interaction);
                    if (door!=null){
                        door.getParent().getDoors().remove(ImmutableVector.of(door.getOrigin()));
                        door.getParent().getMaster().getDoors().remove(ImmutableVector.of(door.getOrigin()));
                        door.remove();
                        leftClick.event.getPlayer().sendMessage(ComponentUtil.toComponent("<green>Removed door '" + door.getMaster().getIdentifier() + "'"));
                    }else{
                        leftClick.event.getPlayer().sendMessage(ComponentUtil.toComponent("<red>Please click a door"));
                    }
                }
        ),
        ENTITY(
                (cond)->{
                    // no conditions
                    return true;
                },
                (rightClick)->{
                    Location interaction = rightClick.event.getPlayer().getLocation();
                    rightClick.event.getPlayer().sendMessage(ComponentUtil.toComponent("<green>What should the id be for this mob? </green><red><italic>(send 'cancel' or 'n' to cancel)"));
                    long start = System.currentTimeMillis();
                    ListenerFactory.registerEvent(AsyncChatEvent.class,(e,l)->{
                        if (e.getPlayer() != rightClick.event.getPlayer()) return;
                        String response = ComponentUtil.toString(e.message());
                        if (System.currentTimeMillis() - start >= 15000){ // 15 seconds
                            return;
                        }else if (response.equals("cancel") || response.equals("n")){
                            e.setCancelled(true);
                            e.getPlayer().sendMessage(ComponentUtil.toComponent("<red>Cancelled mob creation"));
                        }else{
                            e.setCancelled(true);
                            interaction.subtract(rightClick.instancedRoom.getLocation().toLocation());
                            DungeonMob newMob = new DungeonMob(response,interaction,false, new HashMap<>());
                            rightClick.instancedRoom.getMaster().getEntities().put(response,newMob);
                        }
                        HandlerList.unregisterAll(l);
                    });
                },
                (leftClick)->{
                    Location interaction = leftClick.event.getPlayer().getLocation();
                    Location def = interaction.clone().multiply(64);
                    Location closest = def;
                    String closestMobId = "";
                    for (Map.Entry<String, DungeonMob> entry : leftClick.instancedRoom.getEntities().entrySet()) {
                        Location spawn = entry.getValue().getSpawnLocation().toLocation();
                        double dist = spawn.distance(interaction);
                        if (dist <= 2 && dist < spawn.distance(closest)){
                            closest = spawn;
                            closestMobId = entry.getValue().getMobIdentifier();
                        }
                    }
                    if (closest == def) return;
                    leftClick.instancedRoom.getMaster().getEntities().remove(closestMobId);
                    leftClick.event.getPlayer().sendMessage(ComponentUtil.toComponent("<green>Removed entity '" + closestMobId + "' from room '" + leftClick.instancedRoom.getIdentifier() + "'"));
                    Instantiated.logger().debug("Removed entity '" + closestMobId + "' from room '" + leftClick.instancedRoom.getIdentifier() + "'");
                }
        ),
        VERTEX(
                (cond)->{
                    // no conditions
                    return true;
                },
                (rightClick)->{
                    Location interaction = rightClick.interactionLocation==null ? rightClick.event.getPlayer().getLocation() : rightClick.interactionLocation;
                    rightClick.instancedRoom.getRegion().addVertex(VectorUtil.toBlockVector(interaction));
                    ConvexPolyhedralRegion newRegion = new ConvexPolyhedralRegion(rightClick.instancedRoom.getRegion());
                    newRegion.shift(VectorUtil.toBlockVector(rightClick.instancedRoom.getLocation().toLocation()).multiply(-1));
                    rightClick.instancedRoom.getMaster().setRegion(newRegion); // update the master 'template'
                    rightClick.event.getPlayer().sendMessage(ComponentUtil.toComponent("<green>Added vertex at " + interaction.toVector()));
                    Instantiated.logger().debug("Added vertex at " + interaction.toVector() + " from room '" + rightClick.instancedRoom.getIdentifier() + "' in '" + rightClick.instancedDungeon.getIdentifier() + "'");
                },
                (leftClick)->{
                    Location interaction = leftClick.interactionLocation==null ? leftClick.event.getPlayer().getLocation() : leftClick.interactionLocation;
                    BlockVector3 vInteraction = VectorUtil.toBlockVector(interaction);
                    BlockVector3 def = vInteraction.multiply(20).subtract(vInteraction.multiply(-2)).divide(2); // random ass thing
                    // gets the closest vertex by sorting the stream, then finding the first one
                    BlockVector3 closest = def;
                    for (BlockVector3 vertex : leftClick.instancedRoom.getRegion().getVertices()) {
                        double dist = vInteraction.distance(vertex);
                        if (dist <= 2.5 && dist < vInteraction.distance(closest)) closest = vertex;
                    }
                    if (Objects.equals(closest, def)) return;
                    List<BlockVector3> vertices = new ArrayList<>(leftClick.instancedRoom.getRegion().getVertices());
                    vertices.remove(closest);
                    ConvexPolyhedralRegion newRegion = new ConvexPolyhedralRegion(FaweAPI.getWorld(leftClick.instancedRoom.getLocation().getWorld().getName()));
                    for (BlockVector3 vertex : vertices) {
                        newRegion.addVertex(vertex);
                    }
                    leftClick.instancedRoom.setRegion(newRegion); // sets the current dungeon
                    newRegion.shift(VectorUtil.toBlockVector(leftClick.instancedRoom.getLocation().toLocation()).multiply(-1));
                    leftClick.instancedRoom.getMaster().setRegion(newRegion); // as well as the master 'template'
                    leftClick.event.getPlayer().sendMessage(ComponentUtil.toComponent("<green>Removed vertex at " + closest));
                    Instantiated.logger().debug("Removed vertex at " + closest + " from room '" + leftClick.instancedRoom.getIdentifier() + "' in '" + leftClick.instancedDungeon.getIdentifier() + "'");
                }
        );
        private final Predicate<ToolInteraction> condition;
        private final Consumer<ToolInteraction> rightClick;
        private final Consumer<ToolInteraction> leftClick;
        /**
         *
         * @param condition if false, code execution stops
         * @param rightClick
         * @param leftClick
         */
        ToolFunctionality(
                Predicate<ToolInteraction> condition,
                Consumer<ToolInteraction> rightClick,
                Consumer<ToolInteraction> leftClick
        ){
            this.condition = condition;
            this.rightClick = rightClick;
            this.leftClick = leftClick;
        }
        private void instanceExecute(ToolInteraction ti){
            if (this.condition.test(ti)){
                EditMode.unsavedChanges = true;
                if (ti.event.getAction() == Action.RIGHT_CLICK_AIR || ti.event.getAction() == Action.RIGHT_CLICK_BLOCK){
                    this.rightClick.accept(ti);
                }else if (ti.event.getAction() == Action.LEFT_CLICK_AIR || ti.event.getAction() == Action.LEFT_CLICK_BLOCK){
                    this.leftClick.accept(ti);
                }
            }
        }
        public static void execute(ToolInteraction ti){
            ItemStack active = ti.event().getItem();
            if (active==null) return;
            ToolStack tool = ToolStack.getFromItemStack(active);
            if (tool!=null){
                try {
                    ToolFunctionality func = ToolFunctionality.valueOf(tool.toString());
                    ti.event.setCancelled(true);
                    func.instanceExecute(ti);
                } catch (IllegalArgumentException e) {
                    Instantiated.logger().severe("This is should typically not be an issue");
                    e.printStackTrace();
                }
            }
        }
    }

    public static void putInEditMode(Player player){
        playersInEditMode.add(player);
        previousInventory.put(player.getUniqueId(),player.getInventory().getContents().clone());
        player.getInventory().clear();
        player.getInventory().setItem(0, ToolStack.VERTEX.getItem());
        player.getInventory().setItem(2, ToolStack.ENTITY.getItem());
        player.getInventory().setItem(4, ToolStack.DOOR.getItem());
    }
    public static void takeOutOfEditMode(Player player){
        playersInEditMode.remove(player);
        player.getInventory().clear();
        player.getInventory().setContents(previousInventory.get(player.getUniqueId()));
        previousInventory.remove(player.getUniqueId());
    }
    public enum ToolStack {
        DOOR(()->{
            ItemStack vertex = new ItemStack(Material.STICK);
            vertex.addUnsafeEnchantment(Enchantment.MENDING, 69);
            ItemMeta meta = vertex.getItemMeta();
            meta.displayName(ComponentUtil.toComponent("<rainbow>Dungeon door creation tool"));
            meta.lore(new ArrayList<>(List.of(
                    ComponentUtil.toComponent("<gray>Right click to create door using selection"),
                    ComponentUtil.toComponent("<gray>Left click to remove clicked door")
            )));
            meta.getPersistentDataContainer().set(DungeonManager.NSK_EDIT_TOOL, PersistentDataType.STRING,"DOOR");
            vertex.setItemMeta(meta);
            return vertex;
        }),
        VERTEX(()->{
            ItemStack vertex = new ItemStack(Material.STICK);
            vertex.addUnsafeEnchantment(Enchantment.DURABILITY, 69);
            ItemMeta meta = vertex.getItemMeta();
            meta.displayName(ComponentUtil.toComponent("<green>Dungeon room vertex move tool"));
            meta.lore(new ArrayList<>(List.of(
                    ComponentUtil.toComponent("<gray>Right click to move nearest vertex")
            )));
            meta.getPersistentDataContainer().set(DungeonManager.NSK_EDIT_TOOL, PersistentDataType.STRING,"VERTEX");
            vertex.setItemMeta(meta);
            return vertex;
        }),
        ENTITY(()->{
            ItemStack entity = new ItemStack(Material.STICK);
            entity.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 69);
            ItemMeta meta = entity.getItemMeta();
            meta.displayName(ComponentUtil.toComponent("<yellow>Dungeon room mob tool"));
            meta.lore(new ArrayList<>(List.of(
                    ComponentUtil.toComponent("<gray>Right click to add mob at your location"),
                    ComponentUtil.toComponent("<gray>Left click to delete mobs at clicked block")
            )));
            meta.getPersistentDataContainer().set(DungeonManager.NSK_EDIT_TOOL, PersistentDataType.STRING,"ENTITY");
            entity.setItemMeta(meta);
            return entity;
        });
        private final ItemStack stack;
        ToolStack(Supplier<ItemStack> stack) {
            this.stack = stack.get();
        }
        public ItemStack getItem(){
            return this.stack;
        }
        public static ToolStack getFromItemStack(ItemStack item){
            if (item.getItemMeta()==null) return null;
            String editTool = item.getItemMeta().getPersistentDataContainer().get(DungeonManager.NSK_EDIT_TOOL,PersistentDataType.STRING);
            if (editTool==null) return null;
            for (ToolStack value : ToolStack.values()) {
                if (editTool.equals(value.toString())){
                    return value;
                }
            }
            return null;
        }
    }
}
