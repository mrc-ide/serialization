package org.mrc.ide.serialization

import kotlin.reflect.KFunction
import kotlin.reflect.KType

class FlexibleDataTableDeserializer<out T>(
        headers: List<HeaderDefinition>,
        constructor: KFunction<T>,
        private val flexibleType: KType,
        allColumnsRequired: Boolean
) : DataTableDeserializer<T>(headers, constructor, allColumnsRequired)
{
    override val extraHeadersAllowed = true

    override fun prepareValuesForConstructor(values: List<Any?>, actualHeaders: List<HeaderDefinition>): List<Any?>
    {
        // So currently we have values like this: 1,2,3,4
        // But our target data type expects 1 and 2 as "fixed" columns, and then a map that has 3 and 4 as values.
        val fixed = values.take(headerDefinitions.size)
        val extraValues = values.drop(headerDefinitions.size)
        val extraHeaders = actualHeaders.drop(headerDefinitions.size)
        val flexibleAsMap = extraHeaders.zip(extraValues).map { (header, value) -> header.name to value }.toMap()
        return fixed + flexibleAsMap
    }

    override fun getActualHeaderDefinitions(actualHeaders: List<String>): List<HeaderDefinition>
    {
        val extraHeaders = actualHeaders.drop(headerDefinitions.size)
        return headerDefinitions + extraHeaders.map { HeaderDefinition(it, flexibleType) }
    }
}