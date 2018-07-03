package org.mrc.ide.serialization.tests

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.charset.Charset

fun serializeToStreamAndGetAsString(work: (OutputStream) -> Unit): String
{
    return ByteArrayOutputStream().use {
        work(it)
        String(it.toByteArray(), Charset.defaultCharset())
    }
}