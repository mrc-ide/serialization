package org.mrc.ide.serialization

import com.google.gson.Gson

interface Serializer
{
    fun toResult(data: Any?): String
    fun toJson(result: Any?): String
    fun <T> fromJson(json: String, klass: Class<T>): T
    fun convertFieldName(name: String): String
    fun serializeEnum(value: Any): String
    fun serializeValueForCSV(value: Any?): String
    val gson: Gson
    val serializeNullsTo: String
}