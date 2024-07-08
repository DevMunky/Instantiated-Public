package dev.munky.instantiated.oldDungeon.edit;

import com.destroystokyo.paper.ParticleBuilder;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.util.nbt.CompoundBinaryTag;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockTypes;
import dev.munky.instantiated.Instantiated;
import dev.munky.instantiated.dungeon.IdentifiableKey;
import dev.munky.instantiated.dungeon.InstancedDungeon;
import dev.munky.instantiated.dungeon.InstancedDungeonDoor;
import dev.munky.instantiated.dungeon.InstancedDungeonRoom;
import dev.munky.instantiated.event.DungeonLoadEvent;
import dev.munky.instantiated.event.ListenerFactory;
import dev.munky.instantiated.oldDungeon.DungeonManager;
import dev.munky.instantiated.dungeon.mob.DungeonMob;
import dev.munky.instantiated.util.ComponentUtil;
import dev.munky.instantiated.util.ImmutableVector;
import dev.munky.instantiated.util.Util;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.sk89q.worldedit.internal.cui.ServerCUIHandler.getMaxServerCuiSize;

public class ParticleRenderer {
    private static final Map<Location, Pair<InstancedDungeonRoom,TextDisplay>> mobIdentifiers = new HashMap<>();
    private static final int[][] TWELVE_EDGES_OF_A_CUBE = { // all 12 edges of a cube
            {0, 1}, {0, 2}, {0, 4},
            {1, 3}, {1, 5},
            {2, 3}, {2, 6},
            {3, 7},
            {4, 5}, {4, 6},
            {5, 7},
            {6, 7}
    };

