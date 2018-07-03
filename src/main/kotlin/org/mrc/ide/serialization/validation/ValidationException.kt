package org.mrc.ide.serialization.validation

import org.mrc.ide.serialization.models.ErrorInfo

class ValidationException(val errors: List<ErrorInfo>)
    : Exception()
{
    override fun toString(): String
            = "${super.toString()}: ${errors.joinToString("\n")}"
}