package org.mrc.ide.serialization.tests

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.mrc.ide.serialization.DataTable
import org.mrc.ide.serialization.DataTableDeserializer
import org.mrc.ide.serialization.DefaultSerializer
import org.mrc.ide.serialization.models.FlexibleColumns
import org.mrc.ide.serialization.models.AllColumnsRequired
import org.mrc.ide.serialization.models.FlexibleProperty
import org.mrc.ide.serialization.validation.ValidationException

enum class TestEnum {  IN_PREPARATION,
    OPEN,
    FINISHED
}

class DataTableTests {

    data class ABC(val a: String, val b: String, val c: String)
    data class MixedTypes(val text: String?, val int: Int?, val dec: Float?)
    data class SomeRequiredColumns(val text: String?, val int: Int)
    data class WithEnums(val text: String, val enum: TestEnum)
    @FlexibleColumns
    data class Flexible(val a: Int, val b: String, val extra: Map<String, Int>)

    @FlexibleColumns
    data class FlexibleWithStrings(val a: Int, val b: String, val extra: Map<String, String>)

    @Test
    fun `headers are written in order of constructor`() {
        val table = DataTable.new<ABC>(emptySequence())
        assertThat(serialize(table)).isEqualTo("""a,b,c""")
    }

    @Test
    fun `data is written out line by line`() {
        val table = DataTable.new(sequenceOf(
                ABC("g", "h", "i"),
                ABC("x", "y", "z")
        ))
        assertThat(serialize(table)).isEqualTo("""a,b,c
g,h,i
x,y,z""")
    }

