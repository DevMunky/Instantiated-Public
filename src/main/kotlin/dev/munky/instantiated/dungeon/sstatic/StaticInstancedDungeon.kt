package dev.munky.instantiated.dungeon.sstatic

import com.fastasyncworldedit.core.extent.transform.PatternTransform
import com.google.common.collect.ListMultimap
import com.google.common.collect.MultimapBuilder
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.WorldEditException
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.function.operation.Operation
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.math.Vector3
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.session.ClipboardHolder
import com.sk89q.worldedit.world.block.BlockTypes
import dev.munky.instantiated.Instantiated
import dev.munky.instantiated.PluginState
import dev.munky.instantiated.dungeon.IdentifiableKey
import dev.munky.instantiated.dungeon.InstancedDungeon
import dev.munky.instantiated.dungeon.InstancedDungeonRoom
import dev.munky.instantiated.dungeon.mob.DungeonMob
import dev.munky.instantiated.exception.DungeonException
import dev.munky.instantiated.exception.DungeonExceptions
import dev.munky.instantiated.oldDungeon.DungeonManager
import dev.munky.instantiated.util.*
import org.apache.commons.lang3.tuple.Pair
import org.bukkit.Bukkit
import org.bukkit.FluidCollisionMode
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import java.io.FileInputStream
import java.io.IOException
import java.util.*


