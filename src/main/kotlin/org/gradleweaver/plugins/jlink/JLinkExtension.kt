package org.gradleweaver.plugins.jlink

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Project
import javax.inject.Inject

open class JLinkExtension
@Inject
internal constructor(
        project: Project
) {
    val configure: NamedDomainObjectCollection<JLinkOptions> = project.container(JLinkOptions::class.java) { name ->
        JLinkOptions(name = name)
    }
}