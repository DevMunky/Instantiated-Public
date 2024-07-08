package dev.munky.instantiated.dungeon.mob

import com.google.gson.JsonObject
import dev.munky.instantiated.data.json.asJsonObject
import dev.munky.instantiated.dungeon.IdentifiableKey
import dev.munky.instantiated.util.ImmutableLocation
import dev.munky.instantiated.util.emptyOptional
import org.bukkit.entity.LivingEntity
import java.util.*
import kotlin.jvm.optionals.getOrNull

class BossDungeonMob(
    override val identifier: IdentifiableKey,
    override val spawnLocation: ImmutableLocation,
    override var isMarked: Boolean,
    override val custom: MutableMap<String, String>,
    override var livingEntity: Optional<LivingEntity>,
) : DungeonMob() {

    constructor(
        identifier: IdentifiableKey,
        spawnLocation: ImmutableLocation,
        marked: Boolean,
        custom: MutableMap<String, String>
    ) : this (identifier,spawnLocation, marked,custom,emptyOptional())
    constructor(
        identifier: IdentifiableKey,
        spawnLocation: ImmutableLocation,
        marked: Boolean,
    ) : this (identifier,spawnLocation, marked, mutableMapOf(),emptyOptional())

    override val asJsonObject: JsonObject
        get() {
            val json = JsonObject()
            json.addProperty("identifier",identifier.id)
            json.add("spawn-location",spawnLocation.toLocation().serialize().asJsonObject)
            json.addProperty("marked",isMarked)
            custom["type"] = "boss"
            json.add("custom",custom.asJsonObject)
            return json
        }
    override fun onDeath(killer: LivingEntity, damage: Double): Boolean {
        return true
    }
    override fun toString(): String {
        return "boss.$identifier#${livingEntity.getOrNull()?.javaClass?.simpleName ?: "Unconfigured"}"
    }
    override fun clone(): BossDungeonMob {
        return BossDungeonMob(identifier, spawnLocation, isMarked, custom, livingEntity)
    }
}