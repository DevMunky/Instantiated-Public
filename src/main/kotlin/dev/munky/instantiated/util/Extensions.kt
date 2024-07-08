@file:JvmName("Util")
package dev.munky.instantiated.util

import com.sk89q.worldedit.math.BlockVector3
import dev.jorel.commandapi.CommandAPIBukkit
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException
import dev.munky.instantiated.Instantiated
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.World
import java.util.*

val <T : Any> T?.asOptional : Optional<T> get() {
    return Optional.ofNullable(this)
}
fun String.clean() : String {
    return this.lowercase().replace(" ", "_")
}
fun Location.toImmutable() : ImmutableLocation {
    return ImmutableLocation.of(this)
}
fun BlockVector3.toImmutableLocation(world : World) : ImmutableLocation {
    return ImmutableLocation(world,this.x.toDouble(),this.y.toDouble(),this.z.toDouble())
}
fun BlockVector3.toImmutableVector() : ImmutableVector {
    return ImmutableVector(this.x.toDouble(),this.y.toDouble(),this.z.toDouble())
}
fun Location.toBlockVector() : BlockVector3 {
    return BlockVector3.at(this.x,this.y,this.z)
}
val String.asMini : Component get() {
    return ComponentUtil.toComponent(this)
}
fun Component.commandApiFail() : WrapperCommandSyntaxException {
    return CommandAPIBukkit.failWithAdventureComponent(this)
}

fun Throwable.stackMessage() : String{
    var message = "\n -> at ${this.message}"
    if (Instantiated.instance().debug()) {
        // 5 is optimal, gets method that threw, skipping all the methods to initialize the stack trace
        val frame = this.stackTrace[5]
        message += " | ${frame.className.split(".").last()}#${frame.methodName}@${frame.lineNumber}"
    }
    if (this.cause.asOptional.isPresent) message += this.cause?.stackMessage()
    return message
}