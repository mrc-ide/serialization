package org.mrc.ide.serialization.models

class Result(val status: ResultStatus, data: Any?, errors: Iterable<ErrorInfo>)
{
    val data = data ?: ""
    val errors = errors.toList()
}

enum class ResultStatus
{
    SUCCESS, FAILURE
}