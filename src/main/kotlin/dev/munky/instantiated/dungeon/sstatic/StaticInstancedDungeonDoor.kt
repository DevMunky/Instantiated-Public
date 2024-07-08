package dev.munky.instantiated.dungeon.sstatic

import com.sk89q.worldedit.EditSession
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.world.block.BlockTypes
import dev.munky.instantiated.Instantiated
import dev.munky.instantiated.dungeon.DungeonDoorFormat
import dev.munky.instantiated.dungeon.InstancedDungeonDoor
import dev.munky.instantiated.event.room.key.InstancedDungeonKeyUsedEvent
import dev.munky.instantiated.util.ComponentUtil
import dev.munky.instantiated.util.ImmutableLocation
import dev.munky.instantiated.util.VectorUtil
import dev.munky.instantiated.util.toImmutableLocation
import net.kyori.adventure.title.Title
import org.bukkit.Location
import org.bukkit.Material
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.*

class StaticInstancedDungeonDoor(
    override val parent : StaticInstancedDungeonRoom,
    override val master : DungeonDoorFormat,
    editSession: EditSession
) : InstancedDungeonDoor {
    private val shift: BlockVector3 = VectorUtil.toBlockVector(parent.locationInWorld.toLocation()).add(master.origin)
    override var region = CuboidRegion(master.region.pos1.add(shift), master.region.pos2.add(shift))
    override var open = false
    override val locationInWorld : ImmutableLocation = shift.toImmutableLocation(parent.locationInWorld.world)
    override val identifier = master.identifier
    init{
        update(editSession, master.baseMaterial)
        Instantiated.logger().debug("Injected door '${master.identifier}'")
    }
    override fun open(useKeys: Boolean) {
        if (useKeys) {
            if (this.parent.parent.doorKeys <= 0) return
            val event = InstancedDungeonKeyUsedEvent(parent.parent, this)
            event.callEvent()
            if (event.isCancelled) return
        }
        if (open) {
            return
        }
        try {
            WorldEdit.getInstance().newEditSessionBuilder().world(
                BukkitAdapter.adapt(locationInWorld.world)
            ).build().use { session ->
                update(session, master.openMaterial)
                Instantiated.logger().debug(
                    "Opened door '${master.identifier}', new keys = ${parent.parent.doorKeys}"
                )
                this.open = true
                if (!useKeys) return
                this.parent.parent.doorKeys--
                val doorOpenTitle = Title.title(
                    ComponentUtil.toComponent("<gradient:red:blue:red>Door opened!"),
                    ComponentUtil.toComponent("<gray><italic>Current keys: ${parent.parent.doorKeys}"),
                    Title.Times.times(
                        Duration.of(500, ChronoUnit.MILLIS),
                        Duration.of(2, ChronoUnit.SECONDS),
                        Duration.of(500, ChronoUnit.MILLIS)
                    )
                )
                for (player in this.parent.parent.onlinePlayers) {
                    player.showTitle(doorOpenTitle)
                }
            }
        } catch (e: Exception) {
            Instantiated.logger().severe("Error opening door '${master.identifier}':\n${e.message}")
        }
    }
    override fun close() {
        if (!this.open) return
        try {
            WorldEdit.getInstance().newEditSessionBuilder().world(
                BukkitAdapter.adapt(locationInWorld.world)
            ).build().use { session ->
                update(session, master.baseMaterial)
                Instantiated.logger().debug("Closed door '${master.identifier}'")
                this.open = false
            }
        } catch (e: java.lang.Exception) {
            Instantiated.logger().severe("Error closing door '${master.identifier}':\n${e.message}")
        }
    }
    override fun remove() {
        try {
            WorldEdit.getInstance().newEditSessionBuilder().world(
                BukkitAdapter.adapt(locationInWorld.world)
            ).build().use { session ->
                update(session, Material.AIR)
                this.open = true
            }
        } catch (e: java.lang.Exception) {
            Instantiated.logger().severe("Error removing door '${master.identifier}':\n${e.message}")
        }
    }
    fun toRelative(location: Location): Location {
        return locationInWorld.toLocation().add(location)
    }
    private fun update(session: EditSession, block: Material) {
        val region = CuboidRegion(region.pos1 ,region.pos2)
        session.setBlocks(region as Region, BlockTypes.get(block.toString().lowercase(Locale.getDefault())))
    }
}