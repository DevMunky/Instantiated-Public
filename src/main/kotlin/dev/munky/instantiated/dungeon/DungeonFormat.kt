package dev.munky.instantiated.dungeon

import dev.munky.instantiated.exception.DungeonException
import org.bukkit.Location
import org.bukkit.util.Vector

interface DungeonFormat : Identifiable {
    override val identifier : IdentifiableKey
    val instances : MutableSet<out InstancedDungeon>
    var spawnVector : Vector
    val cached : Set<InstancedDungeon> get(){
        return instances.filter { i -> i.cache.isCached }.toSet()
    }
    val rooms : MutableMap<IdentifiableKey, out DungeonRoomFormat>
    @Throws(DungeonException::class)
    fun instance(location: Location, option: InstanceOption) : InstancedDungeon
    enum class InstanceOption{
        CACHE,
        NEW_NON_CACHED,
        CONSUME_CACHE,
    }
}
// option to create a new dungeon with no players if you wanted for some reason