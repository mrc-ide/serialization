package org.mrc.ide.serialization

import org.mrc.ide.serialization.models.FlexibleProperty
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation

class FlexibleDataTable<T : Any>(data: Sequence<T>,
                                 private val flexibleHeaders: Iterable<Any>,
                                 type: KClass<T>)
    : DataTable<T>(data, type)
{

    private val flexibleParameter: KParameter = constructor.parameters.firstOrNull {
        it.findAnnotation<FlexibleProperty>() != null
    }
            ?: throw Exception("No parameter marked as flexible." +
                    " Use the DataTable class to serialise data with fixed headers.")

    private val flexibleProperty = properties.firstOrNull { it.name == flexibleParameter.name }
            ?: throw Exception("No property marked as flexible." +
                    " Use the DataTable class to serialise data with fixed headers.")

    init
    {
        flexibleParameter.type.arguments.lastOrNull() ?: throw Exception("Properties marked as flexible must be of " +
                "type Map<*, *>, where * can be whatever you like.")
    }

    override fun prepareHeadersForCSV(headers: Iterable<DataTableHeader<T>>): Array<String>
    {
        return headers.map { it.name }.toTypedArray()
                .plus(flexibleHeaders.map { it.toString() })
    }

    override fun allValuesAsArray(headers: Iterable<DataTableHeader<T>>, line: T, serializer: Serializer): Array<String>
    {
        val values = headers.map { it.property.get(line) }
                .plus(flexibleHeaders.map { getFlexibleValue(it, line) })

        return values
                .map { serializer.serializeValueForCSV(it) }
                .toTypedArray()
    }

    private fun getFlexibleValue(key: Any, line: T): Any?
    {
        val map = flexibleProperty.get(line) as Map<*, *>
        return map[key]
    }

    override fun getHeaders(serializer: Serializer): Iterable<DataTableHeader<T>>
    {
        return constructor.parameters
                .filter { it != flexibleParameter }
                .mapNotNull { it.name }
                .map { name -> DataTableHeader(name, properties.single { name == it.name }, serializer) }

    }

    companion object
    {
        // Simple helper to get around JVM type erasure
        inline fun <reified R : Any> new(data: Sequence<R>, flexibleHeaders: Iterable<Any>) = FlexibleDataTable(data, flexibleHeaders, R::class)
    }
}