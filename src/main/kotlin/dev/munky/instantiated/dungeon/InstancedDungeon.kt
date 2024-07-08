package dev.munky.instantiated.dungeon

import com.google.common.collect.ListMultimap
import dev.munky.instantiated.dungeon.mob.DungeonMob
import dev.munky.instantiated.exception.DungeonException
import dev.munky.instantiated.util.ImmutableLocation
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.*

interface InstancedDungeon : Identifiable{
    val master : DungeonFormat
    var cache : CacheState
    var difficulty : Double
    val rooms: MutableMap<IdentifiableKey, out InstancedDungeonRoom>
    val activeMobs: ListMultimap<IdentifiableKey, DungeonMob>
    val locationInWorld: ImmutableLocation
    var doorKeys : Int
    val players : List<UUID>
    val onlinePlayers : List<Player>
    fun addPlayer(player:Player)
    fun addPlayers(players: Array<Player>){
        players.forEach { addPlayer(it) }
    }
    fun removePlayer(player: UUID)
    fun removePlayers(players: Array<UUID>){
        players.forEach { removePlayer(it) }
    }
    fun getClosestRoom(player: Player) : Optional<InstancedDungeonRoom>
    fun getRoomAt(location: Location) : Optional<InstancedDungeonRoom>
    @Throws(DungeonException::class)
    fun remove(context: RemovalReason, continueHolding: Boolean)
    enum class CacheState{
        CACHED,
        NEVER_CACHED,
        PREVIOUSLY_CACHED;
        val isCached : Boolean get() {
            return this == CACHED
        }
        val wasCached : Boolean get() {
            return this == CACHED || this == PREVIOUSLY_CACHED
        }
        val neverCached : Boolean get(){
            return this == NEVER_CACHED
        }
    }
    enum class RemovalReason {
        NO_PLAYERS_LEFT,
        FORMAT_CHANGE,
        PLUGIN_RELOAD,
        EXCEPTION_THROWN,
        PLUGIN_DISABLE
    }

    val playerLocations: Map<UUID, Location>
}