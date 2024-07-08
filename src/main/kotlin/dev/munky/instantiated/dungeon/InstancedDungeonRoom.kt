package dev.munky.instantiated.dungeon

import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import dev.munky.instantiated.Instantiated
import dev.munky.instantiated.dungeon.mob.DungeonMob
import dev.munky.instantiated.event.ListenerFactory
import dev.munky.instantiated.oldDungeon.DungeonManager
import dev.munky.instantiated.util.ComponentUtil
import dev.munky.instantiated.util.ImmutableLocation
import dev.munky.instantiated.util.ImmutableVector
import io.papermc.paper.util.Tick
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Item
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerAttemptPickupItemEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.persistence.PersistentDataType
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.*

interface InstancedDungeonRoom : Identifiable {
    val origin : BlockVector3
    val doors : MutableMap<ImmutableVector, InstancedDungeonDoor>
    val locationInWorld : ImmutableLocation
    var region : CuboidRegion
    var areMobsSpawned : Boolean
    val parent : InstancedDungeon
    val master : DungeonRoomFormat
    fun spawnMobs()
    fun spawnDoors()
    fun getDoorAtBlockLocation(location: Location) : Optional<InstancedDungeonDoor>
    fun registerDungeonMobDeath(dungeonMob: DungeonMob): Boolean
    /**
     * Drops a key at the given location, that is not really a real key and cannot be picked up.
     * @param location real location, not relative
     * @return the spawned item
     */
    fun dropKey(location: Location) {
        val keyItem = ItemStack(master.keyMaterial)
        val meta = keyItem.itemMeta
        meta.displayName(ComponentUtil.toComponent("<black><obfuscated>YouShouldNotSeeThis"))
        keyItem.setItemMeta(meta)
        val keyEntity = location.world.spawn(
            location,
            Item::class.java
        ) { e: Item ->
            e.isCustomNameVisible = true
            e.customName(ComponentUtil.toComponent("<rainbow>Door key!"))
            e.setCanMobPickup(false)
            e.isUnlimitedLifetime = true
            e.setMetadata(DungeonManager.METADATA_PERSISTENT_ENTITY, FixedMetadataValue(Instantiated.instance(), true))
            e.setMetadata(DungeonManager.METADATA_KEY_ENTITY, FixedMetadataValue(Instantiated.instance(), true))
            e.persistentDataContainer
                .set(DungeonManager.NSK_OWNED_ENTITY, PersistentDataType.LONG, Instantiated.INIT_TIME)
            e.itemStack = keyItem
            if (Instantiated.instance().mainConfig.DROPPED_KEYS_USE_BUKKIT_GLOW) {
                e.isGlowing = true
                val board = Bukkit.getScoreboardManager().mainScoreboard
                var team = board.getTeam("instantiated-dropped-key-team")
                if (team == null) {
                    team = board.registerNewTeam("instantiated-dropped-key-team")
                }
                // defaults to BLUE
                team.color(Instantiated.instance().mainConfig.DROPPED_KEYS_GLOW_COLOR)
                team.addEntity(e)
            }
        }
        Bukkit.getScheduler().runTaskLater(Instantiated.instance(), Runnable {
            if (!keyEntity.isInWorld || keyEntity.isDead || !keyEntity.isTicking) return@Runnable
            keyEntity.remove()
            this.parent.doorKeys++
            val keyPickupTitle = Title.title(
                ComponentUtil.toComponent("<gradient:blue:green:blue>Key picked up!"),
                ComponentUtil.toComponent("<gray><italic>Current keys: " + this.parent.doorKeys),
                Title.Times.times(
                    Duration.of(500, ChronoUnit.MILLIS),
                    Duration.of(2, ChronoUnit.SECONDS),
                    Duration.of(500, ChronoUnit.MILLIS)
                )
            )
            for (player in this.parent.onlinePlayers) {
                player.showTitle(keyPickupTitle)
            }
            Instantiated.logger().debug(
                "Key automatically picked up after 15 seconds! Current keys = ${this.parent.doorKeys}"
            )
        }, Tick.tick().fromDuration(Duration.of(15, ChronoUnit.SECONDS)).toLong())
        val keyDropTitle = Title.title(
            ComponentUtil.toComponent("<gradient:red:blue:red>Key dropped!"),
            ComponentUtil.toComponent("<gray><italic>Current keys: ${this.parent.doorKeys}"),
            Title.Times.times(
                Duration.of(500, ChronoUnit.MILLIS),
                Duration.of(2, ChronoUnit.SECONDS),
                Duration.of(500, ChronoUnit.MILLIS)
            )
        )
        for (player in this.parent.onlinePlayers) {
            player.showTitle(keyDropTitle)
        }
        ListenerFactory.registerEvent(
            PlayerAttemptPickupItemEvent::class.java
        ) { e: PlayerAttemptPickupItemEvent, l: Listener? ->
            if (e.item.itemStack != keyItem || !e.item
                    .hasMetadata(DungeonManager.METADATA_KEY_ENTITY)
            ) return@registerEvent
            e.isCancelled = true
            e.flyAtPlayer = true
            e.item.remove()
            this.parent.doorKeys++
            val keyPickupTitle = Title.title(
                ComponentUtil.toComponent("<gradient:blue:green:blue>Key picked up!"),
                ComponentUtil.toComponent(
                    "<gray><italic>Current keys: " + this.parent.doorKeys
                ),
                Title.Times.times(
                    Duration.of(500, ChronoUnit.MILLIS),
                    Duration.of(2, ChronoUnit.SECONDS),
                    Duration.of(500, ChronoUnit.MILLIS)
                )
            )
            for (player in this.parent.onlinePlayers) {
                player.showTitle(keyPickupTitle)
            }
            Instantiated.logger().debug(
                "Player '${e.player.name}' picked up a key! Current keys = ${this.parent.doorKeys}"
            )
            HandlerList.unregisterAll(l!!)
        }
        Instantiated.logger().debug("Spawned key at $location")
    }
}