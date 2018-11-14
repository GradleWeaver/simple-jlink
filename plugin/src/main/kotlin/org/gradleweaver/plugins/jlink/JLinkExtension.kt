package org.gradleweaver.plugins.jlink

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import javax.inject.Inject

open class JLinkExtension
@Inject
internal constructor(
        project: Project
) {
    val configure: NamedDomainObjectContainer<JLinkOptions> = project.container(JLinkOptions::class.java) { name ->
        JLinkOptions(project = project, name = name)
    }

    /**
     * Registers a new jlink configuration.
     *
     * ```
     * jlink {
     *   "example" {
     *     // options...
     *   }
     * }
     * ```
     *
     * is equivalent to
     * ```
     * jlink {
     *   register("example") {
     *     // options...
     *   }
     * }
     * ```
     */
    operator fun String.invoke(configurationAction: Action<in JLinkOptions>) = register(this, configurationAction)

    /**
     * Registers a new jlink configuration.
     *
     * @param name                the name of the configuration
     * @param configurationAction the action to use to set up the configuration
     */
    fun register(name: String, configurationAction: Action<in JLinkOptions>)
            = configure.register(name, configurationAction)

}
