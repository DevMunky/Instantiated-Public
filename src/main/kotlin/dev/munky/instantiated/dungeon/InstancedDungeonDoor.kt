package dev.munky.instantiated.dungeon

import com.sk89q.worldedit.regions.CuboidRegion
import dev.munky.instantiated.util.ImmutableLocation

interface InstancedDungeonDoor : Identifiable{
    val master : DungeonDoorFormat
    val parent : InstancedDungeonRoom
    val locationInWorld : ImmutableLocation
    var region : CuboidRegion
    var open : Boolean
    fun open(useKeys: Boolean)
    fun close()
    fun remove()
}