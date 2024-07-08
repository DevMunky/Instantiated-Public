package dev.munky.instantiated.dungeon.sstatic

import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import dev.munky.instantiated.dungeon.DungeonRoomFormat
import dev.munky.instantiated.dungeon.IdentifiableKey
import dev.munky.instantiated.dungeon.InstancedDungeon
import dev.munky.instantiated.dungeon.mob.DungeonMob
import org.bukkit.Material

class StaticDungeonRoomFormat(
    override val identifier: IdentifiableKey,
    override val parent: StaticDungeonFormat,
    override var origin: BlockVector3,
    private var cuboidNow: CuboidRegion,
    private val mobsRaw : Map<IdentifiableKey, DungeonMob>,
    override var keyDropMode: DungeonRoomFormat.KeyDropMode,
    override var keyMaterial: Material
) : DungeonRoomFormat {
    override var region: CuboidRegion
        get() = cuboidNow
        set(value) {
            cuboidNow = value
        }
    val doors: MutableMap<IdentifiableKey, StaticDungeonDoorFormat> = HashMap()
    override val mobs: MutableMap<IdentifiableKey, DungeonMob> = mobsRaw as MutableMap
    override fun instance(instancedDungeon: InstancedDungeon): StaticInstancedDungeonRoom {
        if (instancedDungeon !is StaticInstancedDungeon) throw ClassCastException("Instanced Dungeon parameter is not static, yet this is a static room!")
        return StaticInstancedDungeonRoom(instancedDungeon,this)
    }
}