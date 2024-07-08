package dev.munky.instantiated.data.json

import com.google.common.collect.ImmutableMap
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dev.munky.instantiated.Instantiated
import dev.munky.instantiated.config.InstantiatedConfiguration
import dev.munky.instantiated.dungeon.DungeonFormat
import dev.munky.instantiated.dungeon.IdentifiableKey
import dev.munky.instantiated.event.DungeonLoadEvent
import dev.munky.instantiated.event.DungeonStartCacheEvent
import dev.munky.instantiated.exception.DataSyntaxException
import dev.munky.instantiated.exception.DungeonExceptions
import dev.munky.instantiated.oldDungeon.DungeonManager
import dev.munky.instantiated.oldDungeon.DungeonManagerImpl
import dev.munky.instantiated.util.stackMessage
import org.bukkit.Bukkit
import org.jetbrains.annotations.ApiStatus
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import java.util.stream.Collectors
import kotlin.reflect.KClass

class JsonDataHandler : InstantiatedConfiguration(Instantiated.instance(),"dungeons.json") {
    private val cachedData : AtomicReference<BufferedInputStream> = AtomicReference()
    var lastLoadResult : DataOperationResult = DataOperationResult.UNDEFINED
    companion object{
        val MANAGER: DungeonManager = Instantiated.instance().dungeonManager
        val GSON: Gson = Gson().newBuilder().setPrettyPrinting().serializeNulls().create()
        @JvmField
        val SCHEMATICS_FOLDER = File(
            Bukkit.getPluginsFolder().toString() +
                    File.separator + "FastAsyncWorldEdit"
                    + File.separator + "schematics"
        )
        @JvmField
        var REGISTERED_SCHEMATICS : ImmutableMap<String,File> = ImmutableMap.of()
        @ApiStatus.Internal
        private fun rawGetSchematicsOnFile(): Map<String, File> {
            try {
                val schemStream = Files.walk(SCHEMATICS_FOLDER.toPath()).parallel()
                val schematicsOnFile = schemStream
                    .filter { file ->
                        (file.toString().endsWith(".schem")
                                || file.toString().endsWith(".schematic"))
                    }.filter { path -> path!=null }
                    .filter { path -> Files.isRegularFile(path) }
                    .filter { path ->
                        Instantiated.logger().debug("Schematic '" + path.fileName + "' registered")
                        true
                    }
                    .collect(Collectors.toMap(
                        { path -> path.fileName.toString() },
                        { path -> File(path.toString()) }
                    ))
                schemStream.close()
                return schematicsOnFile
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
    }
    fun load() : DataOperationResult{
        // this loads all schematics in folder to a map to be retrieved later
        REGISTERED_SCHEMATICS = ImmutableMap.copyOf(rawGetSchematicsOnFile())
        // this is a list to temporarily store formats until registering them at the end
        val dungeonFormats : MutableMap<IdentifiableKey,DungeonFormat> = HashMap()
        var result : DataOperationResult
        try {
            // if the file doesnt exist or isnt a file, then not found
            if (!file.exists() || !file.isFile) throw DungeonExceptions.DungeonDataFileNotFound.consume(file)
            // read and cache the data using a buffered input stream
            val dataInput = file.inputStream().buffered(8192)
            cachedData.set(dataInput)
            // parse the json, now if there is an error we can use the cache and rewrite whatever data was in there
            val jsonData = JsonParser.parseString(String(dataInput.readAllBytes(), Charsets.UTF_8))
            if (!jsonData.isJsonObject) throw DungeonExceptions.DataSyntax.consume("root json is a '${jsonData.javaClass.simpleName}', not a JsonObject")
            val jsonObject = jsonData.asJsonObject
            val dungeonArray = jsonObject.getField<JsonArray>("dungeons",JsonContext.ROOT_DUNGEON_ARRAY)
            result = DataOperationResult.SUCCESS
            for (dungeonElement in dungeonArray){
                try{
                    if (!dungeonElement.isJsonObject) throw DungeonExceptions.DataSyntax.consume("dungeon json is not an object")
                    val dungeonObject = dungeonElement.asJsonObject
                    val dungeon = dungeonObject.asDungeon
                    val event = DungeonLoadEvent(dungeon, dungeonObject)
                    event.callEvent()
                    if (event.isCancelled) {
                        Instantiated.logger().warning("A load event was cancelled, therefore dungeon '${dungeon.identifier}' will try to not load, but nothing is guaranteed")
                        continue
                    }
                    dungeonFormats[dungeon.identifier] = dungeon
                }catch (e: Exception){
                    Instantiated.logger().severe("Syntax error: ${e.stackMessage()}\n")
                    result = DataOperationResult.PARTIAL_SUCCESS
                }
            }
            MANAGER.dungeons.putAll(dungeonFormats)
            DungeonStartCacheEvent(DungeonManagerImpl.RANDOM_UUID_FOR_CACHING).callEvent()
            Instantiated.logger().info("Registered dungeons: ${dungeonFormats.map { f->f.key.id }.toString().replace("[","").replace("]","")}")
        }catch (e : Exception){
            Instantiated.logger().severe("Error while loading data: " + e.stackMessage())
            // rewrite cached data
            file.outputStream().write(cachedData.get().readAllBytes())
            result = DataOperationResult.FAILURE
        }
        // nix the cached data, as we either replaced the data or didn't need it
        cachedData.getAndSet(null).close()
        // edit mode looks at our last load result, so no need to touch edit mode
        lastLoadResult = result
        return result
    }
    fun save() : DataOperationResult{
        return save(false)
    }
    fun save(force: Boolean) : DataOperationResult{
        var result : DataOperationResult
        try{
            if (!force && lastLoadResult!=DataOperationResult.SUCCESS)
                throw DungeonExceptions.Generic.consume("dungeons did not correctly load, therefore saving is disabled")
            if (!file.exists() || !file.isFile) throw DungeonExceptions.DungeonDataFileNotFound.consume(file)
            val rootObject = JsonObject()
            val dungeonArray = JsonArray()
            result = DataOperationResult.SUCCESS
            for (dungeon in MANAGER.dungeons.values){
                try {
                    dungeonArray.add(dungeon.asJsonObject)
                } catch (e: DataSyntaxException) {
                    Instantiated.logger().severe("Syntax error: ${e.stackMessage()}\n")
                    result = DataOperationResult.PARTIAL_SUCCESS
                }
            }
            rootObject.add("dungeons",dungeonArray)
            file.bufferedWriter(Charsets.UTF_8,8192).use {
                GSON.toJson(rootObject,it)
            }
            Instantiated.instance().editModeHandler.unsavedChanges = false
        }catch (e:Exception){
            Instantiated.logger().severe("Error while saving data: ${e.stackMessage()}\n")
            result = DataOperationResult.FAILURE
        }
        return result
    }
}
enum class DataOperationResult{
    SUCCESS,
    PARTIAL_SUCCESS,
    FAILURE,
    UNDEFINED
}

enum class JsonContext(mapFunction: Consumer<MutableMap<String, KClass<*>>>){
    ROOT_DUNGEON_ARRAY(Consumer{
        it["dungeons"] = JsonArray::class
    }),
    STATIC_DUNGEON(Consumer{
        it["identifier"] = String::class
        it["schematic"] = String::class
        it["spawn"] = JsonObject::class
        it["rooms"] = JsonArray::class
        it["type"] = String::class
    }),
    STATIC_ROOM(Consumer{
        it["identifier"] = String::class
        it["key-drop-mode"] = String::class
        it["key-material"] = String::class
        it["origin"] = JsonObject::class
        it["mobs"] = JsonArray::class
        it["region"] = JsonObject::class
        it["doors"] = JsonArray::class
    }),
    STATIC_DOOR(Consumer{
        it["identifier"] = String::class
        it["base-material"] = String::class
        it["open-material"] = String::class
        it["custom"] = Map::class
        it["origin"] = JsonObject::class
        it["region"] = JsonObject::class
    }),
    CUBOID(Consumer{
        it["pos-1"] = JsonObject::class
        it["pos-2"] = JsonObject::class
    }),
    POLYHEDRAL(Consumer{
        it["vertices"] = JsonArray::class
    }),
    MOB(Consumer{
        it["identifier"] = String::class
        it["marked"] = Boolean::class
        it["custom"] = Map::class
        it["spawn-location"] = JsonObject::class
    }),
    LOCATION(Consumer{
        it["world"] = String::class
        it["x"] = Double::class
        it["y"] = Double::class
        it["z"] = Double::class
        it["pitch"] = Float::class
        it["yaw"] = Float::class
    }),
    VECTOR(Consumer{
        it["x"] = Double::class
        it["y"] = Double::class
        it["z"] = Double::class
    }),
    BLOCK_VECTOR(Consumer{
        it["x"] = Integer::class
        it["y"] = Integer::class
        it["z"] = Integer::class
    }),
    CUSTOM(Consumer{
        it["{}"] = String::class
    });
    private val map : MutableMap<String, KClass<*>> = HashMap()
    fun get() : Map<String, KClass<*>> {return map}
    init{
        mapFunction.accept(map)
    }
}
abstract class JsonPrimitiveContext{
    companion object{
        private val CONTEXT : MutableList<KClass<*>> = ArrayList()
        init{
            CONTEXT.add(String::class)
            CONTEXT.add(Boolean::class)
            CONTEXT.add(Double::class)
            CONTEXT.add(Float::class)
            CONTEXT.add(Integer::class)
            CONTEXT.add(Short::class)
            CONTEXT.add(Character::class)
        }
        fun get() : List<KClass<*>> {return CONTEXT}
    }
}