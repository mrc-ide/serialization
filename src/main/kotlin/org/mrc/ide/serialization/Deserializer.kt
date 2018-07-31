package org.mrc.ide.serialization

import java.lang.UnsupportedOperationException
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.withNullability

interface Deserializer {
    fun <T: Enum<T>> parseEnum(name: String, clazz: Class<T>): T
    fun deserialize(raw: String, targetType: KType): Any?
}