package dev.munky.instantiated.data.json

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.ConvexPolyhedralRegion
import com.sk89q.worldedit.regions.CuboidRegion
import dev.munky.instantiated.Instantiated
import dev.munky.instantiated.Instantiated.logger
import dev.munky.instantiated.dungeon.DungeonFormat
import dev.munky.instantiated.dungeon.DungeonRoomFormat
import dev.munky.instantiated.dungeon.IdentifiableKey
import dev.munky.instantiated.dungeon.IdentifiableType
import dev.munky.instantiated.dungeon.mob.DungeonMob
import dev.munky.instantiated.dungeon.procedural.ProceduralDungeonFormat
import dev.munky.instantiated.dungeon.sstatic.StaticDungeonDoorFormat
import dev.munky.instantiated.dungeon.sstatic.StaticDungeonFormat
import dev.munky.instantiated.dungeon.sstatic.StaticDungeonRoomFormat
import dev.munky.instantiated.exception.DungeonException
import dev.munky.instantiated.exception.DungeonExceptions
import dev.munky.instantiated.exception.DungeonExceptions.Companion.DataSyntax
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.tuple.Pair
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.util.Vector
import kotlin.reflect.cast

val JsonObject.asDungeon : DungeonFormat get(){
    var identifier = "Not yet loaded"
    var type = "unknown"
    try{
        identifier = this.getField("identifier",JsonContext.STATIC_DUNGEON)
        type = this.getField("type",JsonContext.STATIC_DUNGEON)
        return when (type){
            "static" -> this.asStaticDungeon
            "procedural" -> this.asProceduralDungeon
            "lobby" -> TODO("UHHHH")
            else -> throw DataSyntax.consume("unhandled dungeon type '$type'")
        }
    }catch (e:Exception){
        throw DataSyntax.consume("$type dungeon '$identifier'",e)
    }
}
val JsonObject.asStaticDungeon : StaticDungeonFormat get() {
    val identifier : String = this.getField("identifier",JsonContext.STATIC_DUNGEON)
    val schematic : String = this.getField("schematic",JsonContext.STATIC_DUNGEON)
    val schemFile = JsonDataHandler.REGISTERED_SCHEMATICS[schematic]
        ?: throw DataSyntax.consume("schematic '$schematic' does not exist")
    val spawnVector : Vector = this.getField<JsonObject>("spawn",JsonContext.STATIC_DUNGEON).asVector
    val dungeon = StaticDungeonFormat(IdentifiableKey(IdentifiableType.DUNGEON,identifier),schemFile,spawnVector)
    val roomArray : JsonArray = this.getField("rooms",JsonContext.STATIC_DUNGEON)
    for (roomElement in roomArray){
        if (!roomElement.isJsonObject) throw DataSyntax.consume("a room is not a json object (map of values)")
        val dungeonRoom : StaticDungeonRoomFormat = roomElement.asJsonObject.asStaticRoom(dungeon)
        dungeon.rooms[dungeonRoom.identifier] = dungeonRoom
    }
    logger().debug("Loaded dungeon '$identifier'")
    return dungeon
}
fun JsonObject.asStaticRoom(parent:StaticDungeonFormat) : StaticDungeonRoomFormat {
    var identifier = "Not yet loaded"
    try{
        identifier = this.getField("identifier",JsonContext.STATIC_ROOM)
        val origin : BlockVector3 = this.getField<JsonObject>("origin",JsonContext.STATIC_ROOM).asBlockVector
        val region : CuboidRegion = this.getField<JsonObject>("region",JsonContext.STATIC_ROOM).asCuboidRegion
        val mobs : Map<IdentifiableKey, DungeonMob> = this.getField<JsonArray>("mobs",JsonContext.STATIC_ROOM).asMobs
        val sKeyDropMode : String = this.getField("key-drop-mode", JsonContext.STATIC_ROOM)
        val sKeyMaterial : String = this.getField("key-material", JsonContext.STATIC_ROOM)
        val keyMaterial = Material.matchMaterial(sKeyMaterial)
            ?: throw DataSyntax.consume("Key Material ($sKeyMaterial) not found")
        val keyDropMode: DungeonRoomFormat.KeyDropMode
        try {
            keyDropMode = DungeonRoomFormat.KeyDropMode.valueOf(sKeyDropMode.uppercase())
        } catch (e: IllegalArgumentException) {
            throw DataSyntax.consume("key drop mode '$sKeyDropMode' does not exist, must be one of " + DungeonRoomFormat.KeyDropMode.entries.toTypedArray().contentToString())
        }
        val room = StaticDungeonRoomFormat(
            IdentifiableKey(IdentifiableType.ROOM,identifier),
            parent,
            origin,
            region,
            mobs,
            keyDropMode,
            keyMaterial
        )
        val doors : Map<IdentifiableKey,StaticDungeonDoorFormat> = this.getField<JsonArray>("doors",JsonContext.STATIC_ROOM).asStaticDoors(room)
        for (door in doors) room.doors[door.key] = door.value
        logger().debug("Loaded room '$identifier'")
        return room
    }catch (e:Exception){
        throw DataSyntax.consume("static dungeon room '$identifier'", e)
    }
}
val JsonObject.asProceduralDungeon : ProceduralDungeonFormat get() {
    var identifier = "Not yet loaded"
    try{
        throw DungeonExceptions.DungeonNotFound.consume("There is no such thing as procedural yet")
    }catch (e:Exception){
        throw DataSyntax.consume("procedural dungeon '$identifier'",e)
    }
}
val JsonObject.asVector : Vector get() {
    try{
        val x = getField<Double>("x",JsonContext.VECTOR)
        val y = getField<Double>("y",JsonContext.VECTOR)
        val z = getField<Double>("z",JsonContext.VECTOR)
        return Vector(x,y,z)
    }catch (e:Exception){
        throw DataSyntax.consume("a vector", e)
    }
}
val JsonObject.asCuboidRegion : CuboidRegion get() {
    try{
        val pos1 = getField<JsonObject>("pos-1",JsonContext.CUBOID).asBlockVector
        val pos2 = getField<JsonObject>("pos-2",JsonContext.CUBOID).asBlockVector
        val reg = CuboidRegion(pos1,pos2)
        reg.contract(BlockVector3.ONE.multiply(-1))
        return reg
    }catch (e:Exception){
        throw DataSyntax.consume("a cuboid region", e)
    }
}
val JsonObject.asBlockVector : BlockVector3 get() {
    try{
        val x = getField<Int>("x",JsonContext.VECTOR)
        val y = getField<Int>("y",JsonContext.VECTOR)
        val z = getField<Int>("z",JsonContext.VECTOR)
        return BlockVector3.at(x,y,z)
    }catch (e:Exception){
        throw DataSyntax.consume("a block vector", e)
    }
}
val JsonObject.asLocation : Location get() {
    try{
        val x = getField<Double>("x",JsonContext.LOCATION)
        val y = getField<Double>("y",JsonContext.LOCATION)
        val z = getField<Double>("z",JsonContext.LOCATION)
        val yaw = getField<Float>("yaw",JsonContext.LOCATION)
        val pitch = getField<Float>("pitch",JsonContext.LOCATION)
        return Location(Instantiated.instance().dungeonManager.dungeonWorld,x,y,z,yaw,pitch)
    }catch (e:Exception){
        throw DataSyntax.consume("a location", e)
    }
}
val JsonObject.asPolyhedralRegion : ConvexPolyhedralRegion get() {
    try {
        val region = ConvexPolyhedralRegion(BukkitAdapter.adapt(Instantiated.instance().dungeonManager.dungeonWorld))
        for (jsonElement in this.getField<JsonArray>("vertices",JsonContext.POLYHEDRAL)) {
            if (!jsonElement.isJsonObject) throw DataSyntax.consume("vertex is not an object (map of values)")
            val vertex = jsonElement.asJsonObject.asBlockVector
            val didChange = region.addVertex(vertex)
            if (!didChange) logger().debug("vertex not added $vertex")
        }
        return region
    } catch (e: Exception) {
        throw DataSyntax.consume("a polyhedral region", e)
    }
}
val JsonArray.asMobs : Map<IdentifiableKey, DungeonMob> get() {
    val mobs: MutableMap<IdentifiableKey, DungeonMob> = HashMap()
    for (jsonElement in this.asList()) {
        var identifier = "Not yet loaded"
        if (!jsonElement.isJsonObject) throw DataSyntax.consume("a mob is not an object (map of values)")
        try {
            val mobJson = jsonElement.asJsonObject
            identifier = mobJson.getField("identifier", JsonContext.MOB)
            val custom : Map<String,String> = mobJson.getField("custom",JsonContext.MOB)
            val type = custom["type"].let {
                if (it == null) throw DataSyntax.consume("custom map does not contain a type, while it is required for mobs (\"type\":\"simple\" ?)")
                it
            }
            val generator = DungeonMob.MOB_TYPES[type].let{
                if (it==null) throw DataSyntax.consume("type '$type' was not registered, so there is no associated generator")
                it
            }
            val mob = generator.apply(mobJson)
            logger().debug("Loaded $type mob '$identifier'")
            mobs[mob.identifier] = mob
        } catch (e: Exception) {
            throw DataSyntax.consume("dungeon mob '$identifier'", e)
        }
    }
    return mobs
}
fun JsonArray.asStaticDoors(parent: StaticDungeonRoomFormat) : Map<IdentifiableKey,StaticDungeonDoorFormat> {
    val map: MutableMap<IdentifiableKey, StaticDungeonDoorFormat> = HashMap()
    for (jsonElement in this.asList()) {
        var identifier = "Not yet loaded"
        var origin: BlockVector3? = null
        if (!jsonElement.isJsonObject) throw DataSyntax.consume("a door is not an object (map of values)")
        try {
            val doorJson = jsonElement.asJsonObject
            identifier = doorJson.getField("identifier", JsonContext.STATIC_DOOR)
            val sBaseMaterial = doorJson.getField<String>("base-material", JsonContext.STATIC_DOOR)
            val sOpenMaterial = doorJson.getField<String>("open-material", JsonContext.STATIC_DOOR)
            origin = doorJson.getField<JsonObject>("origin",  JsonContext.STATIC_DOOR).asBlockVector
            val region = doorJson.getField<JsonObject>("region", JsonContext.STATIC_DOOR).asCuboidRegion
            val custom = doorJson.getField<Map<String, String>>("custom", JsonContext.STATIC_DOOR)
            val baseMaterial = Material.matchMaterial(sBaseMaterial)
            val openMaterial = Material.matchMaterial(sOpenMaterial)
            if (baseMaterial == null) throw DataSyntax.consume("base material ($sBaseMaterial) not found")
            if (openMaterial == null) throw DataSyntax.consume("open material ($sOpenMaterial) not found")
            val door = StaticDungeonDoorFormat(
                parent, IdentifiableKey(IdentifiableType.DOOR,identifier), region, origin,
                baseMaterial, openMaterial, custom
            )
            logger().debug("Loaded door '$identifier'")
            map[door.identifier] = door
        } catch (e: Exception) {
            throw DataSyntax.consume("dungeon door '$identifier' at '$origin'", e)
        }
    }
    return map
}
val <K,V> Map<K,V>.asJsonObject : JsonObject get() {
    try{
        return JsonDataHandler.GSON.toJsonTree(this, object : TypeToken<Map<K?, V?>?>() {}.type).asJsonObject
    }catch (e:Exception){
        throw DataSyntax.consume("a map")
    }
}

