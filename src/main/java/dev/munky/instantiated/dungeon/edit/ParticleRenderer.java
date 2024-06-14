package dev.munky.instantiated.dungeon.edit;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.polyhedron.Triangle;
import com.sk89q.worldedit.util.nbt.CompoundBinaryTag;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockTypes;
import dev.munky.instantiated.Instantiated;
import dev.munky.instantiated.event.DungeonLoadEvent;
import dev.munky.instantiated.event.ListenerFactory;
import dev.munky.instantiated.dungeon.DungeonManager;
import dev.munky.instantiated.dungeon.InstancedDungeon;
import dev.munky.instantiated.dungeon.room.InstancedDungeonRoom;
import dev.munky.instantiated.dungeon.room.door.InstancedDungeonDoor;
import dev.munky.instantiated.dungeon.room.mob.DungeonMob;
import dev.munky.instantiated.util.ComponentUtil;
import dev.munky.instantiated.util.ImmutableVector;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.function.Consumer;

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
    public static final Runnable RUNNABLE = () -> {
        if (Bukkit.getServer().isPrimaryThread()){
            Instantiated.logger().warning("Expensive particle calculation is being run on the main thread when it is explicitly stated that should be run asynchronously!");
        }
        List<InstancedDungeonRoom> roomsToBeRendered = new ArrayList<>();
        for (InstancedDungeon realInstance : Instantiated.getDungeonManager().getInstancedDungeonLocations().values()) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                InstancedDungeonRoom closest = realInstance.getClosestRoom(onlinePlayer);
                if (closest!=null) roomsToBeRendered.add(closest);
            }
        }
        List<Consumer<World>> toBeSync = new ArrayList<>();
        List<Consumer<Map<Location, Pair<InstancedDungeonRoom,TextDisplay>>>> toDoMobIdentifiers = new ArrayList<>();
        for (Map.Entry<Location, Pair<InstancedDungeonRoom,TextDisplay>> entry : mobIdentifiers.entrySet()) {
            if (!roomsToBeRendered.contains(entry.getValue().getLeft())){
                toBeSync.add(world -> {
                    entry.getValue().getRight().setMetadata(DungeonManager.METADATA_KILL_ON_SIGHT,
                            new FixedMetadataValue(Instantiated.instance(),true)
                    ); // this metadata kills the text display if it is ever seen
                    entry.getValue().getRight().remove();
                });
                toDoMobIdentifiers.add(map -> {
                    map.remove(entry.getKey());
                });
            }
        }
        for (Consumer<Map<Location, Pair<InstancedDungeonRoom,TextDisplay>>> toDoMobIdentifier : toDoMobIdentifiers) {
            toDoMobIdentifier.accept(mobIdentifiers);
        }
        for (InstancedDungeonRoom room : roomsToBeRendered) {
            try {
                toBeSync.addAll(renderRoom(room));
            } catch (Exception e) {
                Instantiated.logger().warning("Exception while rendering:\n" + e.getMessage());
            }
        }
        Bukkit.getScheduler().runTask(Instantiated.instance(),()->{
            for (Consumer<World> runnable : toBeSync) {
                runnable.accept(Instantiated.getDungeonManager().getDungeonWorld());
            }
        });
    };
    public static List<Consumer<World>> renderRoom(InstancedDungeonRoom room){
        List<Consumer<World>> todo = new ArrayList<>();
        for (Triangle tri : room.getRegion().getTriangles()) {
            Vector3 previous = tri.getVertex(0);
            for (int i = 1; i < 3; i++) {
                Vector3 vector = tri.getVertex(i);
                todo.addAll(drawLine(
                        vector,
                        previous,
                        Instantiated.getMainConfig().PARTICLE_RENDER_LINE_RESOLUTION,
                        Particle.FLAME,
                        null,
                        0
                ));
                previous = vector;
            }
        }
        for (Map.Entry<String, DungeonMob> entry : room.getEntities().entrySet()) {
            Location relative = room.getLocation().toLocation().add(entry.getValue().getSpawnLocation().toLocation());
            Vector direction = entry.getValue().getSpawnLocation().toLocation().getDirection().normalize().multiply(0.75).add(relative.toVector().add(new Vector(0,2,0)));
            if (!mobIdentifiers.containsKey(entry.getValue().getSpawnLocation().toLocation())){
                todo.add(world -> {
                    TextDisplay display = world.spawn(relative.add(new Vector(0,2.1,0)),TextDisplay.class);
                    display.text(ComponentUtil.toComponent("<green>MobID: " + room.getEntities().get(entry.getValue().getMobIdentifier()).getMobIdentifier()));
                    display.setMetadata("instantiated-mob-identifier",new FixedMetadataValue(Instantiated.instance(),true));
                    display.setBillboard(Display.Billboard.CENTER);
                    display.setPersistent(false); // important, so if server crashes you dont have floating shit
                    mobIdentifiers.put(entry.getValue().getSpawnLocation().toLocation(), Pair.of(room,display));
                });
            }
            todo.addAll(drawLine(
                    Vector3.at(relative.getX(), relative.getY(), relative.getZ()),
                    Vector3.at(relative.getX(), relative.getY() + 2.0d, relative.getZ()),
                    (int) (Instantiated.getMainConfig().PARTICLE_RENDER_LINE_RESOLUTION * 0.5), // sculk souls persist for a while
                    Particle.SCULK_SOUL,
                    null,
                    0
            ));
            todo.addAll(drawLine(
                    Vector3.at(relative.getX(), relative.getY() + 2.0d, relative.getZ()),
                    Vector3.at(direction.getX(),direction.getY(),direction.getZ()),
                    Instantiated.getMainConfig().PARTICLE_RENDER_LINE_RESOLUTION,
                    Particle.SCULK_CHARGE_POP,
                    null,
                    0
            ));
        }
        for (Map.Entry<ImmutableVector, InstancedDungeonDoor> doorEntry : room.getDoors().entrySet()) {
            InstancedDungeonDoor door = doorEntry.getValue();
            todo.addAll(drawCuboid(
                    door.getRegion(),
                    (int) (Instantiated.getMainConfig().PARTICLE_RENDER_LINE_RESOLUTION * 1.5), // because dolphin particles are kinda sparse
                    Particle.DOLPHIN,
                    null
            ));
        }
        return todo;
    }

    public static <T> List<Consumer<World>> drawCuboid(CuboidRegion region, int lineRes, Particle particle, T data){
        List<Consumer<World>> todo = new ArrayList<>();
        List<BlockVector3> vertices = new ArrayList<>();
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
            todo.addAll(drawLine(v1.toVector3(), v2.toVector3(), lineRes, particle, data));
        }
        return todo;
    }

    /**
     * A method to render a line of particles, and is very async friendly. This allows for more complex calculations and more lines
     * @param point1
     * @param point2
     * @param lineRes the amount of particles to render
     * @param particle the particle to use the render the line
     * @param data nullable data for the
     * @return A list of consumers to be run sync.
     * @param <T> data type is based on the particle or can be null if no data is required
     */
    public static <T> List<Consumer<World>> drawLine(Vector3 point1, Vector3 point2, int lineRes, Particle particle, T data){
        return drawLine(point1,point2,lineRes,particle,data,0);
    }
    public static <T> List<Consumer<World>> drawLine(Vector3 point1, Vector3 point2, int lineRes, Particle particle, T data, int extra){
        List<Consumer<World>> todo = new ArrayList<>();
        double deltaX = (point2.getX() - point1.getX()) / lineRes;
        double deltaY = (point2.getY() - point1.getY()) / lineRes;
        double deltaZ = (point2.getZ() - point1.getZ()) / lineRes;

        for (int i = 0; i <= lineRes; i++) {
            double x = point1.getX() + i * deltaX;
            double y = point1.getY() + i * deltaY;
            double z = point1.getZ() + i * deltaZ;
            todo.add((world) -> world.spawnParticle(particle, x, y, z, 1,0,0,0,extra, data));
        }
        return todo;
    }
    private static final int MAX_DISTANCE = 32;
    public static BaseBlock createStructureBlock(Player bukkitPlayer, CuboidRegion ogRegion) {
        com.sk89q.worldedit.entity.Player player = BukkitAdapter.adapt(bukkitPlayer);
        CuboidRegion region = ogRegion.clone();
        region.contract(BlockVector3.at(-1,-1,-1));
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
                        Math.min(Math.min(player.getWorld().getMaxY(), posY + MAX_DISTANCE), posY + 3)
                ) - region.getDimensions().normalize().multiply(3).length());

        posX -= x;
        posY -= y;
        posZ -= z;

        // Instantiated.logger().debug("Region stats: " + posX + " " + posY + " " + posZ + " " + width + " " + height + " " + length);

        if (Math.abs(posX) > MAX_DISTANCE || Math.abs(posY) > MAX_DISTANCE || Math.abs(posZ) > MAX_DISTANCE) {
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
