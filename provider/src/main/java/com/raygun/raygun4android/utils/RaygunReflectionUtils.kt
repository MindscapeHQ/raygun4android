package com.raygun.raygun4android.utils

import java.lang.reflect.Method

object RaygunReflectionUtils {
    @Throws(NoSuchMethodException::class)
    fun findMethod(
        clazz: Class<*>,
        methodName: String,
        args: Array<Class<*>>,
    ): Method {
        var methodMatched: Method? = null

        for (m in getAllMethods(clazz)) {
            if (m.name == methodName) {
                val paramClasses = m.parameterTypes

                if (paramClasses.size == args.size) {
                    var paramsMatch = true

                    for (i in paramClasses.indices) {
                        var paramType = paramClasses[i]
                        paramType = convertPrimitiveClass(paramType)

                        if (paramType != args[i]) {
                            paramsMatch = false
                            break
                        }
                    }

                    if (paramsMatch) {
                        methodMatched = m
                        break
                    }
                }
            }
        }

        if (methodMatched != null) {
            return methodMatched
        } else {
            throw NoSuchMethodException("Cannot find method: $methodName")
        }
    }

    private fun getAllMethods(clazz: Class<*>): Collection<Method> {
        val methods = HashSet<Method>()

        methods.addAll(listOf(*clazz.declaredMethods))

        for (s in getAllSuperClasses(clazz)) {
            methods.addAll(listOf(*s.declaredMethods))
        }

        return methods
    }

    private fun getAllSuperClasses(clazz: Class<*>?): Collection<Class<*>> {
        val classes = HashSet<Class<*>>()

        if ((clazz != null) && (clazz != Any::class.java)) {
            classes.add(clazz)
            classes.addAll(getAllSuperClasses(clazz.superclass))

            for (i in clazz.interfaces) {
                classes.addAll(getAllSuperClasses(i))
            }
        }
        return classes
    }

    private fun convertPrimitiveClass(primitive: Class<*>): Class<*> {
        if (primitive.isPrimitive) {
            if (primitive == Integer.TYPE) {
                return Int::class.java
            }
            if (primitive == java.lang.Boolean.TYPE) {
                return Boolean::class.java
            }
            if (primitive == java.lang.Float.TYPE) {
                return Float::class.java
            }
            if (primitive == java.lang.Long.TYPE) {
                return Long::class.java
            }
            if (primitive == java.lang.Double.TYPE) {
                return Double::class.java
            }
            if (primitive == java.lang.Short.TYPE) {
                return Short::class.java
            }
            if (primitive == java.lang.Byte.TYPE) {
                return Byte::class.java
            }
            if (primitive == Character.TYPE) {
                return Char::class.java
            }
        }
        return primitive
    }
}