    @Test
    fun `special characters are escaped`() {
        val table = DataTable.new(sequenceOf(
                ABC("g", "h", "i"),
                ABC("x", "y", "z"),
                ABC("with, commas", """with "quotes" and no commas""", """both "quotes" and ,commas,""")
        ))
        assertThat(serialize(table)).isEqualTo("""a,b,c
g,h,i
x,y,z
"with, commas","with ""quotes"" and no commas","both ""quotes"" and ,commas,"""")
    }

    @Test
    fun `mixed types are written out`() {
        val table = DataTable.new(sequenceOf(
                MixedTypes("text", 123, 3.1415F)
        ))
        assertThat(serialize(table)).isEqualTo("""text,int,dec
text,123,3.1415""")
    }

    @Test
    fun `null is converted to NA`() {
        val table = DataTable.new(sequenceOf(
                MixedTypes(null, null, null)
        ))
        assertThat(serialize(table)).isEqualTo("""text,int,dec
<NA>,<NA>,<NA>""")
    }

    @Test
    fun `enum is converted to lowercase with hyphens`() {
        val table = DataTable.new(sequenceOf(
                WithEnums("free text", TestEnum.IN_PREPARATION)
        ))
        assertThat(serialize(table)).isEqualTo("""text,enum
free text,in-preparation""")
    }


    @Test
    fun `can deserialize CSV with single quote delimiter`() {
        val csv = """
            'text','int','dec'
            'joe',1,6.53
            'bob',2,2.0"""
        val rows = DataTableDeserializer.deserialize(csv, MixedTypes::class, DefaultSerializer.instance).toList()
        assertThat(rows).containsExactlyElementsOf(listOf(
                MixedTypes("joe", 1, 6.53F),
                MixedTypes("bob", 2, 2.0F)
        ))
    }

    @Test
    fun `can deserialize CSV with double quote delimiter`() {
        val csv = """
            "text","int","dec"
            "joe",1,6.53
            "bob",2,2.0"""
        val rows = DataTableDeserializer.deserialize(csv, MixedTypes::class, DefaultSerializer.instance).toList()
        assertThat(rows).containsExactlyElementsOf(listOf(
                MixedTypes("joe", 1, 6.53F),
                MixedTypes("bob", 2, 2.0F)
        ))
    }

    @Test
    fun `can deserialize CSV with no quote delimiter`() {
        val csv = """
            text,int,dec
            joe,1,6.53
            bob,2,2.0"""
        val rows = DataTableDeserializer.deserialize(csv, MixedTypes::class, DefaultSerializer.instance).toList()
        assertThat(rows).containsExactlyElementsOf(listOf(
                MixedTypes("joe", 1, 6.53F),
                MixedTypes("bob", 2, 2.0F)
        ))
    }

    @Test
    fun `empty CSV data causes an exception`() {
        val csv = ""
        assertThatThrownBy {
            DataTableDeserializer.deserialize(csv, MixedTypes::class, DefaultSerializer.instance).toList()
        }.matches {
            val error = (it as ValidationException).errors.single()
            error.code == "csv-empty"
        }
    }

    @Test
    fun `csv headers are case insensitive`() {
        val csv = """
            Text,Int,dec
            "joe",1,6.53
            "bob",2,2.0"""
        val rows = DataTableDeserializer.deserialize(csv, MixedTypes::class, DefaultSerializer.instance).toList()
        assertThat(rows).containsExactlyElementsOf(listOf(
                MixedTypes("joe", 1, 6.53F),
                MixedTypes("bob", 2, 2.0F)
        ))
    }

    @Test
    fun `error if headers do not match expected`() {
        val csv = """
            a,b,c,d
            1,2,3,4"""
        checkValidationError("csv-unexpected-header") {
            DataTableDeserializer.deserialize(csv, ABC::class, DefaultSerializer.instance).toList()
        }
    }

    @Test
    fun `error if row has too many columns`() {
        val csv = """
            a,b,c
            1,2,3
            1,2,3,4"""
        checkValidationError("csv-wrong-row-length:2") {
            DataTableDeserializer.deserialize(csv, ABC::class, DefaultSerializer.instance).toList()
        }
    }

    @Test
    fun `error if row has too few columns`() {
        val csv = """
            a,b,c
            1,2,3
            1,2"""
        checkValidationError("csv-wrong-row-length:2") {
            DataTableDeserializer.deserialize(csv, ABC::class, DefaultSerializer.instance).toList()
        }
    }

    @Test
    fun `error if row value in wrong data type`() {
        val csv = """
            text,int,dec
            "joe",1,3.14
            "sam",2.6,1"""
        checkValidationError("csv-bad-data-type:2:int", "Unable to parse '2.6' as Int? (Row 2, column int)") {
            DataTableDeserializer.deserialize(csv, MixedTypes::class, DefaultSerializer.instance).toList()
        }
    }

    @Test
    fun `error if NA value for non-nullable type`() {
        val csv = """
            text,int
            "x",5
            "y",NA"""
        checkValidationError("csv-bad-data-type:2:int", "Unable to parse 'NA' as Int (Row 2, column int)") {
            DataTableDeserializer.deserialize(csv, SomeRequiredColumns::class, DefaultSerializer.instance).toList()
        }
    }

    @Test
    fun `can deserialize CSV with single quoted flexible headers`() {
        val csv = """
            'a','b','x','y','z'
            1,"joe",1,2,3
            2,"bob",4,5,6"""
        val rows = DataTableDeserializer.deserialize(csv, Flexible::class, DefaultSerializer.instance).toList()
        assertThat(rows).containsExactlyElementsOf(listOf(
                Flexible(1, "joe", mapOf("x" to 1, "y" to 2, "z" to 3)),
                Flexible(2, "bob", mapOf("x" to 4, "y" to 5, "z" to 6))
        ))
    }

    @Test
    fun `can deserialize CSV with double quoted flexible headers`() {
        val csv = """
            "a","b","x","y","z"
            1,"joe",1,2,3
            2,"bob",4,5,6"""
        val rows = DataTableDeserializer.deserialize(csv, Flexible::class, DefaultSerializer.instance).toList()
        assertThat(rows).containsExactlyElementsOf(listOf(
                Flexible(1, "joe", mapOf("x" to 1, "y" to 2, "z" to 3)),
                Flexible(2, "bob", mapOf("x" to 4, "y" to 5, "z" to 6))
        ))
    }

    @Test
    fun `can deserialize CSV with no quoted flexible headers`() {
        val csv = """
            a,b,x,y,z
            1,"joe",1,2,3
            2,"bob",4,5,6"""
        val rows = DataTableDeserializer.deserialize(csv, Flexible::class, DefaultSerializer.instance).toList()
        assertThat(rows).containsExactlyElementsOf(listOf(
                Flexible(1, "joe", mapOf("x" to 1, "y" to 2, "z" to 3)),
                Flexible(2, "bob", mapOf("x" to 4, "y" to 5, "z" to 6))
        ))
    }

    @Test
    fun `error if flexible CSV has missing required headers`() {
        val csv = """
            a,x,y
            1,2,3
            3,4,5"""
        checkValidationError("csv-unexpected-header") {
            DataTableDeserializer.deserialize(csv, Flexible::class, DefaultSerializer.instance).toList()
        }
    }

    @Test
    fun `error if row in flexible CSV has too few values`() {
        val csv = """
            a,  b,x,y,z
            0,"p",1,2,3
            0,"q",1,2"""
        checkValidationError("csv-wrong-row-length:2") {
            DataTableDeserializer.deserialize(csv, Flexible::class, DefaultSerializer.instance).toList()
        }
    }

    @Test
    fun `error if row in flexible CSV has too many values`() {
        val csv = """
            a,  b,x,y,z
            0,"p",1,2,3
            0,"q",1,2,3,4"""
        checkValidationError("csv-wrong-row-length:2") {
            DataTableDeserializer.deserialize(csv, Flexible::class, DefaultSerializer.instance).toList()
        }
    }

    @Test
    fun `error if extra value in flexible CSV has wrong data type`() {
        val csv = """
            a,  b,x,y,z
            0,"p",1,2,3
            0,"q",1,2,3.5"""
        checkValidationError("csv-bad-data-type:2:z") {
            DataTableDeserializer.deserialize(csv, Flexible::class, DefaultSerializer.instance).toList()
        }
    }

    @AllColumnsRequired
    @FlexibleColumns
    data class ModelRun(val runId: String,  @FlexibleProperty val parameterValues: Map<String, String>)

    @Test
    fun `error if empty values when all columns required`() {
        val csv = """
            run_id,p1,p2,p3
            1,14,15.2,4
            2,14,15.3,"""
        checkValidationError("csv-missing-data:2:p3") {
            DataTableDeserializer.deserialize(csv, ModelRun::class, DefaultSerializer.instance).toList()
        }
    }

    @Test
    fun `no error if empty values when not all columns required`() {
        val csv = """
            a,b,x,y
            1,14,15.2,4
            2,14,15.3,"""

        val result =
                DataTableDeserializer.deserialize(csv, FlexibleWithStrings::class, DefaultSerializer.instance).toList()

        assertThat(result.last().extra["y"]).isEmpty()
    }

    private fun serialize(table: DataTable<*>) = serializeToStreamAndGetAsString {
        table.serialize(it, DefaultSerializer.instance)
    }.trim()

    private fun checkValidationError(code: String, message: String? = null, body: () -> Any?) {
        assertThatThrownBy { body() }
                .isInstanceOf(ValidationException::class.java)
                .matches {
                    val error = it as ValidationException
                    assertThat(error.errors.first().code).isEqualTo(code)
                    if (message != null) {
                        assertThat(error.errors.first().message).isEqualTo(message)
                    }
                    true
                }
    }
}