package org.mrc.ide.serialization.models

data class ErrorInfo(val code: String, val message: String)
{
    override fun toString(): String = message
}