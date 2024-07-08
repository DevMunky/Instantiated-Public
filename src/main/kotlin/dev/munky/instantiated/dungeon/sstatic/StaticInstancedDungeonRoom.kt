package dev.munky.instantiated.dungeon.sstatic

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import dev.munky.instantiated.Instantiated
import dev.munky.instantiated.dungeon.DungeonRoomFormat
import dev.munky.instantiated.dungeon.IdentifiableKey
import dev.munky.instantiated.dungeon.InstancedDungeonDoor
import dev.munky.instantiated.dungeon.InstancedDungeonRoom
import dev.munky.instantiated.dungeon.mob.DungeonMob
import dev.munky.instantiated.event.room.mob.InstancedDungeonMobSpawnEvent
import dev.munky.instantiated.exception.DungeonExceptions
import dev.munky.instantiated.oldDungeon.DungeonManager
import dev.munky.instantiated.util.*
import org.apache.commons.lang3.tuple.Pair
import org.bukkit.Location
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Zombie
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.persistence.PersistentDataType
import java.util.*

class StaticInstancedDungeonRoom(
    override val parent: StaticInstancedDungeon,
    override val master: StaticDungeonRoomFormat
) : InstancedDungeonRoom {
    override val origin = master.origin
    override val identifier: IdentifiableKey = master.identifier
    override val doors : MutableMap<ImmutableVector,InstancedDungeonDoor> = HashMap()
    private val shift = BlockVector3.at(parent.locationInWorld.x,parent.locationInWorld.y,parent.locationInWorld.z).add(origin);
    override val locationInWorld = shift.toImmutableLocation(parent.locationInWorld.world)
    override var region = master.region.clone() as CuboidRegion
    override var areMobsSpawned: Boolean = false
    init{ init() }
    private fun init(){
        region.shift(shift)
        spawnDoors()
    }
    override fun spawnDoors() {
        Instantiated.logger().debug("Initiating doors for dungeon room '$identifier'...")
        try {
            WorldEdit.getInstance().newEditSessionBuilder().world(
                BukkitAdapter.adapt(locationInWorld.world)
            ).build().use { editSession ->
                for (door in master.doors.values) {
                    try {
                        val instancedDoor: StaticInstancedDungeonDoor = door.instance(this,editSession)
                        doors[instancedDoor.locationInWorld.toImmutableVector()] = instancedDoor
                    } catch (e: Exception) {
                        throw DungeonExceptions.Instantiation.consume(door.identifier,e)
                    }
                }
            }
        } catch (e: Exception) {
            Instantiated.logger().severe(e.message)
        }
    }

    override fun getDoorAtBlockLocation(location: Location): Optional<InstancedDungeonDoor> {
        // finds the first door that contains this location
        return doors.values.firstOrNull {
            val region = it.region.clone()
            region.contains(location.toBlockVector())
        }.asOptional
    }

    override fun registerDungeonMobDeath(dungeonMob: DungeonMob) : Boolean {
        if (dungeonMob.livingEntity.isEmpty) return false
        parent.activeMobs.remove(identifier,dungeonMob)
        Instantiated.logger()
            .debug("Registered dungeon mob death '${dungeonMob.identifier}' which is a '${dungeonMob.livingEntity.get()::class.simpleName}'")
        if ((master.keyDropMode === DungeonRoomFormat.KeyDropMode.MARKED_ROOM_MOB_KILL && dungeonMob.isMarked)
            || (master.keyDropMode === DungeonRoomFormat.KeyDropMode.ROOM_MOBS_CLEAR && parent.activeMobs.get(identifier).isEmpty())
        ) {
            // drop a key if the corresponding modes predicate it fulfilled
            dropKey(dungeonMob.livingEntity.get().location)
            return true
        }
        return false
    }

    override fun spawnMobs() {
        if (areMobsSpawned) return
        Instantiated.logger().debug("Spawning mobs for dungeon room '$identifier'...")
        try {
            for ((_, value) in master.mobs.entries) {
                val mobLocation = value.spawnLocation.toLocation()
                val thisLocation: ImmutableLocation = locationInWorld
                val realLocation = Location(
                    thisLocation.world,
                    thisLocation.x,
                    thisLocation.y,
                    thisLocation.z,
                    mobLocation.yaw,
                    mobLocation.pitch
                )
                realLocation.add(mobLocation)
                val event = InstancedDungeonMobSpawnEvent(this, value, realLocation)
                event.callEvent()
                if (event.isCancelled) continue
                val toSpawn = event.livingEntity
                var spawnedEntity: LivingEntity
                if (toSpawn == null) {
                    spawnedEntity = event.spawnLocation.world.spawn(event.spawnLocation, Zombie::class.java) { zomb ->
                        zomb.customName(ComponentUtil.toComponent("<red>Im fucked and need configured"))
                        zomb.isCustomNameVisible = true
                        zomb.setAI(false)
                        zomb.isSilent = true
                        Instantiated.logger().warning(
                            "Spawned an un-configured mob\n" +
                                    "with mob id '${event.dungeonMob.identifier}'\n" +
                                    "in room '${master.identifier}'\n" +
                                    "in dungeon '${parent.identifier}'\n" +
                                    "at ${event.spawnLocation.toBlockVector()}"
                        )
                    }
                } else {
                    toSpawn.isPersistent = true
                    toSpawn.setMetadata(DungeonManager.METADATA_PERSISTENT_ENTITY, FixedMetadataValue(Instantiated.instance(), true))
                    spawnedEntity = toSpawn
                }
                // clone so we don't unintentionally mutate the dungeon mob inside the master mob list
                val newDungeonMob = event.dungeonMob.clone()
                newDungeonMob.livingEntity = spawnedEntity.asOptional
                spawnedEntity.setMetadata(DungeonManager.METADATA_DUNGEON_MOB, FixedMetadataValue(Instantiated.instance(), Pair.of(this, newDungeonMob)))
                // if the init time doesnt match, then we know the entity is from a different session!
                spawnedEntity.persistentDataContainer.set(
                    DungeonManager.NSK_OWNED_ENTITY,
                    PersistentDataType.LONG,
                    Instantiated.INIT_TIME
                )
                parent.activeMobs.put(identifier,newDungeonMob)
            }
            areMobsSpawned = true
        } catch (e: Exception) {
            Instantiated.logger().severe("Error spawning mobs for dungeon dungeon '$identifier': " + e.stackMessage())
        }
    }
    fun toRelative(location: Location) : Location{
        val worldLocation = location.clone()
        worldLocation.subtract(locationInWorld.toLocation())
        worldLocation.yaw = location.yaw
        worldLocation.pitch = location.pitch
        worldLocation.world = location.world
        return worldLocation
    }
    fun getDungeonMob(realLocation: Location, searchDistance: Double): Optional<DungeonMob> {
        val def = realLocation.clone().multiply(64.0)
        val relativeLocation: Location = toRelative(realLocation)
        var closest = def
        var closestMob: Optional<DungeonMob> = emptyOptional()
        for (dungeonMob in master.mobs.values) {
            val spawn = dungeonMob.spawnLocation.toLocation()
            val dist = spawn.distance(relativeLocation)
            if (spawn == relativeLocation) return dungeonMob.asOptional // quick termination if there is an exact match
            if (dist <= searchDistance && dist < spawn.distance(closest)) {
                closest = spawn
                closestMob = dungeonMob.asOptional
            }
        }
        return closestMob
    }
    fun remove() {
        for (door in doors.values) {
            door.remove()
        }
        doors.clear()
        parent.activeMobs.get(identifier).forEach {
            it.livingEntity.ifPresent{ e -> e.remove() }
        }
    }
}