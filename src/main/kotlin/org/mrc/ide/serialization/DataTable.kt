package org.mrc.ide.serialization

import com.opencsv.CSVWriter
import org.mrc.ide.serialization.models.ContentTypes
import java.io.OutputStream
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor

open class DataTable<T : Any>(override val data: Sequence<T>, val type: KClass<T>) : StreamSerializable<T>
{
    class DataTableHeader<T>(name: String, val property: KProperty1<T, *>, serializer: Serializer)
    {
        val name = serializer.convertFieldName(name)
        override fun toString() = name
    }

    override val contentType = ContentTypes.csv

    protected val constructor: KFunction<T> = type.primaryConstructor
            ?: throw Exception("Data type must have a primary constructor.")

    protected val properties = type.declaredMemberProperties

    override fun serialize(stream: OutputStream, serializer: Serializer)
    {
        val headers = getHeaders(serializer)
        stream.writer().let { writer ->
            CSVWriter(writer).let { csv ->

                val headerArray = prepareHeadersForCSV(headers)
                csv.writeNext(headerArray, false)

                for (line in data)
                {
                    val asArray = allValuesAsArray(headers, line, serializer)
                    csv.writeNext(asArray, false)
                }
            }
            // We want to flush this writer, but we don't want to close the underlying stream, as there
            // be more to write to it
            writer.flush()
        }
    }

    open protected fun prepareHeadersForCSV(headers: Iterable<DataTableHeader<T>>): Array<String>
    {
        return headers.map { it.name }.toTypedArray()
    }

    open protected fun allValuesAsArray(headers: Iterable<DataTableHeader<T>>, line: T, serializer: Serializer): Array<String>
    {
        return headers
                .map { it.property.get(line) }
                .map { serializer.serializeValueForCSV(it) }
                .toTypedArray()
    }

    open protected fun getHeaders(serializer: Serializer): Iterable<DataTableHeader<T>>
    {
        return constructor.parameters
                .mapNotNull { it.name }
                .map { name -> DataTableHeader(name, properties.single { name == it.name }, serializer) }
    }

    companion object
    {
        // Simple helper to get around JVM type erasure
        inline fun <reified R : Any> new(data: Sequence<R>) = DataTable(data, R::class)
    }
}