package dev.munky.instantiated.command

import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIBukkit
import dev.jorel.commandapi.CommandPermission
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.arguments.*
import dev.jorel.commandapi.arguments.CustomArgument.CustomArgumentInfoParser
import dev.jorel.commandapi.arguments.EntitySelectorArgument.ManyPlayers
import dev.jorel.commandapi.executors.CommandArguments
import dev.jorel.commandapi.executors.CommandExecutor
import dev.munky.instantiated.Instantiated
import dev.munky.instantiated.data.json.JsonDataHandler
import dev.munky.instantiated.dungeon.*
import dev.munky.instantiated.dungeon.procedural.ProceduralDungeonFormat
import dev.munky.instantiated.dungeon.sstatic.StaticDungeonFormat
import dev.munky.instantiated.dungeon.sstatic.StaticDungeonRoomFormat
import dev.munky.instantiated.oldDungeon.DungeonManager
import dev.munky.instantiated.util.*
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST") // its just command api argument casting
object DungeonCommand {
    private var MANAGER: DungeonManager? = null
    private var EDIT_MODE = Instantiated.instance().editModeHandler
    @JvmStatic
    fun register(dungeonManager: DungeonManager?) {
        MANAGER = dungeonManager!!
        CommandTree("instantiated")
            .withPermission(CommandPermission.OP)
            .withAliases("inst", "dungeon", "dungeon")
            .then(LiteralArgument("save")
                .executes(CommandExecutor { sender, _ ->
                    Instantiated.instance().saveData()
                    sender.sendMessage(ComponentUtil.toComponent("<green>Saved data"))
                })
                .then(TextArgument("").replaceSuggestions(ArgumentSuggestions.strings(""))
                    .then(BooleanArgument("force")
                        .executes(CommandExecutor { sender, args ->
                            Instantiated.instance().jsonDataHandler.save(args["force"] as Boolean)
                            sender.sendMessage(ComponentUtil.toComponent("<green>Saved data"))
                        }))))
            .then(LiteralArgument("reload")
                .executes(CommandExecutor { sender, _ ->
                    if (EDIT_MODE.unsavedChanges) {
                        EDIT_MODE.unsavedChanges = false
                        throw "<red><bold>You have unsaved changes!<newline><gray><italic>Try again to reload anyway.".asMini.commandApiFail()
                    }
                    try {
                        Instantiated.instance().reload(false)
                        sender.sendMessage("<green>Reloaded plugin".asMini)
                    } catch (e: Exception) {
                        sender.sendMessage("<red>Exception while reloading:<dark_red><newline>${e.stackMessage()}".asMini)
                        e.printStackTrace()
                        Instantiated.logger().severe("Error while reloading above")
                    }
                }))
            .then(LiteralArgument("start")
                .then(ManyPlayers("players")
                    .then(DungeonArgument("dungeon")
                        .then(BooleanArgument("force-create").setOptional(true)
                            .executes(CommandExecutor { sender, args ->
                                val dungeon = args["dungeon"] as Optional<DungeonFormat>
                                if (dungeon.isEmpty)
                                    throw CommandAPIBukkit.failWithAdventureComponent(ComponentUtil.toComponent(
                                        "<red>Dungeon '${args.rawArgsMap()["dungeon"]}' does not exist"
                                    ))
                                var force = args["force-create"] as Boolean?
                                if (force == null) force = false
                                val world = MANAGER!!.dungeonWorld
                                // command api has me covered
                                val players = args["players"] as Collection<Player>?
                                if (players.isNullOrEmpty()) {
                                    sender.sendMessage(ComponentUtil.toComponent("<red>Starting a dungeon with no players"))
                                    MANAGER!!.startDungeon(dungeon.get().identifier.id, force)
                                    return@CommandExecutor
                                }
                                if (world == null)
                                    throw CommandAPIBukkit.failWithAdventureComponent(ComponentUtil.toComponent(
                                        "<red>World not found"
                                    ))
                                if (!MANAGER!!.startDungeon(dungeon.get().identifier.id, force, *players.toTypedArray()))
                                    throw CommandAPIBukkit.failWithAdventureComponent(ComponentUtil.toComponent(
                                        "<red>Could not inject dungeon '${dungeon.get().identifier}'"
                                    ))
                                sender.sendMessage(ComponentUtil.toComponent("<green>Successfully created dungeon, and moved players ${players.map{obj -> obj.name}}"))
                            })))))
            .then(LiteralArgument("leave")
                .executes(CommandExecutor { sender: CommandSender, _: CommandArguments? ->
                    if (sender !is Player) {
                        throw CommandAPI.failWithString("You must be a player to leave a dungeon")
                    }
                    val dungeon =
                        MANAGER!!.getCurrentDungeon(sender.uniqueId)
                    if (dungeon.isEmpty) {
                        throw CommandAPIBukkit.failWithAdventureComponent(ComponentUtil.toComponent("<red>You are not currently inside of a dungeon"))
                    }
                    dungeon.get().removePlayer(sender.uniqueId)
                    sender.sendMessage("<green>Successfully left dungeon ".asMini)
                })
                .then(ManyPlayers("players")
                    .withPermission(CommandPermission.OP)
                    .executes(CommandExecutor { sender: CommandSender, args: CommandArguments ->
                        val world = Bukkit.getWorlds().stream().findFirst()
                        // command api has me covered
                        val players = args["players"] as Collection<Player>
                        if (players.isEmpty()) throw "<red>Selector returned no players".asMini.commandApiFail()
                        if (world.isEmpty) throw "<red>World not found".asMini.commandApiFail()
                        for (player in players) {
                            val dungeon = MANAGER!!.getCurrentDungeon(player.uniqueId)
                            dungeon.ifPresent { it.removePlayer(player.uniqueId) }
                        }
                        sender.sendMessage(ComponentUtil.toComponent("<green>Successfully teleported players ${players.map{ obj: Player -> obj.name }}"))
                    })))
            .then(LiteralArgument("opendoors")
                .executes(CommandExecutor { sender: CommandSender, _: CommandArguments? ->
                    if (sender !is Player) {
                        throw "Only player's may use this command".asMini.commandApiFail()
                    }
                    val maybeDungeon =
                        MANAGER!!.getCurrentDungeon(sender.uniqueId)
                    if (maybeDungeon.isEmpty) {
                        throw "<red>You are not in a dungeon".asMini.commandApiFail()
                    }
                    val dungeon = maybeDungeon.get()
                    val closest = dungeon.getClosestRoom(sender)
                    if (closest.isEmpty) {
                        throw "<red>The closest room is too far".asMini.commandApiFail()
                    }
                    closest.get().doors.values.forEach { it.open(false) }
                    sender.sendMessage(("<green>Opened every door in '" + closest.get().identifier + "' in '" + closest.get().parent.identifier + "'").asMini)
                }))
            .then(LiteralArgument("edit")
                .executes(CommandExecutor { sender, _ ->
                    if (sender !is Player){
                        sender.sendMessage("<red>You must be a player to use this command, did you mean <i><aqua>/edit <static | procedural> <dungeon-name></aqua></i> ?")
                        return@CommandExecutor
                    }
                    if (sender.uniqueId in EDIT_MODE.playersInEditMode) {
                        EDIT_MODE.takeOutOfEditMode(sender)
                        sender.sendMessage("<green>Stopped editing".asMini)
                    }else{
                        EDIT_MODE.putInEditMode(sender)
                        sender.sendMessage("<green>Now editing".asMini)
                    }
                })
                .then(LiteralArgument("static")
                    .then(DungeonArgument("dungeon", DungeonArgument.DungeonType.STATIC)
                        .then(LiteralArgument("set-schematic")
                            .then(schematic()
                                .executes(CommandExecutor{sender,args->
                                    val schematicName = args["schematic"] as String
                                    val dungeonOp = args["dungeon"] as Optional<StaticDungeonFormat>
                                    if (dungeonOp.isEmpty) throw "<red>Dungeon '${args.rawArgsMap["dungeon"]}' not found".asMini.commandApiFail()
                                    val schematic = JsonDataHandler.REGISTERED_SCHEMATICS[schematicName].asOptional
                                    if (schematic.isEmpty) throw "<red>Schematic '$schematicName' not found".asMini.commandApiFail()
                                    dungeonOp.get().schematic = schematic.get()
                                    dungeonOp.get().instances.forEach{it.remove(InstancedDungeon.RemovalReason.FORMAT_CHANGE,true)}
                                    sender.sendMessage("<green>Set schematic of dungeon '${dungeonOp.get().identifier.id}' to '$schematicName'".asMini)
                                })))
                        .then(LiteralArgument("set-spawn")
                            .then(LocationArgument("new-location")
                                .executes(CommandExecutor{sender,args->
                                    val spawnLocation = args["new-location"] as Location
                                    val dungeonOp = args["dungeon"] as Optional<StaticDungeonFormat>
                                    if (dungeonOp.isEmpty) throw "<red>Dungeon '${args.rawArgsMap["dungeon"]}' not found".asMini.commandApiFail()
                                    dungeonOp.get().spawnVector = spawnLocation.toVector()
                                    dungeonOp.get().instances.forEach{it.remove(InstancedDungeon.RemovalReason.FORMAT_CHANGE,true)}
                                    sender.sendMessage("<green>Set spawn of dungeon '${dungeonOp.get().identifier.id}' to '${spawnLocation.toVector()}'".asMini)
                                })))
                        .then(LiteralArgument("add-room")
                            .then(TextArgument("room-id")
                                .replaceSuggestions(ArgumentSuggestions.stringCollectionAsync {
                                    val dungeonOp = it.previousArgs["dungeon"] as Optional<StaticDungeonFormat>
                                    if (dungeonOp.isEmpty)
                                        CompletableFuture.completedFuture(listOf("<red>Dungeon '${it.previousArgs.rawArgsMap["dungeon"]}' not found"))
                                    else
                                        CompletableFuture.completedFuture(dungeonOp.get().rooms.keys.map{k->k.id})
                                })
                                .executes(CommandExecutor{sender,args->
                                    val roomId = args["room-id"] as String
                                    val dungeonOp = args["dungeon"] as Optional<StaticDungeonFormat>
                                    if (dungeonOp.isEmpty) throw "<red>Dungeon '${args.rawArgsMap["dungeon"]}' not found".asMini.commandApiFail()
                                    val roomKey = IdentifiableKey(IdentifiableType.ROOM,roomId)
                                    if (dungeonOp.get().rooms.containsKey(roomKey)) throw "<red>Room '$roomId' already exists!".asMini.commandApiFail()
                                    val room = StaticDungeonRoomFormat(
                                        roomKey,
                                        dungeonOp.get(),
                                        BlockVector3.ZERO,
                                        CuboidRegion(
                                            BukkitAdapter.adapt(MANAGER!!.dungeonWorld),
                                            BlockVector3.ZERO,
                                            BlockVector3.at(5,5,5)
                                        ),
                                        HashMap(),
                                        DungeonRoomFormat.KeyDropMode.ROOM_MOBS_CLEAR,
                                        Material.GOLD_INGOT
                                    )
                                    dungeonOp.get().rooms[roomKey] = room
                                    dungeonOp.get().instances.forEach{it.remove(InstancedDungeon.RemovalReason.FORMAT_CHANGE,true)}
                                    sender.sendMessage("<green>Added room '$roomId' to dungeon '${dungeonOp.get().identifier.id}'".asMini)
                                })))
                        .then(LiteralArgument("remove-room")
                            .then(TextArgument("room-id")
                                .replaceSuggestions(ArgumentSuggestions.stringCollectionAsync {
                                    val dungeonOp = it.previousArgs["dungeon"] as Optional<StaticDungeonFormat>
                                    if (dungeonOp.isEmpty)
                                        CompletableFuture.completedFuture(listOf("<red>Dungeon '${it.previousArgs.rawArgsMap["dungeon"]}' not found"))
                                    else
                                        CompletableFuture.completedFuture(dungeonOp.get().rooms.keys.map{k->k.id})
                                })
                                .executes(CommandExecutor{sender,args->
                                    val roomId = args["room-id"] as String
                                    val dungeonOp = args["dungeon"] as Optional<StaticDungeonFormat>
                                    if (dungeonOp.isEmpty) throw "<red>Dungeon '${args.rawArgsMap["dungeon"]}' not found".asMini.commandApiFail()
                                    val roomKey = IdentifiableKey(IdentifiableType.ROOM,roomId)
                                    if (!dungeonOp.get().rooms.containsKey(roomKey)) throw "<red>Room '$roomId' not found".asMini.commandApiFail()
                                    dungeonOp.get().rooms.remove(roomKey)
                                    dungeonOp.get().instances.forEach{it.remove(InstancedDungeon.RemovalReason.FORMAT_CHANGE,true)}
                                    sender.sendMessage("<green>Removed room '$roomId' from dungeon '${dungeonOp.get().identifier.id}'".asMini)
                                })))
                    )
                )
                .then(LiteralArgument("procedural")
                )
            )
            .register()
    }

