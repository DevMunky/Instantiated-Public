package dev.munky.instantiated.dungeon.room.door;

import com.fastasyncworldedit.core.FaweAPI;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockTypes;
import dev.munky.instantiated.Instantiated;
import dev.munky.instantiated.event.room.key.InstancedDungeonKeyUsedEvent;
import dev.munky.instantiated.dungeon.Instantiable;
import dev.munky.instantiated.dungeon.room.InstancedDungeonRoom;
import dev.munky.instantiated.dungeon.room.RegionHolder;
import dev.munky.instantiated.util.ComponentUtil;
import dev.munky.instantiated.util.ImmutableLocation;
import dev.munky.instantiated.util.VectorUtil;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class InstancedDungeonDoor implements RegionHolder<CuboidRegion>, Instantiable<DungeonDoor> {
    private final InstancedDungeonRoom parent;
    private final DungeonDoor master;
    private CuboidRegion region;
    private final ImmutableLocation realLocation;
    @Deprecated
    private Interaction interaction; // TODO remove
    private final boolean instanced;
    private boolean open;
    public InstancedDungeonDoor(InstancedDungeonRoom parent, DungeonDoor master){
        this.parent = parent;
        this.master = master;
        BlockVector3 shift = VectorUtil.toBlockVector(parent.getLocation().toLocation());
        shift.add(master.getOrigin());
        this.region = new CuboidRegion(master.getRegion().getPos1().add(shift),master.getRegion().getPos2().add(shift));
        this.realLocation = ImmutableLocation.of(VectorUtil.toLocation(shift,parent.getLocation().getWorld()));
        this.instanced = true;
        this.open = false;
        this.interaction = null;
    }
    public void inject(EditSession session){
        update(session,getMaster().getBaseMaterial());
        Instantiated.logger().debug("Injected door '" + getMaster().getIdentifier() + "'");
    }
    protected void update(EditSession session, Material block){
        CuboidRegion region = new CuboidRegion(getRegion().getPos1(),getRegion().getPos2());
        region.contract(BlockVector3.at(-1,-1,-1));
        session.setBlocks((Region) region, BlockTypes.get(block.toString().toLowerCase()));
    }
    public void open(boolean useKeys){
        if (useKeys){
            if (this.getParent().getParent().getDoorKeys() <= 0) return;
            InstancedDungeonKeyUsedEvent event = new InstancedDungeonKeyUsedEvent(getParent().getParent(),this);
            event.callEvent();
            if (event.isCancelled()) return;
        }
        if (!this.open) {
            try (
                    EditSession session = WorldEdit.getInstance().newEditSessionBuilder().world(FaweAPI.getWorld(
                            getLocation().getWorld().getName())).build()
            ) {
                update(session, getMaster().getOpenMaterial());
                Instantiated.logger().debug("Opened door '" + getMaster().getIdentifier() + "', new keys = " + this.getParent().getParent().getDoorKeys());
                this.open = true;
                if (useKeys) {
                    this.getParent().getParent().decrementDoorKeys();
                    Title doorOpenTitle = Title.title(
                            ComponentUtil.toComponent("<gradient:red:blue:red>Door opened!"),
                            ComponentUtil.toComponent("<gray><italic>Current keys: " + this.getParent().getParent().getDoorKeys()),
                            Title.Times.times(
                                    Duration.of(500, ChronoUnit.MILLIS),
                                    Duration.of(2, ChronoUnit.SECONDS),
                                    Duration.of(500, ChronoUnit.MILLIS)
                            )
                    );
                    for (Player player : this.getParent().getParent().getOnlinePlayers()) {
                        player.showTitle(doorOpenTitle);
                    }
                }
            } catch (Exception e) {
                Instantiated.logger().severe("Error opening door '" + getMaster().getIdentifier() + "':\n" + e.getMessage());
            }
        }
    }
    public void close(){
        if (this.open) {
            try (
                    EditSession session = WorldEdit.getInstance().newEditSessionBuilder().world(FaweAPI.getWorld(
                            getLocation().getWorld().getName())).build()
            ) {
                update(session, getMaster().getBaseMaterial());
                Instantiated.logger().debug("Closed door '" + getMaster().getIdentifier() + "'");
                this.open = false;
            } catch (Exception e) {
                Instantiated.logger().severe("Error closing door '" + getMaster().getIdentifier() + "':\n" + e.getMessage());
            }
        }
    }
    public void remove(){
        if (this.interaction!=null){
            this.interaction.remove();
            this.interaction = null;
        }
        try(
                EditSession session = WorldEdit.getInstance().newEditSessionBuilder().world(FaweAPI.getWorld(
                        getLocation().getWorld().getName())).build()
        ){
            update(session,Material.AIR);
            this.open = true;
        }catch (Exception e){
            Instantiated.logger().severe("Error removing door '" + getMaster().getIdentifier() + "':\n" + e.getMessage());
        }
    }
    public InstancedDungeonRoom getParent(){
        return this.parent;
    }
    @Override
    public DungeonDoor getMaster() {
        return this.master;
    }

    @Override
    public ImmutableLocation getLocation() {
        return this.realLocation;
    }

    @Override
    public Location toRelative(Location location) {
        return getLocation().toLocation().add(location);
    }

    @Override
    public boolean isInstanced() {
        return this.instanced;
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
        return getMaster().getOrigin();
    }
    public boolean isOpen(){
        return this.open;
    }
}
