package org.magiclib

import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

internal object ReflectionUtils {

    private val fieldClass = Class.forName("java.lang.reflect.Field", false, Class::class.java.classLoader)
    private val setFieldHandle = MethodHandles.lookup().findVirtual(
        fieldClass, "set",
        MethodType.methodType(Void.TYPE, Any::class.java, Any::class.java))
    private val getFieldHandle = MethodHandles.lookup().findVirtual(
        fieldClass, "get",
        MethodType.methodType(Any::class.java, Any::class.java))
    private val getFieldTypeHandle = MethodHandles.lookup().findVirtual(
        fieldClass, "getType",
        MethodType.methodType(Class::class.java))
    private val getFieldNameHandle = MethodHandles.lookup().findVirtual(
        fieldClass, "getName",
        MethodType.methodType(String::class.java))
    private val setFieldAccessibleHandle = MethodHandles.lookup().findVirtual(
        fieldClass, "setAccessible",
        MethodType.methodType(Void.TYPE, Boolean::class.javaPrimitiveType))

    private val methodClass = Class.forName("java.lang.reflect.Method", false, Class::class.java.classLoader)
    private val getMethodNameHandle = MethodHandles.lookup().findVirtual(
        methodClass, "getName",
        MethodType.methodType(String::class.java))
    private val invokeMethodHandle = MethodHandles.lookup().findVirtual(
        methodClass, "invoke",
        MethodType.methodType(Any::class.java, Any::class.java, Array<Any>::class.java))
    private val setMethodAccessibleHandle = MethodHandles.lookup().findVirtual(
        methodClass, "setAccessible",
        MethodType.methodType(Void.TYPE, Boolean::class.javaPrimitiveType))
    private val getMethodReturnHandle = MethodHandles.lookup().findVirtual(
        methodClass, "getReturnType",
        MethodType.methodType(Class::class.java))
    private val getMethodParametersHandle = MethodHandles.lookup().findVirtual(
        methodClass, "getParameterTypes",
        MethodType.methodType(arrayOf<Class<*>>().javaClass))

    private val constructorClass = Class.forName("java.lang.reflect.Constructor", false, Class::class.java.classLoader)
    private val getConstructorParametersHandle = MethodHandles.lookup().findVirtual(
        constructorClass, "getParameterTypes", MethodType.methodType(arrayOf<Class<*>>().javaClass))


    @JvmStatic
    fun set(fieldName: String, instanceToModify: Any, newValue: Any?) {
        var field: Any? = null

        try {
            field = instanceToModify.javaClass.getField(fieldName)
        } catch (_: Exception) {
        }
        if (field == null) {
            field = instanceToModify.javaClass.getDeclaredField(fieldName)
        }

        setFieldAccessibleHandle.invoke(field, true)
        setFieldHandle.invoke(field, instanceToModify, newValue)
    }

    @JvmStatic
    fun get(fieldName: String, instanceToGetFrom: Any): Any? {
        var field: Any? = null

        try {
            field = instanceToGetFrom.javaClass.getField(fieldName)
        } catch (_: Exception) {
        }
        if (field == null) {
            field = instanceToGetFrom.javaClass.getDeclaredField(fieldName)
        }

        setFieldAccessibleHandle.invoke(field, true)
        return getFieldHandle.invoke(field, instanceToGetFrom)
    }

    @JvmStatic
    fun getFromSuper(fieldName: String, instanceToGetFrom: Any): Any? {
        var field: Any? = null

        try {
            field = instanceToGetFrom.javaClass.superclass.getField(fieldName)
        } catch (_: Exception) {
        }
        if (field == null) {
            field = instanceToGetFrom.javaClass.superclass.getDeclaredField(fieldName)
        }

        setFieldAccessibleHandle.invoke(field, true)
        return getFieldHandle.invoke(field, instanceToGetFrom)
    }

    fun hasFieldOfName(name: String, instance: Any): Boolean {
        val instancesOfFields: Array<out Any> = instance.javaClass.getDeclaredFields()
        return instancesOfFields.any { getFieldNameHandle.invoke(it) == name }
    }

    fun findFieldWithMethodReturnType(instance: Any, clazz: Class<*>): ReflectedField? {
        val instancesOfFields: Array<out Any> = instance.javaClass.declaredFields

        return instancesOfFields.map { fieldObj -> fieldObj to getFieldTypeHandle.invoke(fieldObj) }
            .firstOrNull { (fieldObj, fieldClass) ->
                ((fieldClass!! as Class<Any>).declaredMethods as Array<Any>).any { methodObj ->
                    getMethodReturnHandle.invoke(
                        methodObj
                    ) == clazz
                }
            }?.let { (fieldObj, fieldClass) ->
                return ReflectedField(fieldObj)
            }
    }

    fun findFieldWithMethodName(instance: Any, methodName: String): ReflectedField? {
        val instancesOfFields: Array<out Any> = instance.javaClass.declaredFields

        return instancesOfFields.map { fieldObj -> fieldObj to getFieldTypeHandle.invoke(fieldObj) }
            .firstOrNull { (fieldObj, fieldClass) ->
                hasMethodOfNameInClass(methodName, fieldClass as Class<Any>)
            }?.let { (fieldObj, fieldClass) ->
                return ReflectedField(fieldObj)
            }
    }

    fun findFieldsOfType(instance: Any, clazz: Class<*>): List<ReflectedField> {
        val instancesOfFields: Array<out Any> = instance.javaClass.declaredFields

        return instancesOfFields.map { fieldObj -> fieldObj to getFieldTypeHandle.invoke(fieldObj) }
            .filter { (fieldObj, fieldClass) ->
                fieldClass == clazz
            }.map { (fieldObj, fieldClass) -> ReflectedField(fieldObj) }
    }