    private fun schematic(): Argument<String> {
        return StringArgument("schematic").replaceSuggestions(ArgumentSuggestions.stringsAsync {
            CompletableFuture.supplyAsync {
                if (JsonDataHandler.REGISTERED_SCHEMATICS.isEmpty()) arrayOf("Folder is empty")
                JsonDataHandler.REGISTERED_SCHEMATICS.keys.toTypedArray()
            }
        })
    }
    /**
     * Returns an optional dungeon, in-case one does not exist with the identifier
     * @param nodeName self-explanatory, read command api
     */
    class DungeonArgument(
        nodeName: String,
        private val type: DungeonType = DungeonType.NONE
    ) : CustomArgument<Optional<DungeonFormat>, String>(
        TextArgument(nodeName),
        CustomArgumentInfoParser { info ->
            val dungeon = MANAGER!!.dungeons[IdentifiableKey(IdentifiableType.DUNGEON, info.input())].asOptional
            if (dungeon.isPresent && !type.clazz.isInstance(dungeon.get()))// don't want to do type checking in args
                emptyOptional()
            else dungeon
        }
    ) {
        init {
            this.replaceSuggestions(ArgumentSuggestions.stringCollectionAsync {
                CompletableFuture.completedFuture(MANAGER!!.dungeons.values.filter { type.clazz.isInstance(it) }.map { d: DungeonFormat -> d.identifier.id })
            })
        }
        enum class DungeonType(kClass: KClass<*>) {
            STATIC(StaticDungeonFormat::class),
            PROCEDURAL(ProceduralDungeonFormat::class),
            NONE(Any::class),
            LOBBY(StaticDungeonFormat::class);
            val clazz = kClass
        }
    }
}