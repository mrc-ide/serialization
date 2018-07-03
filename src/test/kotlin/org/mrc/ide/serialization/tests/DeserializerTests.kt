package org.mrc.ide.serialization.tests

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Before
import org.junit.Test
import org.mrc.ide.serialization.Deserializer
import org.mrc.ide.serialization.UnknownEnumValue

class DeserializerTests
{
    lateinit var deserializer: Deserializer

    @Before
    fun createDeserializer()
    {
        deserializer = Deserializer()
    }

    @Test
    fun `can parse enum`()
    {
        assertThat(deserializer.parseEnum<TestEnum>("open"))
                .isEqualTo(TestEnum.OPEN)
    }

    @Test
    fun `can parse enum with hyphens`()
    {
        assertThat(deserializer.parseEnum<TestEnum>("in-preparation"))
                .isEqualTo(TestEnum.IN_PREPARATION)
    }

    @Test
    fun `parseEnum throws exception if value is unrecognized`()
    {
        assertThatThrownBy { deserializer.parseEnum<TestEnum>("bad-value") }
                .isInstanceOf(UnknownEnumValue::class.java)
    }

}