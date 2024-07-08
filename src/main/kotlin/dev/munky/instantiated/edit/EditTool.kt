package dev.munky.instantiated.edit

import dev.munky.instantiated.Instantiated
import dev.munky.instantiated.dungeon.IdentifiableKey
import dev.munky.instantiated.dungeon.IdentifiableType
import dev.munky.instantiated.dungeon.InstancedDungeon
import dev.munky.instantiated.dungeon.InstancedDungeonRoom
import dev.munky.instantiated.dungeon.mob.DungeonMob
import dev.munky.instantiated.dungeon.mob.SimpleDungeonMob
import dev.munky.instantiated.event.ListenerFactory
import dev.munky.instantiated.oldDungeon.DungeonManager
import dev.munky.instantiated.util.*
import io.papermc.paper.event.player.AsyncChatEvent
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.HandlerList
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

data class EditToolInteraction(
    val event: PlayerInteractEvent,
    val instancedDungeon: InstancedDungeon,
    val instancedRoom: InstancedDungeonRoom,
    val interactionPoint: Location?
)
sealed interface EditTool{
    companion object{
        @JvmStatic
        val tools = EditTool::class.sealedSubclasses
            .map { it.objectInstance!! }
            .associateBy { it::class.simpleName!!.lowercase() }

        /**
         * @return true if an edit tool was used
         */
        @JvmStatic
        fun execute(click: EditToolInteraction) : Boolean{
            val item = click.event.item ?: return false
            val editToolString = click.event.item!!.itemMeta.persistentDataContainer.get(
                DungeonManager.NSK_EDIT_TOOL,
                PersistentDataType.STRING
            ) ?: return false
            val tool = tools[editToolString] ?: return false
            if (click.event.action == Action.RIGHT_CLICK_AIR || click.event.action == Action.RIGHT_CLICK_BLOCK) {
                tool.onRightClick(click)
            } else if (click.event.action == Action.LEFT_CLICK_AIR || click.event.action == Action.LEFT_CLICK_BLOCK) {
                tool.onLeftClick(click)
            }
            return true
        }
    }
    fun condition(click: EditToolInteraction) : Boolean
    fun onRightClick(click: EditToolInteraction)
    fun onLeftClick(click: EditToolInteraction)
    val item: ItemStack
    data object Vertex : EditTool {
        override fun onRightClick(click: EditToolInteraction) = onClick(click)
        override fun onLeftClick(click: EditToolInteraction) = onClick(click)
        override val item: ItemStack
            get() {
            val item = ItemStack(Material.STICK)
            item.addUnsafeEnchantment(Enchantment.CHANNELING,5)
            item.editMeta {
                it.displayName("<green>Dungeon Room edit tool".asMini)
                it.lore(mutableListOf(
                    "<gray>Click either right or left".asMini,
                    "<gray>To move a corner of the closest room".asMini,
                    "<gray>If you are not looking at a block,".asMini,
                    "<gray>your current position is used".asMini
                ))
                it.persistentDataContainer.set(
                    DungeonManager.NSK_EDIT_TOOL,
                    PersistentDataType.STRING,
                    "vertex"
                )
            }
            return item
        }
        override fun condition(click: EditToolInteraction): Boolean = true // no condition
        private fun onClick(click: EditToolInteraction){
            val interaction = click.interactionPoint ?: click.event.player.location;
            val interactionVector = interaction.toBlockVector()
            val newRegion = getCuboidFromNewCorner(click.instancedRoom.region, interactionVector)
            click.instancedRoom.region = newRegion.clone()
            newRegion.shift(click.instancedRoom.locationInWorld.toBlockVector3().multiply(-1))
            click.instancedRoom.master.region = newRegion.clone() // update the master 'template'
            click.event.player.sendMessage("<green>Added vertex at ${interaction.toBlockVector()}".asMini)
            Instantiated.logger().debug(
                "Added vertex at " + interaction.toBlockVector() +
                        " from room '" + click.instancedRoom.identifier +
                        "' in '" + click.instancedDungeon.identifier + "'"
            )
        }
    }
    data object Door : EditTool {
        override fun condition(click: EditToolInteraction): Boolean = true
        override fun onRightClick(click: EditToolInteraction) {
            TODO("Not yet implemented")
        }
        override fun onLeftClick(click: EditToolInteraction) {
            TODO("Not yet implemented")
        }
        override val item: ItemStack
            get() {
            val doorItem = ItemStack(Material.STICK)
            doorItem.addUnsafeEnchantment(Enchantment.MENDING, 69)
            doorItem.editMeta {
                it.displayName(ComponentUtil.toComponent("<rainbow>Dungeon door creation tool"))
                it.lore(mutableListOf(
                    "<gray>Right click to move closest door corner".asMini,
                    "<gray>Left click to remove clicked door".asMini
                ))
                it.persistentDataContainer.set(
                    DungeonManager.NSK_EDIT_TOOL,
                    PersistentDataType.STRING,
                    "door"
                )
            }
            return doorItem
        }
    }
    data object Config : EditTool{
        override fun condition(click: EditToolInteraction): Boolean = true
        override fun onRightClick(click: EditToolInteraction) = onClick(click)
        override fun onLeftClick(click: EditToolInteraction) = onClick(click)
        private fun onClick(click: EditToolInteraction){
            val interaction = click.interactionPoint ?: click.event.player.location
            val door = click.instancedRoom.getDoorAtBlockLocation(interaction)
            val mob = click.instancedRoom.master.mobs.values.filter { it.spawnLocation.toLocation().distance(interaction) < 2 }.let {
                if (it.isEmpty()) emptyOptional()
                else it.first().asOptional
            }
            // edit entities by standing close to them
            val message =
                if (door.isPresent) {
                    ChatQuestions.getDoorConfigQuestion(door.get())
                } else if (mob.isPresent) {
                    ChatQuestions.getMobConfigQuestion(click.instancedRoom,mob.get())
                }else if (click.event.player.isSneaking) {
                    ChatQuestions.getDungeonConfigQuestion(click.instancedDungeon)
                } else {
                    ChatQuestions.getRoomConfigQuestion(click.instancedRoom)
                }
            click.event.player.sendMessage(message)
        }
        override val item: ItemStack
            get() {
            val configItem = ItemStack(Material.EMERALD)
            configItem.editMeta {
                it.displayName(ComponentUtil.toComponent("<blue>Configuration Editor"))
                it.lore(mutableListOf(
                    "<gray>Click on things to change their configs,".asMini,
                    "<gray>Or click nothing to configure the current room".asMini
                ))
                it.persistentDataContainer.set(
                    DungeonManager.NSK_EDIT_TOOL,
                    PersistentDataType.STRING,
                    "config"
                )
            }
            return configItem
        }
    }
    data object Entity : EditTool {
        override fun condition(click: EditToolInteraction): Boolean = true
        override fun onRightClick(click: EditToolInteraction) {
            val interaction = click.event.player.location
            click.event.player.sendMessage("<green>What should the id be for this mob? </green><red><italic>(send 'cancel' or 'n' to cancel)".asMini)
            val start = System.currentTimeMillis()
            ListenerFactory.registerEvent(AsyncChatEvent::class.java) { e, l ->
                if (e.player !== click.event.player) return@registerEvent
                val response = ComponentUtil.toString(e.message())
                if (System.currentTimeMillis() - start >= 15000/* 15 seconds */) return@registerEvent
                if (response == "cancel" || response == "n") {
                    e.isCancelled = true
                    e.player.sendMessage(ComponentUtil.toComponent("<red>Cancelled mob creation"))
                    return@registerEvent
                }
                e.isCancelled = true
                interaction.subtract(click.instancedRoom.locationInWorld.toLocation())
                val newMob: DungeonMob = SimpleDungeonMob(
                    IdentifiableKey(IdentifiableType.MOB, response),
                    ImmutableLocation.of(interaction),
                    false,
                    HashMap()
                )
                click.instancedRoom.master.mobs[newMob.identifier] = newMob
                HandlerList.unregisterAll(l)
            }
        }

