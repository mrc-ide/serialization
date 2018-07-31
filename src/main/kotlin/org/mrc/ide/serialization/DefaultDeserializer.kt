package org.mrc.ide.serialization

import java.lang.UnsupportedOperationException
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.withNullability

class DefaultDeserializer: Deserializer
{
    companion object {
        val instance: Deserializer = DefaultDeserializer()
        const val noValue = "NA"
    }

    override fun <T : Enum<T>> parseEnum(name: String, clazz: Class<T>): T
    {
        return clazz.enumConstants
                .firstOrNull { name.replace('-', '_').equals(it.name, ignoreCase = true) }
                ?: throw UnknownEnumValue(name, clazz.simpleName ?: "[unknown]")
    }

    override fun deserialize(raw: String, targetType: KType): Any? = if (targetType.isMarkedNullable)
    {
        if (raw == noValue)
        {
            null
        }
        else
        {
            deserialize(raw, targetType.withNullability(false))
        }
    }
    else
    {
        when (targetType)
        {
            String::class.createType() -> raw
            Int::class.createType() -> raw.toInt()
            Float::class.createType() -> raw.toFloat()
            else -> throw UnsupportedOperationException("org.mrc.ide.serialization.DefaultDeserializer does not support target type $targetType")
        }
    }
}
