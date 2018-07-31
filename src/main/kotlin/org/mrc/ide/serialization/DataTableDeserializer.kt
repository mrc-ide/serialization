package org.mrc.ide.serialization

import com.opencsv.CSVReader
import org.mrc.ide.serialization.models.AllColumnsRequired
import org.mrc.ide.serialization.models.ErrorInfo
import org.mrc.ide.serialization.models.FlexibleColumns
import org.mrc.ide.serialization.validation.ValidationException
import java.io.Reader
import java.io.StringReader
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor

class HeaderDefinition(val name: String, val type: KType)

open class DataTableDeserializer<out T>(
        protected val headerDefinitions: List<HeaderDefinition>,
        private val constructor: KFunction<T>,
        private val allColumnsRequired: Boolean = false,
        private val deserializer: Deserializer
)
{
    private val headerCount = headerDefinitions.size
    protected open val extraHeadersAllowed = false

    private fun shouldTrim(char: Char): Boolean
            = char.isWhitespace() || char == '\''

    fun deserialize(stream: Reader): Sequence<T>
    {
        val reader = CSVReader(stream)
        val rows = generateSequence { reader.readNext() }.stripEmptyRows()
        val (headerRow, content) = rows.headAndTail()

        if (headerRow == null)
        {
            throw ValidationException(listOf(ErrorInfo("csv-empty", "CSV was empty - no rows or headers were found")))
        }

        val actualHeaderNames = headerRow.map({ it.trim(this::shouldTrim) }).toList()
        checkHeaders(actualHeaderNames)
        val actualHeaders = getActualHeaderDefinitions(actualHeaderNames)

        return content.withIndex().map { (i, row) ->
            deserializeRow(row.toList(), actualHeaders, i)
        }
    }

    private fun deserializeRow(
            row: List<String>,
            actualHeaders: List<HeaderDefinition>,
            rowIndex: Int
    ): T
    {
        val problems = mutableListOf<ErrorInfo>()
        if (row.size != actualHeaders.size)
        {
            val oneIndexedRow = rowIndex + 1
            problems.add(ErrorInfo("csv-wrong-row-length:$oneIndexedRow",
                    "Row $oneIndexedRow has a different number of columns from the header row"))
        }
        val values = row.zip(actualHeaders).map { (raw, header) ->
            deserialize(raw, header.type, rowIndex, header.name, problems)
        }
        if (problems.any())
        {
            throw ValidationException(problems)
        }
        return constructor.call(*prepareValuesForConstructor(values, actualHeaders).toTypedArray())
    }

    protected open fun prepareValuesForConstructor(values: List<Any?>, actualHeaders: List<HeaderDefinition>): List<Any?>
    {
        return values
    }

    private fun deserialize(raw: String, targetType: KType,
                            row: Int, column: String,
                            problems: MutableList<ErrorInfo>): Any?
    {
        val trimmed = raw.trim(this::shouldTrim)
        checkValueIsPresentIfRequired(trimmed, targetType, row, column, problems)

        return try
        {
            val value = deserializer.deserialize(trimmed, targetType)
            value
        }
        catch (e: Exception)
        {
            if (e is UnsupportedOperationException)
            {
                throw e
            }

            val oneIndexedRow = row + 1

            problems.add(ErrorInfo(
                    "csv-bad-data-type:$oneIndexedRow:$column",
                    "Unable to parse '$trimmed' as ${targetType.toString().replace("kotlin.", "")} (Row $oneIndexedRow, column $column)"
            ))

            null
        }
    }

    private fun checkValueIsPresentIfRequired(trimmed: String, targetType: KType,
                                              row: Int, column: String,
                                              problems: MutableList<ErrorInfo>)
    {
        if (allColumnsRequired && trimmed.isEmpty())
        {
            val oneIndexedRow = row + 1
            problems.add(ErrorInfo(
                    "csv-missing-data:$oneIndexedRow:$column",
                    "Unable to parse '$trimmed' as ${targetType.toString().replace("kotlin.", "")} (Row $oneIndexedRow, column $column)"
            ))
        }
    }

    private fun checkHeaders(actualHeaders: List<String>): List<HeaderDefinition>
    {
        val problems = mutableListOf<ErrorInfo>()
        var index = 0
        val maxHeaderCount = maxOf(headerCount, actualHeaders.size)
        while (index < maxHeaderCount)
        {
            val expected = headerDefinitions.getOrNull(index)
            val actual = actualHeaders.getOrNull(index)?.trim()

            if (actual == null)
            {
                // at most one of actual and expected can be null, so we can infer here that expected is not null
                problems.add(ErrorInfo("csv-missing-header",
                        "Not enough column headers were provided. Expected a '${expected!!.name}' header."))
            }
            else if (expected == null)
            {
                if (!extraHeadersAllowed)
                {
                    problems.add(ErrorInfo("csv-unexpected-header",
                            "Too many column headers were provided. Unexpected '$actual' header."))
                }
            }
            else if (!actual.equals(expected.name, ignoreCase = true))
            {
                problems.add(ErrorInfo("csv-unexpected-header",
                        "Expected column header '${expected.name}'; found '$actual' instead (column $index)"))
            }

            index += 1
        }

        if (problems.any())
        {
            throw ValidationException(problems)
        }
        return headerDefinitions
    }

    protected open fun getActualHeaderDefinitions(actualHeaders: List<String>): List<HeaderDefinition>
    {
        return headerDefinitions
    }

    private fun Sequence<Array<String>>.stripEmptyRows(): Sequence<Array<String>>
    {
        return this
                // Skip leading empty rows
                .dropWhile { row -> row.all { it.isBlank() } }
                // Skip trailing empty rows
                .takeWhile { row -> row.any { !it.isBlank() } }
    }

    companion object
    {
        fun <T : Any> deserialize(
                body: Reader,
                type: KClass<T>,
                serializer: Serializer,
                deserializer: Deserializer
        ): Sequence<T>
        {
            return getDeserializer(type, serializer, deserializer).deserialize(body)
        }

        fun <T : Any> deserialize(
                body: String,
                type: KClass<T>,
                serializer: Serializer,
                deserializer: Deserializer
        ): Sequence<T>
        {
            return deserialize(StringReader(body), type, serializer, deserializer)
        }

        private fun <T : Any> getDeserializer(type: KClass<T>,
                                              serializer: Serializer,
                                              deserializer: Deserializer): DataTableDeserializer<T>
        {
            val constructor = type.primaryConstructor
                    ?: throw Exception("Cannot deserialize to type ${type.simpleName} - it has no primary constructor")
            val headers = constructor.parameters
                    .map { HeaderDefinition(serializer.convertFieldName(it.name!!), it.type) }

            val allColumnsRequired = type.findAnnotation<AllColumnsRequired>() != null
            return if (type.findAnnotation<FlexibleColumns>() != null)
            {
                val flexibleType = getFlexibleColumnType(constructor, type)
                FlexibleDataTableDeserializer(headers.dropLast(1), constructor, flexibleType, allColumnsRequired,
                        deserializer)
            }
            else
            {
                DataTableDeserializer(headers, constructor, allColumnsRequired, deserializer)
            }
        }

        private fun <T : Any> getFlexibleColumnType(constructor: KFunction<T>, type: KClass<T>): KType
        {
            // If the last argument is a map, get the type of the values the map stores
            return try
            {
                constructor.parameters.last().type.arguments.last().type!!
            }
            catch (e: Exception)
            {
                throw Exception("Type '$type' was marked with the @FlexibleColumns annotation, but something " +
                        "went wrong finding out the type of flexible data. The last parameter in the constructor " +
                        "should be of type Map<String, *>, where * can be whatever you like.", e)
            }
        }
    }
}