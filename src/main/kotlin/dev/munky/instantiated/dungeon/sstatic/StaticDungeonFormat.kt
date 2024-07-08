package dev.munky.instantiated.dungeon.sstatic

import dev.munky.instantiated.data.json.*
import dev.munky.instantiated.dungeon.*
import dev.munky.instantiated.exception.DungeonExceptions
import dev.munky.instantiated.exception.InstantiationException
import dev.munky.instantiated.util.toImmutable
import org.bukkit.Location
import org.bukkit.util.Vector
import java.io.File

class StaticDungeonFormat(
    override val identifier: IdentifiableKey,
    var schematic: File,
    override var spawnVector: Vector
) : Identifiable,DungeonFormat {
    override val instances : MutableSet<StaticInstancedDungeon> = mutableSetOf()
    override val rooms : MutableMap<IdentifiableKey, StaticDungeonRoomFormat> = HashMap()
    @Throws(InstantiationException::class)
    override fun instance(location: Location, option: DungeonFormat.InstanceOption): StaticInstancedDungeon {
        try{
            val instance : StaticInstancedDungeon =
                when (option){
                    DungeonFormat.InstanceOption.CACHE -> StaticInstancedDungeon(this,location.toImmutable(),true)
                    DungeonFormat.InstanceOption.NEW_NON_CACHED -> StaticInstancedDungeon(this,location.toImmutable(),false)
                    DungeonFormat.InstanceOption.CONSUME_CACHE ->
                        if (cached.isEmpty()) StaticInstancedDungeon(this,location.toImmutable(),false)
                        else cached.first() as StaticInstancedDungeon
                }
            instances.add(instance)
            return instance
        }catch (e: Exception){
            throw DungeonExceptions.Instantiation.consume(identifier,e)
        }
    }
}