@Suppress("deprecation") // I NEED LEVENSHTEIN DISTANCE!!!
@Throws(DungeonException::class)
fun <T> JsonObject.getField(key: String, context: JsonContext): T {
    try {
        val parent = this
        if (context.get().keys.none { k -> k == key }) {
            throw DataSyntax.consume("context '$context' does not contain field '$key'")
        }
        var contextualClass = context.get()[key]
        if (contextualClass == null && context === JsonContext.CUSTOM) {
            contextualClass = String::class
        }
        assert(contextualClass != null)
        contextualClass = contextualClass!!
        var value: Any? = null
        val element = parent[key]
        if (element == null || element.isJsonNull) {
            val closest = parent.asMap().keys.stream()
                .map { member -> Pair.of(member, StringUtils.getLevenshteinDistance(member, key)) }
                .filter { p-> p.value < 5 }
                .sorted(Comparator.comparingInt { obj: Pair<String, Int> -> obj.value })
                .map { obj -> obj.left }
                .findFirst()
            if (closest.isPresent) throw DataSyntax.consume("field not found, yet a close match (${closest.get()}) was found")
            else throw DataSyntax.consume("field not found")
        }
        if (JsonPrimitiveContext.get().contains(contextualClass)) {
            if (!element.isJsonPrimitive) {
                throw DataSyntax.consume("field '$key' is not a(n) ${contextualClass.simpleName}")
            }
            val prim = element.asJsonPrimitive
            if (contextualClass == String::class) {
                if (!prim.isString) throw DataSyntax.consume("field '$key' is not a(n) ${contextualClass.simpleName}")
                value = prim.asString.lowercase().replace(" ","_").trim()
            }
            if (contextualClass == Int::class) {
                if (!prim.isNumber) throw DataSyntax.consume("field '$key' is not a(n) ${contextualClass.simpleName}")
                value = prim.asInt
            }
            if (contextualClass == Double::class) {
                if (!prim.isNumber) throw DataSyntax.consume("field '$key' is not a(n) ${contextualClass.simpleName}")
                value = prim.asDouble
            }
            if (contextualClass == Boolean::class) {
                if (!prim.isBoolean) throw DataSyntax.consume("field '$key' is not a(n) ${contextualClass.simpleName}")
                value = prim.asBoolean
            }
            if (contextualClass == Float::class) {
                if (!prim.isNumber) throw DataSyntax.consume("field '$key' is not a(n) ${contextualClass.simpleName}")
                value = prim.asFloat
            }
            if (contextualClass == Short::class) {
                if (!prim.isNumber) throw DataSyntax.consume("field '$key' is not a(n) ${contextualClass.simpleName}")
                value = prim.asShort
            }
            if (contextualClass == Char::class) {
                if (!prim.isString) throw DataSyntax.consume("field '$key' is not a(n) ${contextualClass.simpleName}")
                value = prim.asString[0]
            }
        }
        if (contextualClass == JsonObject::class) {
            if (!element.isJsonObject) throw DataSyntax.consume("field '$key' is not a(n) ${contextualClass.simpleName}")
            value = element.asJsonObject
        }
        if (contextualClass == MutableMap::class) {
            if (!element.isJsonObject) throw DataSyntax.consume("field '$key' is not a(n) ${contextualClass.simpleName}")
            val jsonMap = element.asJsonObject.asMap()
            value = jsonMap.entries.associate {
                val elem = it.value
                if (!elem.isJsonPrimitive) throw DataSyntax.consume("value in custom map is not a primitive")
                Pair(it.key,elem.asString)
            }
        }
        if (contextualClass == JsonArray::class) {
            if (!element.isJsonArray) throw DataSyntax.consume("field '$key' is not a(n) ${contextualClass.simpleName}")
            value = element.asJsonArray
        }
        if (!contextualClass.isInstance(value)) {
            logger()
                .severe("This is probably a CONTEXT issue (primitive wrapper classes vs primitives remember?), so please contact DevMunky with the entire log")
            logger().severe("value = $value, context = $contextualClass, element = $element")
            throw DataSyntax.consume("field '$key' is not a(n) ${contextualClass.simpleName}")
        }
        return contextualClass.cast(value) as T
    } catch (e: Exception) {
        throw DataSyntax.consume("field '$key'", e)
    }
}