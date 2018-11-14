package org.gradleweaver.plugins.jlink

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

internal inline fun <reified T : Any> ObjectFactory.property() = property(T::class.java)
internal inline fun <reified T : Any> ObjectFactory.property(initialValue: T): Property<T> {
    val prop = property(T::class.java)
    prop.set(initialValue)
    return prop
}

internal inline fun <reified T : Any> ObjectFactory.listProperty() = listProperty(T::class.java)

internal fun Property<*>.isNotPresent() = !isPresent
