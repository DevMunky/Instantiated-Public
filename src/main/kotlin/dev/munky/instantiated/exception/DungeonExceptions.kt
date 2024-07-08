package dev.munky.instantiated.exception

import dev.munky.instantiated.dungeon.IdentifiableKey
import dev.munky.instantiated.dungeon.sstatic.StaticInstancedDungeon
import dev.munky.instantiated.util.stackMessage
import java.io.File
import java.util.function.Function
import kotlin.reflect.KClass

/**
 * Convenience class for grabbing different kinds of exceptions
 */
class DungeonExceptions {
    companion object{
        @JvmStatic
        val Instantiation = ExceptionFactory<IdentifiableKey, InstantiationException>(InstantiationException::class) { id ->
            "could not instance '$id'"
        }
        @JvmStatic
        val DataSyntax = ExceptionFactory<String,DataSyntaxException>(DataSyntaxException::class) { string ->
            string
        }
        @JvmStatic
        val PhysicalRemoval = ExceptionFactory<StaticInstancedDungeon,PhysicalRemovalException>(PhysicalRemovalException::class){ dungeon->
            "could not remove physical component of dungeon '${dungeon.identifier}'"
        }
        @JvmStatic
        val DungeonNotFound = ExceptionFactory<String,DungeonNotFoundException>(DungeonNotFoundException::class){identifier->
            "$identifier not found"
        }
        @JvmStatic
        val DungeonDataFileNotFound = ExceptionFactory<File,DungeonDataFileNotFoundException>(DungeonDataFileNotFoundException::class){
            "dungeon data file '${it.name}' not found"
        }
        @JvmStatic
        val Generic = ExceptionFactory<String,GenericException>(GenericException::class){
            it
        }
    }
}
class ExceptionFactory<T : Any,E : DungeonException> internal constructor(
    private val exceptionClass : KClass<E>,
    private val msgFunction: Function<T,String>
) {
    companion object{
        val exceptions : MutableMap<DungeonException,Long> = HashMap()
    }
    fun consume(datum : T, cause: Throwable?) : E {
        val constructor = exceptionClass.java.getConstructor(String::class.java,Throwable::class.java)
        val exception : E = constructor.newInstance(msgFunction.apply(datum),cause)
        exceptions[exception] = System.currentTimeMillis()
        return exception
    }
    fun consume(datum: T) : E{
        return consume(datum,null)
    }
}
abstract class DungeonException(message:String?, cause:Throwable?) : RuntimeException(
    message ?: cause?.getLastMessage(),
    cause
){
    // for java
    fun getStackMessage() : String {
        return this.stackMessage()
    }
}
class InstantiationException(msg:String, cause:Throwable?) : DungeonException(msg,cause)
class GenericException(msg:String, cause:Throwable?) : DungeonException(msg,cause)
class DataSyntaxException(msg:String,cause:Throwable?) : DungeonException(msg,cause)
class PhysicalRemovalException(msg:String,cause:Throwable?) : DungeonException(msg,cause)
class DungeonNotFoundException(msg:String,cause:Throwable?) : DungeonException(msg,cause)
class DungeonDataFileNotFoundException(msg:String,cause:Throwable?) : DungeonException(msg,cause)

fun Throwable.getLastMessage() : String {
    return if (this.cause!=null){
        cause!!.getLastMessage()
    }else if (this.message!=null){
        this.message!!
    }else{
        "no message"
    }
}