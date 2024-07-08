package dev.munky.instantiated.data.json

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.ConvexPolyhedralRegion
import com.sk89q.worldedit.regions.CuboidRegion
import dev.munky.instantiated.Instantiated
import dev.munky.instantiated.dungeon.DungeonFormat
import dev.munky.instantiated.dungeon.mob.DungeonMob
import dev.munky.instantiated.dungeon.procedural.ProceduralDungeonFormat
import dev.munky.instantiated.dungeon.sstatic.StaticDungeonDoorFormat
import dev.munky.instantiated.dungeon.sstatic.StaticDungeonFormat
import dev.munky.instantiated.dungeon.sstatic.StaticDungeonRoomFormat
import dev.munky.instantiated.exception.DungeonException
import dev.munky.instantiated.exception.DungeonExceptions

val DungeonFormat.asJsonObject : JsonObject get(){
    return when (this){
        is StaticDungeonFormat -> this.asJsonObject
        is ProceduralDungeonFormat -> this.asJsonObject
        else -> throw DungeonExceptions.DataSyntax.consume("unhandled dungeon format class '${this::class.simpleName}'")
    }
}

val StaticDungeonFormat.asJsonObject : JsonObject get() {
    val json = JsonObject()
    try{
        json.addProperty("type","static")
        json.addProperty("identifier",this.identifier.id)
        json.addProperty("schematic",this.schematic.name)
        json.add("spawn",this.spawnVector.serialize().asJsonObject)
        val roomArray = JsonArray()
        for (room in this.rooms.values){
           roomArray.add((room as StaticDungeonRoomFormat).asJsonObject)
        }
        json.add("rooms",roomArray)
        Instantiated.logger().debug("Saved dungeon '$identifier'")
        return json
    }catch (e: DungeonException){
        throw DungeonExceptions.DataSyntax.consume("static dungeon '$identifier'",e)
    }
}
val StaticDungeonRoomFormat.asJsonObject : JsonObject get() {
    val json = JsonObject()
    try{
        json.addProperty("identifier",this.identifier.id)
        json.add("origin",this.origin.asJsonObject)
        json.add("region",(this.region as CuboidRegion).asJsonObject)
        val mobs = JsonArray()
        for (mob in this.mobs.values) mobs.add(mob.asJsonObject)
        json.add("mobs",mobs)
        json.addProperty("key-drop-mode",this.keyDropMode.toString())
        json.addProperty("key-material",this.keyMaterial.toString())
        val doors = JsonArray()
        for (door in this.doors.values) doors.add(door.asJsonObject)
        json.add("doors",doors)
        Instantiated.logger().debug("Saved dungeon room '$identifier'")
        return json
    }catch (e: DungeonException){
        throw DungeonExceptions.DataSyntax.consume("static dungeon room '$identifier'",e)
    }
}
val StaticDungeonDoorFormat.asJsonObject : JsonObject get(){
    val json = JsonObject()
    try {
        json.addProperty("identifier",this.identifier.id)
        json.addProperty("base-material",this.baseMaterial.toString())
        json.addProperty("open-material",this.openMaterial.toString())
        json.add("origin",this.origin.asJsonObject)
        json.add("region",this.region.asJsonObject)
        json.add("custom",this.custom.asJsonObject)
        Instantiated.logger().debug("Saved door '$identifier'")
        return json
    } catch (e: Exception) {
        throw DungeonExceptions.DataSyntax.consume("dungeon door '$identifier' at '$origin'", e)
    }
}
val CuboidRegion.asJsonObject : JsonObject get(){
    val json = JsonObject()
    try{
        val region = this.clone()
        region.expand(BlockVector3.ONE)
        json.add("pos-1",region.pos1.asJsonObject)
        json.add("pos-2",region.pos2.asJsonObject)
        return json
    }catch (e:Exception){
        throw DungeonExceptions.DataSyntax.consume("a cuboid region",e)
    }
}
val DungeonMob.asJsonObject : JsonObject get(){
    val json = JsonObject()
    try {
        json.addProperty("identifier",this.identifier.id)
        json.add("spawn-location",this.spawnLocation.toLocation().serialize().asJsonObject)
        json.add("custom",this.custom.asJsonObject)
        json.addProperty("marked",this.isMarked)
        Instantiated.logger().debug("Saved mob '$identifier'")
        return json
    } catch (e: DungeonException) {
        throw DungeonExceptions.DataSyntax.consume("dungeon mob '$identifier'", e)
    }
}
val BlockVector3.asJsonObject : JsonObject get(){
    val json = JsonObject()
    try{
        json.addProperty("x",this.x)
        json.addProperty("y",this.y)
        json.addProperty("z",this.z)
        return json
    }catch (e: DungeonException){
        throw DungeonExceptions.DataSyntax.consume("a block vector",e)
    }
}
val ConvexPolyhedralRegion.asJsonObject : JsonObject get(){
    val json = JsonObject()
    val vertices = JsonArray()
    try{
        for (vertex in this.vertices) vertices.add(vertex.asJsonObject)
        json.add("vertices",vertices)
        return json
    }catch (e: DungeonException){
        throw DungeonExceptions.DataSyntax.consume("a block vector",e)
    }
}