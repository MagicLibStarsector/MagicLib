package org.magiclib

import com.fs.starfarer.api.Global
import org.magiclib.ReflectionUtils.getFieldsMatching
import org.magiclib.ReflectionUtils.getMethodsMatching

internal object ReflectionUtilsSafe {
    internal fun Any.safeInvoke(name: String? = null, vararg args: Any?): Any? {
        val paramTypes = args.map { arg -> arg?.let { it::class.javaPrimitiveType ?: it::class.java } }.toTypedArray()
        val reflectedMethods = this.getMethodsMatching(name, parameterTypes = paramTypes)
        if (reflectedMethods.isEmpty()) {
            Global.getLogger(this.javaClass).error(
                "No method found for name: '$name' on class: ${this::class.java.name} " +
                        "with compatible parameter types derived from arguments: ${paramTypes.contentToString()}"
            )
        } else if (reflectedMethods.size > 1) {
            Global.getLogger(this.javaClass).error(
                "Ambiguous method call for name: '$name' on class: ${this::class.java.name}. " +
                        "Multiple methods match parameter types derived from arguments: ${paramTypes.contentToString()}"
            )
        } else return reflectedMethods[0].invoke(this, *args)

        return null
    }

    internal fun Class<*>.safeInvoke(name: String? = null, vararg args: Any?): Any? {
        val paramTypes = args.map { arg -> arg?.let { it::class.javaPrimitiveType ?: it::class.java } }.toTypedArray()
        val reflectedMethods = this.getMethodsMatching(name, parameterTypes = paramTypes)
        if (reflectedMethods.isEmpty())
            Global.getLogger(this.javaClass).error(
                "No method found for name: '$name' on class: ${this::class.java.name} " +
                        "with compatible parameter types derived from arguments: ${paramTypes.contentToString()}"
            )
        else if (reflectedMethods.size > 1)
            Global.getLogger(this.javaClass).error(
                "Ambiguous method call for name: '$name' on class: ${this::class.java.name}. " +
                        "Multiple methods match parameter types derived from arguments: ${paramTypes.contentToString()}"
            )
        else return reflectedMethods[0].invoke(null, *args)
        return null
    }

    internal fun Any.safeGet(name: String? = null, type: Class<*>? = null, searchSuperclass: Boolean = false): Any? {
        val reflectedFields = this.getFieldsMatching(name, fieldAssignableTo = type, searchSuperclass = searchSuperclass)
        if (reflectedFields.isEmpty())
            Global.getLogger(this.javaClass).error(
                "No field found for name: '${name ?: "<any>"}' on class: ${this::class.java.name} " +
                        "that is assignable to type: '${type?.name ?: "<any>"}'."
            )
        else if (reflectedFields.size > 1)
            Global.getLogger(this.javaClass).error(
                "Ambiguous fields with name: '${name ?: "<any>"}' on class ${this::class.java.name} " +
                        "assignable to type: '${type?.name ?: "<any>"}'. Multiple fields match."
            )
        else return reflectedFields[0].get(this)

        return null
    }

    internal fun Any.safeSet(name: String? = null, value: Any?, searchSuperclass: Boolean = false) {
        val valueType = value?.let { it::class.javaPrimitiveType ?: it::class.java }
        val reflectedFields = this.getFieldsMatching(name, fieldAccepts = valueType, searchSuperclass = searchSuperclass)
        if (reflectedFields.isEmpty())
            Global.getLogger(this.javaClass).error(
                "No field found for name: '${name ?: "<any>"}' on class: ${this::class.java.name} " +
                        "that is accepts type: '${valueType?.name ?: "null"}'."
            )
        else if (reflectedFields.size > 1)
            Global.getLogger(this.javaClass).error(
                "Ambiguous fields with name: '${name ?: "<any>"}' on class ${this::class.java.name} " +
                        "assignable to type: '${valueType?.name ?: "null"}'. Multiple fields match."
            )
        else return reflectedFields[0].set(this, value)
    }
}