    static{
        ListenerFactory.registerEvent(DungeonLoadEvent.class,(e)->{
            for (Map.Entry<Location, Pair<InstancedDungeonRoom, TextDisplay>> entry : mobIdentifiers.entrySet()) {
                entry.getValue().getRight().setMetadata(DungeonManager.METADATA_KILL_ON_SIGHT,
                        new FixedMetadataValue(Instantiated.instance(),true)
                ); // this metadata kills the text display if it is ever seen
                entry.getValue().getRight().remove();
            }
        });
    }
    /**
     * Please use asynchronously, this task has explicit support for it
     */
    public static final Consumer<ScheduledTask> TASK = (task) -> {
        try{
            if (Bukkit.getServer().isPrimaryThread()) {
                Instantiated.logger().warning("Expensive particle calculation is being run on the main thread when it is explicitly stated that should be run asynchronously!");
            }
            if (Instantiated.getLoadState().isDisabled()) task.cancel();
            if (!Instantiated.getLoadState().isSafe()) return;
            List<InstancedDungeonRoom> roomsToBeRendered = new ArrayList<>();
            List<InstancedDungeonRoom> selectedRooms = new ArrayList<>();
            for (UUID uuid : Instantiated.instance().getEditModeHandler().getPlayersInEditMode()) {
                Player onlinePlayer = Bukkit.getPlayer(uuid);
                if (onlinePlayer==null) continue;
                Optional<InstancedDungeon> current = DungeonManager.getInstance().getCurrentDungeon(uuid);
                if (current.isEmpty()) continue;
                roomsToBeRendered.addAll(current.get().getRooms().values());
                Optional<InstancedDungeonRoom> closest = current.get().getClosestRoom(onlinePlayer);
                closest.ifPresent(selectedRooms::add);
            }
            roomsToBeRendered = roomsToBeRendered.stream().distinct().toList();
            List<Consumer<World>> toBeSync = new ArrayList<>();
            List<Consumer<Map<Location, Pair<InstancedDungeonRoom, TextDisplay>>>> toDoMobIdentifiers = new ArrayList<>();
            for (Map.Entry<Location, Pair<InstancedDungeonRoom, TextDisplay>> entry : mobIdentifiers.entrySet()) {
                if (roomsToBeRendered.contains(entry.getValue().getLeft())) continue;
                toBeSync.add(world -> {
                    entry.getValue().getRight().setMetadata(DungeonManager.METADATA_KILL_ON_SIGHT,
                          new FixedMetadataValue(Instantiated.instance(), true)
                    ); // this metadata kills the text display if it is ever seen
                    entry.getValue().getRight().remove();
                });
                toDoMobIdentifiers.add(map -> {
                    map.remove(entry.getKey());
                });
            }
            for (Consumer<Map<Location, Pair<InstancedDungeonRoom, TextDisplay>>> toDoMobIdentifier : toDoMobIdentifiers) {
                toDoMobIdentifier.accept(mobIdentifiers);
            }
            for (InstancedDungeonRoom room : roomsToBeRendered) {
                try {
                    toBeSync.addAll(renderRoom(room, selectedRooms.contains(room)));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            Bukkit.getServer().getGlobalRegionScheduler().run(Instantiated.instance(), scheduledTask -> {
                for (Consumer<World> runnable : toBeSync) {
                    runnable.accept(Instantiated.instance().getDungeonManager().getDungeonWorld());
                }
            });
        }catch (Exception e){
            Instantiated.logger().severe("Error while doing particle calculations:" + Util.stackMessage(e));
        }
    };
    private static final ParticleBuilder PARTICLE_FOR_SELECTED_ROOM = new ParticleBuilder(Particle.FLAME).extra(0).count(1).data(null);
    private static final ParticleBuilder PARTICLE_FOR_UNSELECTED_ROOM = new ParticleBuilder(Particle.REDSTONE).extra(0).count(1)
                                                                              .data(new Particle.DustOptions(Color.fromRGB(10,10,255),0.8f));
    private static final ParticleBuilder PARTICLE_FOR_MOB_BODY = new ParticleBuilder(Particle.SCULK_SOUL).extra(0).count(1).data(null);
    private static final ParticleBuilder PARTICLE_FOR_MOB_DIRECTION = new ParticleBuilder(Particle.SCULK_CHARGE_POP).data(null).count(1).extra(0);
    private static final ParticleBuilder PARTICLE_FOR_CLOSED_DOOR = new ParticleBuilder(Particle.REDSTONE).extra(0).count(1)
                                                                          .data(new Particle.DustOptions(Color.RED,0.8f));
    private static final ParticleBuilder PARTICLE_FOR_OPENED_DOOR = new ParticleBuilder(Particle.DOLPHIN).data(null).count(1).extra(0);
    public static List<Consumer<World>> renderRoom(InstancedDungeonRoom room, boolean selected){
        List<Consumer<World>> todo = new ArrayList<>();
        try{
            if (Instantiated.instance().debug()){
                BlockVector3 center = room.getRegion().getCenter().toBlockPoint().abs().multiply((int) ((room.getDoors().size() + 1) * (room.getMaster().getMobs().size() + 1) * room.getRegion().getDimensions().length()));
                ParticleBuilder particle = new ParticleBuilder(Particle.REDSTONE)
                                                 .data(new Particle.DustOptions(Color.fromRGB(center.getX() % 255, center.getY() % 255, center.getZ() % 255), 0.5f))
                                                 .extra(0d).count(1);
                room.getRegion().stream()
                      .filter(it-> it.getX() % 2 == 0 && it.getZ() % 2 == 0) // pillars of particles
                      .flatMap(it-> Stream.of(it.toVector3(),it.toVector3().add(0,0.5,0)))
                      .forEach(it -> particle.location(room.getLocationInWorld().getWorld(), it.getX(), it.getY(), it.getZ()).spawn());
            }
            drawCuboid(
                  room.getRegion(),
                  Instantiated.instance().getMainConfig().PARTICLE_RENDER_LINE_RESOLUTION,
                  selected ? PARTICLE_FOR_SELECTED_ROOM : PARTICLE_FOR_UNSELECTED_ROOM
            );
        }catch (Exception e){
            Instantiated.logger().warning("Exception trying to render room '" + room.getIdentifier() + "':"+ Util.stackMessage(e));
        }
        for (Map.Entry<IdentifiableKey, DungeonMob> entry : room.getMaster().getMobs().entrySet()) {
            try{
                Location relative = room.getLocationInWorld().toLocation().add(entry.getValue().getSpawnLocation().toLocation());
                Vector direction = entry.getValue().getSpawnLocation().toLocation()
                                         .getDirection().normalize()
                                         .multiply(0.75)
                                         .add(relative.toVector().add(new Vector(0, 2, 0)));
                if (!mobIdentifiers.containsKey(entry.getValue().getSpawnLocation().toLocation())) {
                    todo.add(world -> {
                        TextDisplay display = world.spawn(relative.add(new Vector(0, 2.1, 0)), TextDisplay.class);
                        display.text(ComponentUtil.toComponent("<green>" + entry.getValue().getIdentifier()));
                        display.setMetadata("instantiated-mob-identifier", new FixedMetadataValue(Instantiated.instance(), true));
                        display.setBillboard(Display.Billboard.CENTER);
                        display.setPersistent(false); // important, so if server crashes you dont have floating shit
                        display.getPersistentDataContainer().set(DungeonManager.NSK_OWNED_ENTITY, PersistentDataType.BOOLEAN,true);
                        mobIdentifiers.put(entry.getValue().getSpawnLocation().toLocation(), Pair.of(room, display));
                    });
                }
                final double EYE_HEIGHT = 1.80d;
                drawLine(
                      Vector3.at(relative.getX(), relative.getY(), relative.getZ()),
                      Vector3.at(relative.getX(), relative.getY() + EYE_HEIGHT, relative.getZ()),
                      Instantiated.instance().getMainConfig().PARTICLE_RENDER_LINE_RESOLUTION, // sculk souls persist for a while
                      PARTICLE_FOR_MOB_BODY
                );
                drawLine(
                      Vector3.at(relative.getX(), relative.getY() + EYE_HEIGHT, relative.getZ()),
                      Vector3.at(direction.getX(), direction.getY(), direction.getZ()),
                      Instantiated.instance().getMainConfig().PARTICLE_RENDER_LINE_RESOLUTION,
                      PARTICLE_FOR_MOB_DIRECTION
                );
            }catch (Exception e){
                Instantiated.logger().warning("Exception trying to render mob'" + entry.getValue().getIdentifier() + "':" + Util.stackMessage(e));
                e.printStackTrace();
            }
        }
        for (Map.Entry<ImmutableVector, InstancedDungeonDoor> doorEntry : room.getDoors().entrySet()) {
            try{
                InstancedDungeonDoor door = doorEntry.getValue();
                if (door.getOpen()){
                    drawCuboid(
                          door.getRegion(),
                          Instantiated.instance().getMainConfig().PARTICLE_RENDER_LINE_RESOLUTION * 2, // because dolphin particles are kinda sparse
                          PARTICLE_FOR_OPENED_DOOR
                    );
                } else {
                    drawCuboid(
                          door.getRegion(),
                          Instantiated.instance().getMainConfig().PARTICLE_RENDER_LINE_RESOLUTION,
                          PARTICLE_FOR_CLOSED_DOOR
                    );
                }
            }catch (Exception e){
                Instantiated.logger().warning("Exception trying to render door '" + doorEntry.getValue().getMaster().getIdentifier() + "':"+Util.stackMessage(e));
            }
        }
        return todo;
    }

    public static void drawCuboid(CuboidRegion cuboid, int lineRes, ParticleBuilder particle){
        List<BlockVector3> vertices = new ArrayList<>();
        CuboidRegion region = cuboid.clone();
        region.expand(BlockVector3.ONE);
        double x1 = region.getMinimumPoint().getX(), y1 = region.getMinimumPoint().getY(), z1 = region.getMinimumPoint().getZ();
        double x2 = region.getMaximumPoint().getX(), y2 = region.getMaximumPoint().getY(), z2 = region.getMaximumPoint().getZ();
        for (double x : new double[]{x1, x2}) {
            for (double y : new double[]{y1, y2}) {
                for (double z : new double[]{z1, z2}) {
                    vertices.add(BlockVector3.at(x, y, z));
                }
            }
        }
        for (int[] edge : TWELVE_EDGES_OF_A_CUBE) {
            BlockVector3 v1 = vertices.get(edge[0]);
            BlockVector3 v2 = vertices.get(edge[1]);
            drawLine(v1.toVector3(), v2.toVector3(), lineRes, particle);
        }
    }

    /**
     * A method to render a line of particles, and is very async friendly. This allows for more complex calculations and more lines
     * @param point1
     * @param point2
     * @param res the amount of particles to render
     * @param particle the particle to use the render the line
     * @return A list of consumers to be run sync.
     */
    public static void drawLine(Vector3 point1, Vector3 point2, int res, ParticleBuilder particle){
        int lineRes = (int) (res * point1.distance(point2));
        double deltaX = (point2.getX() - point1.getX()) / lineRes;
        double deltaY = (point2.getY() - point1.getY()) / lineRes;
        double deltaZ = (point2.getZ() - point1.getZ()) / lineRes;
        for (int i = 0; i <= lineRes; i++) {
            double x = point1.getX() + i * deltaX;
            double y = point1.getY() + i * deltaY;
            double z = point1.getZ() + i * deltaZ;
            particle.location(Instantiated.instance().getDungeonManager().getDungeonWorld(),x,y,z).spawn();
        }
    }
    private static final int STRUCTURE_BLOCK_MAX_DISTANCE = 32;
    public static BaseBlock createStructureBlock(Player bukkitPlayer, CuboidRegion ogRegion) {
        com.sk89q.worldedit.entity.Player player = BukkitAdapter.adapt(bukkitPlayer);
        CuboidRegion region = ogRegion.clone();
        int posX = region.getMinimumPoint().getX();
        int posY = region.getMinimumPoint().getY();
        int posZ = region.getMinimumPoint().getZ();
        int width = region.getWidth();
        int height = region.getHeight();
        int length = region.getLength();

        int maxSize = getMaxServerCuiSize();

        if (width > maxSize || length > maxSize || height > maxSize) {
            // Structure blocks have a limit of maxSize^3
            Instantiated.logger().warning("Door too large to render with structure blocks");
            return null;
        }

        // Borrowed this math from FAWE
        final com.sk89q.worldedit.util.Location location = player.getLocation();
        double rotX = location.getYaw();
        double rotY = location.getPitch();
        double xz = Math.cos(Math.toRadians(rotY));
        int x = (int) (location.getX() - (-xz * Math.sin(Math.toRadians(rotX))) * 12);
        int z = (int) (location.getZ() - (xz * Math.cos(Math.toRadians(rotX))) * 12);
        int y = (int) (Math.max(
                        player.getWorld().getMinY(),
                        Math.min(Math.min(player.getWorld().getMaxY(), posY + STRUCTURE_BLOCK_MAX_DISTANCE), posY + 3)
                ) - region.getDimensions().normalize().multiply(3).length());

        posX -= x;
        posY -= y;
        posZ -= z;

        // Instantiated.logger().debug("Region stats: " + posX + " " + posY + " " + posZ + " " + width + " " + height + " " + length);

        if (Math.abs(posX) > STRUCTURE_BLOCK_MAX_DISTANCE || Math.abs(posY) > STRUCTURE_BLOCK_MAX_DISTANCE || Math.abs(posZ) > STRUCTURE_BLOCK_MAX_DISTANCE) {
            // Structure blocks have a limit
            Instantiated.logger().warning("Door too large to render with structure blocks");
            return null;
        }

        CompoundBinaryTag.Builder structureTag = CompoundBinaryTag.builder();
        structureTag.putString("name", "instantiated:" + player.getName());
        structureTag.putString("author", player.getName());
        structureTag.putString("metadata", "");
        structureTag.putInt("x", x);
        structureTag.putInt("y", y);
        structureTag.putInt("z", z);
        structureTag.putInt("posX", posX);
        structureTag.putInt("posY", posY);
        structureTag.putInt("posZ", posZ);
        structureTag.putInt("sizeX", width);
        structureTag.putInt("sizeY", height);
        structureTag.putInt("sizeZ", length);
        structureTag.putString("rotation", "NONE");
        structureTag.putString("mirror", "NONE");
        structureTag.putString("mode", "SAVE");
        structureTag.putByte("ignoreEntities", (byte) 1);
        structureTag.putByte("showboundingbox", (byte) 1);
        assert BlockTypes.STRUCTURE_BLOCK != null;
        structureTag.putString("id", BlockTypes.STRUCTURE_BLOCK.getId());

        return BlockTypes.STRUCTURE_BLOCK.getDefaultState().toBaseBlock(structureTag.build());
    }
}
