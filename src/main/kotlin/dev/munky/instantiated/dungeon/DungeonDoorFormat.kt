package dev.munky.instantiated.dungeon

import com.sk89q.worldedit.EditSession
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import org.bukkit.Material

interface DungeonDoorFormat : Identifiable {
    val parent: DungeonRoomFormat
    override val identifier: IdentifiableKey
    var region: CuboidRegion
    var origin: BlockVector3
    var baseMaterial: Material
    var openMaterial: Material
    val custom: Map<String, String>
    fun instance(parent: InstancedDungeonRoom, editSession: EditSession) : InstancedDungeonDoor
}