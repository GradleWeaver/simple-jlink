package org.gradleweaver.plugins.jlink

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property

/**
 * Generates an executable native launcher script for the application.
 */
open class JLinkLauncherTask : DefaultTask() {

    private val generator = LauncherGenerator()

    /**
     * Launch options for the JVM.
     */
    @get:Input
    @get:Optional
    val vmOptions = project.objects.listProperty<String>()

    /**
     * The application JAR file. Does not need to be set if the application is modular.
     */
    @get:Optional
    @get:InputFile
    val applicationJarLocation: RegularFileProperty = newInputFile()

    /**
     * The directory in which the jlink image should be build. By default, this is is `${project.buildDir}/jlink`.
     */
    @get:InputDirectory
    @get:Optional
    val jlinkDir: DirectoryProperty = newInputDirectory()

    /**
     * The name of the application launcher. Defaults to the project name if not set.
     */
    @get:Input
    @get:Optional
    val launcherName = project.objects.property<String>()

    /**
     * The name of the application module. This _must_ be set if the application is modular.
     */
    @get:Input
    @get:Optional
    val moduleName = project.objects.property<String>()

    /**
     * The application main class. This _must_ be set if the application is modular.
     */
    @get:Input
    @get:Optional
    val mainClassName = project.objects.property<String>()

    @TaskAction
    fun generateScript() {
        val os = OperatingSystem.current()
        val vmOpts = vmOptions.orNull?.joinToString(separator = " ") ?: ""

        val scriptText = if (applicationJarLocation.isPresent) {
            generator.generateJarScript(os, vmOpts, applicationJarLocation.get().asFile.name)
        } else {
            generator.generateModuleLaunchScript(os, vmOpts, moduleName.get(), mainClassName.get())
        }

        generator.generateScriptFile(os, scriptText, getJlinkBin(), launcherName.getOrElse(project.name))
    }

    @TaskAction
    fun copyJar() {
        project.copy {
            from(applicationJarLocation)
            into(getJlinkBin())
        }
    }

    private fun getJlinkBin() = jlinkDir.asFile.getOrElse(project.buildDir.resolve("jlink")).resolve("bin")

}