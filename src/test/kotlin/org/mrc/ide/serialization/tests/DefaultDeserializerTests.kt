package org.mrc.ide.serialization.tests

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Before
import org.junit.Test
import org.mrc.ide.serialization.DefaultDeserializer
import org.mrc.ide.serialization.Deserializer
import org.mrc.ide.serialization.UnknownEnumValue

class DefaultDeserializerTests
{
    private var deserializer = DefaultDeserializer.instance

    @Test
    fun `can parse enum`()
    {
        assertThat(deserializer.parseEnum<TestEnum>("open", TestEnum::class.java))
                .isEqualTo(TestEnum.OPEN)
    }

    @Test
    fun `can parse enum with hyphens`()
    {
        assertThat(deserializer.parseEnum<TestEnum>("in-preparation", TestEnum::class.java))
                .isEqualTo(TestEnum.IN_PREPARATION)
    }

    @Test
    fun `parseEnum throws exception if value is unrecognized`()
    {
        assertThatThrownBy { deserializer.parseEnum<TestEnum>("bad-value", TestEnum::class.java) }
                .isInstanceOf(UnknownEnumValue::class.java)
    }

}