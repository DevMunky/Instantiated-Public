package dev.munky.instantiated.dungeon.mob

import com.destroystokyo.paper.ParticleBuilder
import com.google.gson.JsonObject
import dev.munky.instantiated.data.json.JsonContext
import dev.munky.instantiated.data.json.asLocation
import dev.munky.instantiated.data.json.getField
import dev.munky.instantiated.dungeon.Identifiable
import dev.munky.instantiated.dungeon.IdentifiableType
import dev.munky.instantiated.util.ImmutableLocation
import dev.munky.instantiated.util.clean
import dev.munky.instantiated.util.toImmutable
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.LivingEntity
import java.util.*
import java.util.function.Function
import kotlin.math.cos
import kotlin.math.sin

abstract class DungeonMob : Identifiable {
    abstract val spawnLocation: ImmutableLocation
    abstract var livingEntity : Optional<LivingEntity>
    abstract val asJsonObject : JsonObject
    abstract var isMarked : Boolean
    abstract val custom : MutableMap<String,String>
    companion object{
        val MOB_TYPES : MutableMap<String, Function<in JsonObject,out DungeonMob>> = mutableMapOf()
        init{
            MOB_TYPES["simple"] = Function {
                val identifier = IdentifiableType.MOB.with(it.getField<String>("identifier", JsonContext.MOB).clean())
                val spawn = it.getField<JsonObject>("spawn-location", JsonContext.MOB).asLocation
                val marked = it.getField<Boolean>("marked", JsonContext.MOB)
                val custom = it.getField<Map<String,String>>("custom", JsonContext.MOB).toMutableMap()
                SimpleDungeonMob(identifier,spawn.toImmutable(),marked,custom)
            }
            MOB_TYPES["boss"] = Function {
                val identifier = IdentifiableType.MOB.with(it.getField<String>("identifier", JsonContext.MOB).clean())
                val spawn = it.getField<JsonObject>("spawn-location", JsonContext.MOB).asLocation
                val marked = it.getField<Boolean>("marked", JsonContext.MOB)
                val custom = it.getField<Map<String,String>>("custom", JsonContext.MOB).toMutableMap()
                BossDungeonMob(identifier,spawn.toImmutable(),marked,custom)
            }
        }
    }
    fun doDeath(killer: LivingEntity,damage: Double) : Boolean{
        if (livingEntity.isEmpty) return true // idk true or false for this?
        val con = onDeath(killer,damage)
        if (!con) return false
        deathEffect(livingEntity.get().location)
        return true
    }
    open fun deathEffect(location: Location){
        val particle = ParticleBuilder(Particle.TOTEM).count(10).allPlayers().extra(4.0)
        var d = 0
        while (d <= 90) {
            val particleLoc = Location(location.world, location.x, location.y, location.z)
            particleLoc.x = location.x + cos(d.toDouble()) * 3
            particleLoc.z = location.z + sin(d.toDouble()) * 3
            particle.location(particleLoc).spawn()
            d += 1
        }
    }
    /**
     * Return true if the death is valid.
     * Called before the actual death of the mob
     * @return should this death be cancelled?
     */
    abstract fun onDeath(killer: LivingEntity,damage: Double) : Boolean
    abstract override fun toString(): String
    abstract fun clone() : DungeonMob
}
