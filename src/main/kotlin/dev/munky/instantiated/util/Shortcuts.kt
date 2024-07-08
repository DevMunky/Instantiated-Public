package dev.munky.instantiated.util

import java.util.*

fun <T : Any> emptyOptional() : Optional<T>{
    return Optional.empty<T>()
}