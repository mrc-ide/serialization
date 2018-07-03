package org.mrc.ide.serialization.models

@Target(AnnotationTarget.PROPERTY)
annotation class SerializationRule(val rule: Rule)

enum class Rule
{
    EXCLUDE_IF_NULL
}

annotation class AllColumnsRequired