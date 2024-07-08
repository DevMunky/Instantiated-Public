package dev.munky.instantiated.dungeon.sstatic

import com.sk89q.worldedit.EditSession
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import dev.munky.instantiated.dungeon.DungeonDoorFormat
import dev.munky.instantiated.dungeon.IdentifiableKey
import dev.munky.instantiated.dungeon.InstancedDungeonRoom
import org.bukkit.Material

class StaticDungeonDoorFormat(
    override val parent: StaticDungeonRoomFormat,
    override val identifier: IdentifiableKey,
    override var region: CuboidRegion,
    override var origin: BlockVector3,
    override var baseMaterial: Material,
    override var openMaterial: Material,
    override val custom: Map<String, String>
) : DungeonDoorFormat {
    override fun instance(parent: InstancedDungeonRoom, editSession: EditSession): StaticInstancedDungeonDoor {
        return StaticInstancedDungeonDoor(parent as StaticInstancedDungeonRoom,this,editSession)
    }
}