class StaticInstancedDungeon
@Throws(DungeonException::class)
internal constructor(
    override val master : StaticDungeonFormat,
    override val locationInWorld : ImmutableLocation,
    cache : Boolean = false
) : InstancedDungeon {
    companion object{
        val MANAGER: DungeonManager = Instantiated.instance().dungeonManager
    }
    override var cache = if (cache) InstancedDungeon.CacheState.CACHED else InstancedDungeon.CacheState.NEVER_CACHED
    private var pastedClipboard: Optional<Clipboard> = emptyOptional()
    override var difficulty: Double = 1.0
    // player uuid mapped to locationInWorld before they were teleported in
    private val playerMap: MutableMap<UUID, Location> = HashMap()
    override val onlinePlayers : List<Player> get() {
        return playerMap.keys.mapNotNull{ uuid -> Bukkit.getPlayer(uuid) }
    }
    override val rooms: MutableMap<IdentifiableKey, StaticInstancedDungeonRoom> = HashMap()
    override var doorKeys = 0 // the number of keys that are left for the dungeon.
    override val playerLocations : Map<UUID,Location> get() = playerMap
    /**
     * Keys are the identifier of the room, values are the mob
     */
    override val activeMobs: ListMultimap<IdentifiableKey, DungeonMob> = MultimapBuilder.ListMultimapBuilder.hashKeys().arrayListValues().build()
    override val players : List<UUID> get(){
        return playerMap.keys.toList()
    }
    override val identifier : IdentifiableKey get() = master.identifier
    init{
        try{
            val clipboardFormat = ClipboardFormats.findByFile(master.schematic)
                ?: throw IllegalArgumentException("No schematic found by file name '${master.schematic.name}'")
            var clipboard: Clipboard
            val blockVector3 = BlockVector3.at(locationInWorld.x, locationInWorld.y, locationInWorld.z)
            try {
                clipboardFormat.getReader(FileInputStream(master.schematic)).use { clipboardReader ->
                    val editSession = WorldEdit.getInstance().newEditSessionBuilder()
                        .world(BukkitAdapter.adapt(locationInWorld.world))
                        .fastMode(true)
                        .checkMemory(false)
                        .build()
                    clipboard = clipboardReader.read()

                    val origin = clipboard.origin

                    val holder = ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .copyEntities(false)
                        .to(blockVector3)
                        .ignoreAirBlocks(false)

                    val operation: Operation = holder.build()

                    Operations.complete(operation)
                    clipboard = editSession.lazyCopy(clipboard.region)
                    clipboard.origin = origin
                    editSession.close()
                    if (!this.cache.wasCached)
                        master.instances.add(this)
                    Instantiated.logger().debug("Injected instance '$identifier', modified ${clipboard.volume}")
                }
                pastedClipboard = clipboard.asOptional
            } catch (e: IOException) {
                throw DungeonExceptions.Instantiation.consume(identifier,e)
            }
            for (room in master.rooms.values) {
                rooms[room.identifier] = room.instance(this) as StaticInstancedDungeonRoom
            }
        }catch (e: Exception){
            // I need to remove all blocks that might have been placed halfway through
            try {
                // if the plugin is past enabling, try the hold again
                this.remove(
                    InstancedDungeon.RemovalReason.EXCEPTION_THROWN,
                    Instantiated.getLoadState().isState(PluginState.PROCESSING) && cache && this.cache.wasCached
                )
                throw DungeonExceptions.Instantiation.consume(identifier, e)
            } catch (ex: DungeonException) {
                throw DungeonExceptions.PhysicalRemoval.consume(this,ex)
            }
        }
    }

    override fun getClosestRoom(player: Player): Optional<InstancedDungeonRoom> {
        val closest : Optional<InstancedDungeonRoom> = getRoomAt(player.location);
        if (closest.isPresent) return closest
        val playerVector = Vector3.at(player.x, player.y, player.z)
        rooms.values.map{ r -> Pair.of(r, r.region.center.distance(playerVector)) }
            .filter { p -> p.right < 50 }
            .sortedWith { o1, o2 -> (o1.right - o2.right).toInt() }
            .firstOrNull().let {
                if (it==null) return emptyOptional()
                return (it.left).asOptional
            }
    }

    override fun getRoomAt(location: Location): Optional<InstancedDungeonRoom> {
        if (rooms.isEmpty()) return emptyOptional()
        rooms.values.firstOrNull { p ->
            p.region.contains(location.x.toInt(), location.y.toInt(), location.z.toInt())
        }.let {
            return it.asOptional
        }
    }

    @Throws(DungeonException::class)
    override fun remove(context: InstancedDungeon.RemovalReason, continueHolding: Boolean) {
        removePlayers()
        for (room in rooms.values) {
            room.remove()
        }
        rooms.clear()
        removeMobs() // just in case type thing
        if (cache.wasCached && continueHolding){
            for (roomFormat in master.rooms.values) {
                rooms[roomFormat.identifier] = roomFormat.instance(this) as StaticInstancedDungeonRoom
            }
            cache = InstancedDungeon.CacheState.CACHED
            Instantiated.logger().info("Re-Cached instance of '$identifier'")
        }else{
            master.instances.remove(this)
            removePhysical(context)
            Instantiated.logger().info("Removed instance of '$identifier'")
        }
        System.gc()
    }
    private fun removePhysical(context: InstancedDungeon.RemovalReason): Boolean {
        try {
            if (!WorldEdit.getInstance().platformManager.isInitialized) { // second condition stops an unchecked exception while disabling
                Instantiated.logger().debug("Tried to remove an instance that was never actually instanced")
                return true
            }
            Instantiated.logger().info("Removing instance of dungeon '$identifier' because $context")
            WorldEdit.getInstance().newEditSessionBuilder()
                .world(BukkitAdapter.adapt(locationInWorld.world))
                .fastMode(true)
                .checkMemory(false)
                .build().use { session ->
                    val operation = ClipboardHolder(pastedClipboard.get())
                        .createPaste(PatternTransform(session.extent, BlockTypes.AIR))
                        .to(BlockVector3.at(locationInWorld.x, locationInWorld.y, locationInWorld.z))
                        .ignoreAirBlocks(false)
                        .copyEntities(false)
                        .build()
                    try {
                        Operations.complete(operation)
                        Instantiated.logger()
                            .debug("Removed dungeon of '$identifier' , modified ${pastedClipboard.get().volume} blocks")
                    } catch (e: WorldEditException) {
                        e.printStackTrace()
                    }
                    pastedClipboard.get().close()
                }
        } catch (e: Exception) {
            throw DungeonExceptions.PhysicalRemoval.consume(this, e)
        }
        return false
    }

    private fun removeMobs() {
        for (mob in activeMobs.values()) {
            if (mob.livingEntity.isPresent) mob.livingEntity.get().remove()
        }
        if (pastedClipboard.isPresent) {
            val region: Region = pastedClipboard.get().region.clone()
            region.shift(BlockVector3.ZERO.subtract(pastedClipboard.get().origin))
            region.shift(locationInWorld.toImmutableVector().toBlockVector3())
            for (entity in locationInWorld.world.entities) {
                // removes EVERY entity that is inside the region that the schematic encompasses
                if (region.contains(entity.x.toInt(), entity.y.toInt(), entity.z.toInt())) {
                    if (entity !is Player) entity.remove()
                }
            }
        }
        activeMobs.clear()
    }
    private fun removePlayers() {
        for (player in players) {
            val online = Bukkit.getPlayer(player)
            val teleportLocation = playerLocations[player]
            if (online != null && online.isOnline && teleportLocation.asOptional.isPresent) {
                if (online.location != teleportLocation)
                    online.teleport(teleportLocation!!)
            }
            playerMap.remove(player)
            Instantiated.logger().debug("Removed player '$player' from dungeon '$identifier'")
        }
        playerMap.clear()
    }
    private fun spawnPlayer(player:Player) {
        val l = locationInWorld.toLocation().add(master.spawnVector)
        val oldY = l.y
        l.y += 5.5
        val result = l.world.rayTrace(
            l,
            Vector(0, -1, 0),
            255.0,
            FluidCollisionMode.ALWAYS,
            true,
            1.0,
            null
        )
        if (result != null) l.y = result.hitPosition.y + 0.25
        else l.y = oldY + 0.25
        player.teleport(l)
    }
    override fun addPlayer(player: Player){
        val current = MANAGER.getCurrentDungeon(player.uniqueId)
        var location = player.location
        if (current.isPresent) {
            // assume that if the player came from a dungeon, then that location is set and valid
            location = current.get().playerLocations[player.uniqueId]!!
            current.get().removePlayer(player.uniqueId)
        }
        if (location.world == locationInWorld.world) {
            val maybeLocation = playerMap[player.uniqueId].asOptional
            location = if (maybeLocation.isPresent) maybeLocation.get() else Bukkit.getWorlds().first().spawnLocation
        }
        playerMap[player.uniqueId] = location
        Instantiated.logger().debug("Player (${player.name}) added to '$identifier'")
        spawnPlayer(player)
        if (cache.isCached) cache = InstancedDungeon.CacheState.PREVIOUSLY_CACHED
    }
    override fun removePlayer(player: UUID) {
        val onlinePlayer = Bukkit.getPlayer(player)
        val teleportLocation = playerLocations[player]
        playerMap.remove(player)
        if (onlinePlayer != null && onlinePlayer.isOnline && teleportLocation.asOptional.isPresent && onlinePlayer.location != teleportLocation) {
            onlinePlayer.teleport(teleportLocation!!)
        }
        Instantiated.logger().debug("Removed player '${onlinePlayer?.name ?: player}' from dungeon '$identifier'")
        if (players.isEmpty()) {
            try {
                this.remove(InstancedDungeon.RemovalReason.NO_PLAYERS_LEFT, true)
            } catch (e: DungeonException) {
                Instantiated.logger().severe("Could not remove instance:${e.stackMessage()}")
            }
        }
    }
}