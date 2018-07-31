package org.mrc.ide.serialization

import java.io.OutputStream

interface StreamSerializable<out T>
{
    val contentType: String
    fun serialize(stream: OutputStream)
    val data: Sequence<T>
}