    fun getFieldsOfType(instance: Any, clazz: Class<*>): List<String> {
        val instancesOfMethods: Array<out Any> = instance.javaClass.getDeclaredFields()

        return instancesOfMethods.filter { clazz.isAssignableFrom(getFieldTypeHandle.invoke(it) as Class<*>) }
            .map { getFieldNameHandle.invoke(it) as String }
    }

    fun hasMethodOfName(name: String, instance: Any, contains: Boolean = false): Boolean {
        val instancesOfMethods: Array<out Any> = instance.javaClass.getDeclaredMethods()

        return if (!contains) {
            instancesOfMethods.any { getMethodNameHandle.invoke(it) == name }
        } else {
            instancesOfMethods.any { (getMethodNameHandle.invoke(it) as String).contains(name) }
        }
    }

    fun getMethodsOfName(name: String, instance: Any): List<Any> {
        val instancesOfMethods: Array<out Any> = instance.javaClass.getDeclaredMethods()
        return instancesOfMethods.filter { getMethodNameHandle.invoke(it) == name }
    }

    fun getMethodOfReturnType(instance: Any, clazz: Class<*>): String? {
        val instancesOfMethods: Array<out Any> = instance.javaClass.getDeclaredMethods()

        return instancesOfMethods.firstOrNull {
            getMethodReturnHandle.invoke(it) == clazz
        }?.let { getMethodNameHandle.invoke(it) as String }
    }

    @Suppress("USELESS_CAST")
    fun invoke(methodName: String, instance: Any, vararg arguments: Any?, declared: Boolean = false): Any? {
        val method: Any?

        val clazz = instance.javaClass
        val args = arguments.map { it!!::class.javaPrimitiveType ?: it::class.java }
        val methodType = MethodType.methodType(Void.TYPE, args)

        method = if (!declared) {
            clazz.getMethod(methodName, *methodType.parameterArray()) as Any?
        } else {
            clazz.getDeclaredMethod(methodName, *methodType.parameterArray()) as Any?
        }

        if (declared) setMethodAccessibleHandle.invoke(method, true)

        return invokeMethodHandle.invoke(method, instance, arguments)
    }

    fun rawInvoke(method: Any?, instance: Any, vararg arguments: Any?): Any? {
        return invokeMethodHandle.invoke(method, instance, arguments)
    }

    @Suppress("USELESS_CAST")
    fun invokeStatic(methodName: String, clazz: Class<*>, vararg arguments: Any?, declared: Boolean = false): Any? {
        val method: Any?
        val args = arguments.map { it!!::class.javaPrimitiveType ?: it::class.java }
        val methodType = MethodType.methodType(Void.TYPE, args)

        method = if (!declared) {
            clazz.getMethod(methodName, *methodType.parameterArray()) as Any?
        } else {
            clazz.getDeclaredMethod(methodName, *methodType.parameterArray()) as Any?
        }

        return invokeMethodHandle.invoke(method, null, arguments)
    }

    fun hasMethodOfNameInClass(name: String, instance: Class<Any>, contains: Boolean = false): Boolean {
        val instancesOfMethods: Array<out Any> = instance.getDeclaredMethods()

        return if (!contains) {
            instancesOfMethods.any { getMethodNameHandle.invoke(it) == name }
        } else {
            instancesOfMethods.any { (getMethodNameHandle.invoke(it) as String).contains(name) }
        }
    }

    fun getMethodArguments(method: String, instance: Any): List<Array<Class<*>>> {
        val instancesOfMethods: Array<out Any> = instance.javaClass.declaredMethods
        return instancesOfMethods.filter { getMethodNameHandle.invoke(it) == method }
            .map { getMethodParametersHandle.invoke(it) as Array<Class<*>> }
    }

    fun getMethodArguments(method: String, clazz: Class<*>): List<Array<Class<*>>> {
        val instancesOfMethods: Array<out Any> = clazz.declaredMethods
        return instancesOfMethods.filter { getMethodNameHandle.invoke(it) == method }
            .map { getMethodParametersHandle.invoke(it) as Array<Class<*>> }
    }

    fun hasConstructorOfParameters(instance: Any, vararg parameterTypes: Class<*>): Boolean {
        val constructors = instance.javaClass.getDeclaredConstructors() as Array<*>
        // Iterate over each constructor and check its parameter types.
        for (ctor in constructors) {
            val params = getConstructorParametersHandle.invoke(ctor) as Array<Class<*>>
            if (params.contentEquals(parameterTypes)) return true
        }
        return false
    }

    fun getConstructor(clazz: Class<*>, vararg arguments: Class<*>): MethodHandle =
        MethodHandles.lookup().findConstructor(clazz, MethodType.methodType(Void.TYPE, arguments))

    fun instantiate(clazz: Class<*>, vararg arguments: Any?): Any? {
        val args = arguments.map { it!!::class.javaPrimitiveType ?: it::class.java }

        val constructorHandle = getConstructor(clazz, *args.toTypedArray())

        return constructorHandle.invokeWithArguments(arguments.toList())
    }


    class ReflectedField(val field: Any) {
        fun get(instance: Any?): Any? {
            setFieldAccessibleHandle.invoke(field, true)
            return getFieldHandle.invoke(field, instance)
        }

        fun set(instance: Any?, value: Any?) {
            setFieldHandle.invoke(field, instance, value)
        }
    }

    class ReflectedMethod(val method: Any) {
        fun invoke(instance: Any?, vararg arguments: Any?): Any? =
            invokeMethodHandle.invoke(method, instance, arguments)
    }
}