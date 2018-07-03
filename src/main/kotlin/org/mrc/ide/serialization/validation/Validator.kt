package org.mrc.ide.serialization.validation

import org.mrc.ide.serialization.models.AllowedFormat
import org.mrc.ide.serialization.models.ErrorInfo
import org.mrc.ide.serialization.models.MinimumLength
import org.mrc.ide.serialization.models.RequiredWhen
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberFunctions

class Validator()
{
    fun applyRule(annotation: Annotation, value: Any?, qualifiedFieldName: String, model: Any)
            : List<ErrorInfo>
    {
        return when (annotation)
        {
            is AllowedFormat -> checkFormat(annotation, value, qualifiedFieldName)
            is MinimumLength -> checkLength(annotation, value, qualifiedFieldName)
            is RequiredWhen -> checkRequiredWhen(annotation, value, qualifiedFieldName, model)
            else -> listOf()
        }
    }

    fun checkBlank(value: Any?, qualifiedFieldName: String): List<ErrorInfo>
    {
        if (value is String && value.isBlank())
        {
            return listOf(ErrorInfo(
                    "${invalidFieldCode(qualifiedFieldName)}:blank",
                    "You have supplied an empty or blank string for field '$qualifiedFieldName'"
            ))
        }

        return listOf()
    }

    fun checkNull(value: Any?, property: KProperty1<Any, *>, qualifiedFieldName: String): List<ErrorInfo>
    {
        if (value != null || property.returnType.isMarkedNullable)
            return listOf()

        return missingFieldError(qualifiedFieldName)
    }

    private fun missingFieldError(qualifiedFieldName: String): List<ErrorInfo>
    {
        return listOf(ErrorInfo(
                "${invalidFieldCode(qualifiedFieldName)}:missing",
                "You have not supplied a value, or have supplied a null value, for field '$qualifiedFieldName'"
        ))
    }

    private fun checkFormat(format: AllowedFormat, value: Any?, qualifiedFieldName: String): List<ErrorInfo>
    {
        if (value is String && !value.isBlank())
        {
            val regex = Regex(format.pattern)
            if (!regex.matches(value))
            {
                return listOf(ErrorInfo(
                        "${invalidFieldCode(qualifiedFieldName)}:bad-format",
                        "The '$qualifiedFieldName' field must be in the form '${format.example}'"
                ))
            }
        }
        return listOf()
    }

    private fun checkLength(length: MinimumLength, value: Any?, qualifiedFieldName: String): List<ErrorInfo>
    {
        if (value is String && !value.isBlank())
        {
            if (value.length < length.length)
            {
                return listOf(ErrorInfo(
                        "${invalidFieldCode(qualifiedFieldName)}:too-short",
                        "The '$qualifiedFieldName' field must be at least ${length.length} characters long"
                ))
            }
        }
        return listOf()
    }

    private fun invalidFieldCode(qualifiedFieldName: String): String
            = "invalid-field:$qualifiedFieldName"


    private fun checkRequiredWhen(requiredWhen: RequiredWhen, value: Any?, qualifiedFieldName: String, model: Any): List<ErrorInfo>
    {
        val functionName = requiredWhen.functionName
        val functions = model::class.memberFunctions
        val function = functions.singleOrNull { it.name == functionName && it.returnType.classifier == Boolean::class }
                ?: throw Exception("Class '${model::class.simpleName}' doesn't have a function '$functionName'")

        try
        {
            if (function.call(model) as Boolean)
            {
                return checkMissing(value, qualifiedFieldName)
            }

            return listOf()
        }
        catch(e: InvocationTargetException)
        {
            return listOf()
        }
    }

    private fun checkMissing(value: Any?, qualifiedFieldName: String): List<ErrorInfo>
    {
        value ?: return missingFieldError(qualifiedFieldName)
        return listOf()
    }
}