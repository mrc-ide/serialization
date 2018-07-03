package org.mrc.ide.serialization.models

annotation class CanBeBlank
annotation class AllowedFormat(val pattern: String, val example: String)
annotation class MinimumLength(val length: Int)

// property should be the name of a Boolean property on the model
annotation class RequiredWhen(val functionName: String)