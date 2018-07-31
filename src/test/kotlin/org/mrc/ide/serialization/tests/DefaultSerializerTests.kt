package org.mrc.ide.serialization.tests

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.beust.klaxon.json
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mrc.ide.serialization.DefaultSerializer
import org.mrc.ide.serialization.models.*
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId

class DefaultSerializerTests
{
    private val serializer = DefaultSerializer.instance

    @Test
    fun `convertFieldName handles empty string`()
    {
        assertThat(serializer.convertFieldName("")).isEqualTo("")
    }
    @Test
    fun `convertFieldName handles lowercase field name`()
    {
        assertThat(serializer.convertFieldName("field")).isEqualTo("field")
    }
    @Test
    fun `convertFieldName handles camelCase field name`()
    {
        assertThat(serializer.convertFieldName("camelCase")).isEqualTo("camel_case")
    }

    enum class TestEnum { SIMPLE, COMPLEX_VALUE }

    @Test
    fun `serializeEnum serializes any enum correctly`()
    {
        assertThat(serializer.serializeEnum(TestEnum.SIMPLE)).isEqualTo("simple")
        assertThat(serializer.serializeEnum(TestEnum.COMPLEX_VALUE)).isEqualTo("complex-value")
    }

    @Test
    fun `can serialize LocalDate`()
    {
        val actual = LocalDate.of(1988, Month.MARCH, 19)
        val expected = "\"1988-03-19\""
        checkSerializedForm(expected, actual)
    }

    @Test
    fun `can serialize Instant`()
    {
        val actual = LocalDate.of(1, Month.DECEMBER, 25)
                .atStartOfDay(ZoneId.of("UTC"))
                .toInstant()
        val expected = "\"0001-12-25T00:00:00Z\""
        checkSerializedForm(expected, actual)
    }

    @Test
    fun `can serialize ResultStatus`()
    {
        val actual = ResultStatus.SUCCESS
        val expected = "\"SUCCESS\""
        checkSerializedForm(expected, actual)
    }

    @Test
    fun `toResult wraps object in successful Result`()
    {
        val data = 31415
        val actual = serializer.toResult(data)
        val expected = json {
            obj(
                    "data" to 31415,
                    "errors" to array(),
                    "status" to "SUCCESS"
            )
        }
        assertThat(parse(actual)).isEqualTo(expected)
    }

    @Test
    fun `toJson serializes Result correctly`()
    {
        val result = Result(
                ResultStatus.FAILURE,
                31415,
                listOf(ErrorInfo("code", "message"))
        )
        val actual = serializer.toJson(result)
        val expected = json {
            obj(
                    "data" to 31415,
                    "errors" to array(obj("code" to "code", "message" to "message")),
                    "status" to "FAILURE"
            )
        }
        assertThat(parse(actual)).isEqualTo(expected)
    }

    fun checkSerializedForm(expected: String, actual: Any): Unit
    {
        val actualAsString = serializer.gson.toJson(actual)
        assertThat(actualAsString).isEqualTo(expected)
    }

    fun parse(string: String): JsonObject = Parser().parse(StringBuilder(string)) as JsonObject
}