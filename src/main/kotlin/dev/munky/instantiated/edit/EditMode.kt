package dev.munky.instantiated.edit

import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import dev.munky.instantiated.Instantiated
import dev.munky.instantiated.data.json.DataOperationResult
import dev.munky.instantiated.data.json.JsonDataHandler
import dev.munky.instantiated.dungeon.DungeonRoomFormat
import dev.munky.instantiated.dungeon.InstancedDungeon
import dev.munky.instantiated.dungeon.InstancedDungeonDoor
import dev.munky.instantiated.dungeon.InstancedDungeonRoom
import dev.munky.instantiated.dungeon.mob.DungeonMob
import dev.munky.instantiated.dungeon.sstatic.StaticInstancedDungeon
import dev.munky.instantiated.event.ListenerFactory
import dev.munky.instantiated.util.ComponentUtil
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.function.BiConsumer

class EditModeHandler{
    private val playersInEditMode0: MutableList<UUID> = mutableListOf()
    private val previousInventory: MutableMap<UUID, Array<ItemStack>> = mutableMapOf()
    val playersInEditMode: List<UUID> get() = playersInEditMode0
    var unsavedChanges get() = EditModeHandler.unsavedChanges
        set(value) {
            EditModeHandler.unsavedChanges = value
        }
    companion object{
        var unsavedChanges = false
    }
    init{
        //region events
        ListenerFactory.registerEvent(PlayerInteractEvent::class.java, EventPriority.LOWEST) { e ->
            if (e.hand != EquipmentSlot.HAND) return@registerEvent
            val dungeon =
                Instantiated.instance().dungeonManager.getCurrentDungeon(e.player.uniqueId)
            if (dungeon.isEmpty) return@registerEvent
            val room = dungeon.get().getClosestRoom(e.player)
            if (room.isEmpty) return@registerEvent
            var point = e.interactionPoint
            if (point == null && e.clickedBlock != null) point = e.clickedBlock!!.location
            val interaction = EditToolInteraction(e, dungeon.get(), room.get(), point)
            if (EditTool.execute(interaction)) unsavedChanges = true
        }
        // stops any interaction with edit mode items
        ListenerFactory.registerEvent(InventoryClickEvent::class.java) { e ->
            if (!this.playersInEditMode0.contains(e.whoClicked.uniqueId)) return@registerEvent
            val currentItem = e.currentItem
            if (EditTool.tools.map { it.value.item }.any { it == currentItem }) {
                e.isCancelled = true
            }
        }
        //endregion
    }
    fun putInEditMode(player: Player) {
        if (Instantiated.instance().jsonDataHandler.lastLoadResult!=DataOperationResult.SUCCESS) {
            player.sendMessage(ComponentUtil.toComponent("<red>Cannot enter edit mode because there was an error, and we want to preserve as much data as possible"))
            Instantiated.logger()
                .debug("Denied edit mode access due to invalid loading state -> ${Instantiated.instance().jsonDataHandler.lastLoadResult}")
            return
        }
        playersInEditMode0.add(player.uniqueId)
        previousInventory[player.uniqueId] = player.inventory.contents.mapNotNull { it }.toTypedArray()
        player.inventory.clear()
        player.inventory.setItem(0, EditTool.Vertex.item)
        player.inventory.setItem(2, EditTool.Entity.item)
        player.inventory.setItem(4, EditTool.Door.item)
        player.inventory.setItem(6, EditTool.Config.item)
    }
    fun takeOutOfEditMode(player: Player) {
        playersInEditMode0.remove(player.uniqueId)
        player.inventory.clear()
        player.inventory.contents = previousInventory[player.uniqueId]
        previousInventory.remove(player.uniqueId)
    }
    fun onDisable(){
        val editMode = playersInEditMode0
        editMode.mapNotNull{
            Bukkit.getPlayer(it)
        }.forEach { takeOutOfEditMode(it) }
        playersInEditMode0.clear()
    }
    val Player.isInEditMode : Boolean get() {
        return playersInEditMode0.contains(this.uniqueId)
    }
}
fun getCuboidFromNewCorner(region: CuboidRegion, newCorner: BlockVector3): CuboidRegion {
    val corners = getCuboidOppositeMap(region)
    var closest = corners.keys.first()
    var closestDistance = closest.distance(newCorner)
    for (corner in corners.keys) {
        val distance = corner.distance(newCorner)
        if (distance < closestDistance) {
            closest = corner
            closestDistance = distance
        }
    }
    val mapping = corners[closest]!!
    val finalClosest = closest
    val opposite = corners.entries.first { e->
        e.value == mapping && e.key !== finalClosest
    }.key
    Instantiated.logger().debug("interaction:$newCorner,opposite:$opposite,closest:$closest")
    return CuboidRegion(newCorner,opposite)
}
fun getCuboidOppositeMap(region: CuboidRegion): Map<BlockVector3, Int> {
    val minX = region.minimumX
    val minY = region.minimumY
    val minZ = region.minimumZ
    val maxX = region.maximumX
    val maxY = region.maximumY
    val maxZ = region.maximumZ
    return java.util.Map.of(
        BlockVector3.at(minX, minY, minZ), 0,
        BlockVector3.at(minX, minY, maxZ), 1,
        BlockVector3.at(minX, maxY, minZ), 2,
        BlockVector3.at(minX, maxY, maxZ), 3,
        BlockVector3.at(maxX, minY, minZ), 3,
        BlockVector3.at(maxX, minY, maxZ), 2,
        BlockVector3.at(maxX, maxY, minZ), 1,
        BlockVector3.at(maxX, maxY, maxZ), 0
    )
}
object ChatQuestions{
    fun getRoomConfigQuestion(room: InstancedDungeonRoom): Component {
        return Component
            .text("This message will expire after 25 seconds").color(NamedTextColor.RED).decorate(TextDecoration.BOLD)
            .appendNewline()
            .append(ComponentUtil.toComponent("<gold>Configuring room '" + room.identifier + "'"))
            .appendNewline()
            .append(Component.text("identifier").color(NamedTextColor.AQUA))
            .appendNewline()
            .append(Component.text("key-drop-mode").color(NamedTextColor.AQUA))
            .appendNewline()
            .append(ComponentUtil.Questionnaire.create(
                ComponentUtil.toComponent("<blue>-> ROOM_MOBS_CLEAR"),
                false
            ) { player: Audience ->
                room.master.keyDropMode = DungeonRoomFormat.KeyDropMode.ROOM_MOBS_CLEAR
                player.sendMessage(ComponentUtil.toComponent("<green>Set key drop mode to ROOM_MOBS_CLEAR"))
            })
            .appendNewline()
            .append(ComponentUtil.Questionnaire.create(
                ComponentUtil.toComponent("<blue>-> MARKED_ROOM_MOB_KILL"),
                false
            ) { player: Audience ->
                room.master.keyDropMode = DungeonRoomFormat.KeyDropMode.MARKED_ROOM_MOB_KILL
                player.sendMessage(ComponentUtil.toComponent("<green>Set key drop mode to MARKED_ROOM_MOB_KILL"))
            })
            .appendNewline()
            .append(Component.text("key-material").color(NamedTextColor.AQUA))
            .appendNewline()
            .append(ComponentUtil.Questionnaire.create(
                ComponentUtil.toComponent("<yellow>-> ENTER_MATERIAL"),
                false
            ) { player: Audience ->
                player.sendMessage(ComponentUtil.toComponent("<blue>Enter the new material of the key"))
                ListenerFactory.registerEvent<AsyncChatEvent>(
                    AsyncChatEvent::class.java,
                    BiConsumer<AsyncChatEvent, Listener> { e: AsyncChatEvent, l: Listener? ->
                        if (e.player !== player) return@BiConsumer
                        e.isCancelled = true
                        val sMaterial = ComponentUtil.toString(e.message())
                        val material = Material.matchMaterial(sMaterial.replace(" ".toRegex(), "_"))
                        if (material == null) {
                            player.sendMessage(
                                ComponentUtil.toComponent(
                                    "<red>Non-existent material '%s'".formatted(
                                        sMaterial
                                    )
                                )
                            )
                        } else {
                            room.master.keyMaterial = material
                            player.sendMessage(ComponentUtil.toComponent("<green>Set key material of room to " + material.key))
                        }
                        HandlerList.unregisterAll(l!!)
                    })
            })
            .appendNewline()
    }
    fun getDoorConfigQuestion(door: InstancedDungeonDoor): Component {
        return Component
            .text("This message will expire after 25 seconds").color(NamedTextColor.RED).decorate(TextDecoration.BOLD)
            .appendNewline()
            .append(ComponentUtil.toComponent("<gold>Configuring door '" + door.master.identifier + "'"))
            .appendNewline()
            .append(Component.text("identifier").color(NamedTextColor.AQUA))
            .appendNewline()
            .append(Component.text("base-material").color(NamedTextColor.AQUA))
            .appendNewline()
            .append(ComponentUtil.Questionnaire.create(
                ComponentUtil.toComponent("<yellow>-> ENTER_MATERIAL"),
                false
            ) { player: Audience ->
                player.sendMessage(ComponentUtil.toComponent("<blue>Enter the new material"))
                ListenerFactory.registerEvent<AsyncChatEvent>(
                    AsyncChatEvent::class.java,
                    BiConsumer { e: AsyncChatEvent, l: Listener? ->
                        if (e.player !== player) return@BiConsumer
                        e.isCancelled = true
                        val sMaterial = ComponentUtil.toString(e.message())
                        val material = Material.matchMaterial(sMaterial.replace(" ".toRegex(), "_"))
                        if (material == null) {
                            player.sendMessage(
                                ComponentUtil.toComponent(
                                    "<red>Non-existent material '%s'".formatted(
                                        sMaterial
                                    )
                                )
                            )
                        } else {
                            door.master.baseMaterial = material
                            player.sendMessage(ComponentUtil.toComponent("<green>Set base material of door to " + material.key))
                        }
                        HandlerList.unregisterAll(l!!)
                    })
            })
            .appendNewline()
            .append(Component.text("open-material").color(NamedTextColor.AQUA))
            .appendNewline()
            .append(ComponentUtil.Questionnaire.create(
                ComponentUtil.toComponent("<yellow>-> ENTER_MATERIAL"),
                false
            ) { player: Audience ->
                player.sendMessage(ComponentUtil.toComponent("<blue>Enter the new material"))
                ListenerFactory.registerEvent<AsyncChatEvent>(
                    AsyncChatEvent::class.java,
                    BiConsumer<AsyncChatEvent, Listener> { e: AsyncChatEvent, l: Listener? ->
                        if (e.player !== player) return@BiConsumer
                        e.isCancelled = true
                        val sMaterial = ComponentUtil.toString(e.message())
                        val material = Material.matchMaterial(sMaterial.replace(" ".toRegex(), "_"))
                        if (material == null) {
                            player.sendMessage(
                                ComponentUtil.toComponent(
                                    "<red>Non-existent material '%s'".formatted(
                                        sMaterial
                                    )
                                )
                            )
                        } else {
                            door.master.baseMaterial = material
                            player.sendMessage(ComponentUtil.toComponent("<green>Set open material of door to " + material.key))
                        }
                        HandlerList.unregisterAll(l!!)
                    })
            })
            .appendNewline()
    }
    fun getDungeonConfigQuestion(dungeon: InstancedDungeon): Component {
        return Component
            .text("This message will expire after 25 seconds").color(NamedTextColor.RED).decorate(TextDecoration.BOLD)
            .appendNewline()
            .append(ComponentUtil.toComponent("<gold>Configuring dungeon '" + dungeon.identifier + "'"))
            .appendNewline()
            .append(Component.text("identifier").color(NamedTextColor.AQUA))
            .appendNewline()
            .append(Component.text("schematic"))
            .appendNewline()
            .append(ComponentUtil.Questionnaire.create(
                ComponentUtil.toComponent("<yellow>-> ENTER_SCHEMATIC_FILE_NAME"),
                false
            ) { player: Audience ->
                player.sendMessage(ComponentUtil.toComponent("<blue>Enter the schematic file name"))
                ListenerFactory.registerEvent<AsyncChatEvent>(
                    AsyncChatEvent::class.java,
                    BiConsumer<AsyncChatEvent, Listener> { e: AsyncChatEvent, l: Listener? ->
                        if (e.player !== player) return@BiConsumer
                        e.isCancelled = true
                        val sFileName = ComponentUtil.toString(e.message())
                        var schematicFile = JsonDataHandler.REGISTERED_SCHEMATICS[sFileName]
                        if (schematicFile == null) // trying again, this time appending .schem
                            schematicFile = JsonDataHandler.REGISTERED_SCHEMATICS["$sFileName.schem"]
                        if (schematicFile != null) {
                            if (dungeon !is StaticInstancedDungeon) {
                                player.sendMessage(ComponentUtil.toComponent("<red>This dungeon is not a static dungeon"))
                                return@BiConsumer
                            }
                            dungeon.master.schematic = schematicFile
                            player.sendMessage(ComponentUtil.toComponent("<green>Set schematic to " + schematicFile.name))
                        } else {
                            player.sendMessage(
                                ComponentUtil.toComponent(
                                    "<red>No schematic named '%s' found".formatted(
                                        sFileName
                                    )
                                )
                            )
                        }
                        HandlerList.unregisterAll(l!!)
                    })
            })
            .appendNewline()
    }
    fun getMobConfigQuestion(room: InstancedDungeonRoom, mob: DungeonMob): Component {
        return Component
            .text("This message will expire after 25 seconds").color(NamedTextColor.RED).decorate(TextDecoration.BOLD)
            .appendNewline()
            .append(ComponentUtil.toComponent("<gold>Configuring mob '" + mob.identifier + "'"))
            .appendNewline()
            .append(Component.text("identifier").color(NamedTextColor.AQUA))
            .appendNewline()
            .append(Component.text("marked"))
            .appendNewline()
            .append(ComponentUtil.Questionnaire.create(
                ComponentUtil.toComponent("<blue>-> " + mob.isMarked.toString().uppercase(Locale.getDefault()))
                    .hoverEvent(
                        ComponentUtil.toComponent("<gray>Click to invert me")
                    ),
                true
            ) { player: Audience ->
                mob.isMarked = !mob.isMarked
                player.sendMessage(ComponentUtil.toComponent("<green>Inverted marked state of mob '" + mob.identifier + "'"))
            })
            .appendNewline()
    }
}