        override fun onLeftClick(click: EditToolInteraction) {
            val interaction: Location = click.event.player.location
            val closest: DungeonMob = click.instancedRoom.master.mobs.values.filter { it.spawnLocation.toLocation().distance(interaction) < 2 }.let {
                if (it.isEmpty()){
                    click.event.player.sendMessage(ComponentUtil.toComponent("<red>No mob here"))
                    return
                }
                it.first()
            }
            click.instancedRoom.master.mobs.remove(closest.identifier)
            click.event.player.sendMessage("<green>Removed entity '${closest.identifier}' from room '${click.instancedRoom.identifier}'".asMini)
            Instantiated.logger().debug("Removed entity '${closest.identifier}' from room '${click.instancedRoom.identifier}'")
        }
        override val item: ItemStack
            get() {
            val entityItem = ItemStack(Material.STICK)
            entityItem.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 69)
            entityItem.editMeta {
                it.displayName(ComponentUtil.toComponent("<yellow>Dungeon room mob tool"))
                it.lore(mutableListOf(
                    "<gray>Right click to add mob at your location".asMini,
                    "<gray>Left click to delete mobs at clicked block".asMini
                ))
                it.persistentDataContainer.set(
                    DungeonManager.NSK_EDIT_TOOL,
                    PersistentDataType.STRING,
                    "entity"
                )
            }
            return entityItem
        }
    }
}