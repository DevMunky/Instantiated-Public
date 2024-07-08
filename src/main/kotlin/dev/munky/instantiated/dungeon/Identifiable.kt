package dev.munky.instantiated.dungeon

interface Identifiable {
    val identifier : IdentifiableKey
}
data class IdentifiableKey(
    val type: IdentifiableType,
    val id: String
){
    override fun toString() : String{
        return "$type:$id"
    }
}
enum class IdentifiableType{
    DUNGEON,
    ROOM,
    DOOR,
    MOB;
    override fun toString() : String{
        return name.lowercase()
    }
    fun with(id:String) : IdentifiableKey{
        return IdentifiableKey(this,id)
    }
}