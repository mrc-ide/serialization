package org.mrc.ide.serialization

import com.github.salomonbrys.kotson.jsonSerializer
import com.github.salomonbrys.kotson.registerTypeAdapter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonPrimitive
import org.mrc.ide.serialization.models.Result
import org.mrc.ide.serialization.models.ResultStatus
import java.time.Instant
import java.time.LocalDate

open class DefaultSerializer : Serializer {
    private val toDateStringSerializer = jsonSerializer<Any> {
        JsonPrimitive(it.src.toString())
    }

    override val serializeNullsTo: String = "<NA>"

    companion object {
        val instance: Serializer = DefaultSerializer()
    }

    override val gson: Gson = GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .setFieldNamingStrategy { convertFieldName(it.name) }
            .serializeNulls()
            .registerTypeAdapter<Instant>(toDateStringSerializer)
            .registerTypeAdapter<LocalDate>(toDateStringSerializer)
            .create()

    override fun toResult(data: Any?): String = toJson(Result(ResultStatus.SUCCESS, data, emptyList()))
    override fun toJson(result: Any?): String = gson.toJson(result)
    override fun <T> fromJson(json: String, klass: Class<T>): T = gson.fromJson(json, klass)

    override fun convertFieldName(name: String): String {
        val builder = StringBuilder()
        for (char in name) {
            if (char.isUpperCase()) {
                builder.append("_" + char.toLowerCase())
            } else {
                builder.append(char)
            }
        }
        return builder.toString().trim('_')
    }

    override fun serializeEnum(value: Any): String {
        val text = value.toString()
        return text.toLowerCase().replace('_', '-')
    }

    override fun serializeValueForCSV(value: Any?) = when (value) {
        null -> serializeNullsTo
        is Enum<*> -> serializeEnum(value)
        else -> value.toString()
    }
}