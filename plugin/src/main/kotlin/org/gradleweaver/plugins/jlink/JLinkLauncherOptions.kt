package org.gradleweaver.plugins.jlink

import org.gradle.api.Project

class JLinkLauncherOptions(project: Project) {

    private val objectFactory = project.objects

    /**
     * Runtime options for the JVM.
     */
    val vmOptions = objectFactory.listProperty<String>()

    /**
     * The name of the generated launcher script. If not set, the launcher name will be the name of the
     * corresponding project.
     */
    val launcherName = objectFactory.property(project.name)

    // For Groovy DSL support
    fun setLauncherName(launcherName: String) {
        this.launcherName.set(launcherName)
    }

    fun setVmOptions(options: Iterable<String>) {
        vmOptions.set(options.toList())
    }

}
