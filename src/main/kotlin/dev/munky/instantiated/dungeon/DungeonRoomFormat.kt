package dev.munky.instantiated.dungeon

import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import dev.munky.instantiated.dungeon.mob.DungeonMob
import org.bukkit.Material

interface DungeonRoomFormat : Identifiable {
    val parent: DungeonFormat
    var region: CuboidRegion
    var origin: BlockVector3
    override val identifier: IdentifiableKey
    val mobs : MutableMap<IdentifiableKey, DungeonMob>
    var keyDropMode: KeyDropMode
    var keyMaterial: Material
    fun instance(instancedDungeon: InstancedDungeon) : InstancedDungeonRoom
    enum class KeyDropMode {
        MARKED_ROOM_MOB_KILL,
        ROOM_MOBS_CLEAR
    }
}