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

    /**
     * The name of the application module. Only needs to be set if the application is modular _and_ all dependencies
     * are modular.
     */
    val applicationModuleName = objectFactory.property<String>()

    /**
     * The fully qualified name of the main application class. Only needs to be set if the application is modular
     * _and_ all dependencies are modular.
     */
    val mainClassName = objectFactory.property<String>()

    fun getVmOptions(): List<String> = vmOptions.get()

    fun getLauncherName(): String = launcherName.get()

    fun getApplicationModuleName(): String = applicationModuleName.get()

    fun getMainClassName(): String = mainClassName.get()

    fun setVmOptions(options: Iterable<String>) {
        vmOptions.set(options.toList())
    }
}
