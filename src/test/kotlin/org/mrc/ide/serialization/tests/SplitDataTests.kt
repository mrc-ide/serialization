package org.mrc.ide.serialization.tests

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mrc.ide.serialization.DefaultSerializer
import org.mrc.ide.serialization.Serializer
import org.mrc.ide.serialization.SplitData
import org.mrc.ide.serialization.StreamSerializable
import java.io.OutputStream

class SplitDataTests
{
    @Test
    fun `can serialize`()
    {
        val serializer = mock<Serializer> {
            on { it.toResult(any()) } doReturn "METADATA"
        }
        val table = mock<StreamSerializable<Any>> {
            on { it.serialize(any()) } doAnswer { invocationOnMock ->
                val stream = invocationOnMock.getArgument<OutputStream>(0)
                stream.bufferedWriter().use {
                    writer -> writer.write("ROWS")
                }
            }
        }
        val data = SplitData(1, table, serializer)
        val actual = serializeToStreamAndGetAsString { stream ->
            data.serialize(stream)
        }.trim()
        assertThat(actual).isEqualTo("METADATA\n---\nROWS")
    }
}