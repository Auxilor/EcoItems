package com.willfp.ecoitems.compat

import com.willfp.eco.core.Prerequisite
import com.willfp.libreforge.proxy.InvalidProxyException

private const val BASE_PACKAGE = "com.willfp.ecoitems.compat.modern"
private val isModern = Prerequisite.HAS_PAPER.isMet && Prerequisite.HAS_1_21.isMet

internal annotation class ModernCompatibilityProxy(
    val location: String
)

private val cache = mutableMapOf<Class<*>, Any>()

internal object ModernCompatibilityScope {
    inline fun <reified T> loadProxy(): T {
        return loadCompatibilityProxy(T::class.java)
    }

    inline fun <reified T> useProxy(block: T.() -> Unit) {
        val proxy = loadProxy<T>()

        with(proxy) {
            block()
        }
    }
}

internal object PotentiallyModernScope {
    fun otherwise(block: () -> Any?) {
        if (!isModern) {
            block()
        }
    }
}

internal fun <R1> ifModern(
    block: ModernCompatibilityScope.() -> R1
): PotentiallyModernScope {
    if (isModern) {
        block(ModernCompatibilityScope)
    }

    return PotentiallyModernScope
}

internal fun <T> loadCompatibilityProxy(clazz: Class<T>): T {
    @Suppress("UNCHECKED_CAST")
    return cache.getOrPut(clazz) {
        loadProxyUncached(clazz)
    } as T
}

private fun loadProxyUncached(clazz: Class<*>): Any {
    val proxy = clazz.getAnnotation(ModernCompatibilityProxy::class.java)
    val location = proxy?.location ?: throw IllegalArgumentException("Class ${clazz.name} is not a proxy")
    val className = "$BASE_PACKAGE.$location"

    try {
        val found = Class.forName(className)

        val constructor = found.getConstructor()
        val instance = constructor.newInstance()

        if (!clazz.isInstance(instance)) {
            throw InvalidProxyException("Modern compatibility proxy class $className does not implement ${clazz.name}")
        }

        return instance
    } catch (e: ClassNotFoundException) {
        throw InvalidProxyException("Could not find modern compatibility proxy class $className")
    } catch (e: NoSuchMethodException) {
        throw InvalidProxyException("Could not find no-args constructor for modern compatibility proxy class $className")
    }
}
