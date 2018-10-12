package org.gradleweaver.plugins.jlink

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.jvm.Jvm
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import javax.inject.Inject

// Note: must be open so Gradle can create a proxy subclass
open class JLinkTask : DefaultTask {

    internal val options: JLinkOptions

    @Inject
    constructor(options: JLinkOptions) {
        this.options = options
    }

    @InputFile
    var applicationJarLocation: RegularFileProperty = newInputFile()

    @OutputDirectory
    var jlinkDir: DirectoryProperty = newOutputDirectory()

    @TaskAction
    fun executeJLink() {
        options.execJLink(project, applicationJarLocation.get().asFile.absolutePath)

        // Copy the application JAR into the jlink bin directory
        project.copy {
            from(applicationJarLocation)
            rename {
                "${project.name.toLowerCase()}.jar"
            }
            into("${jlinkDir.get().asFile}/bin")
        }
    }

    /**
     * Configures the jlink options.
     */
    fun options(options: JLinkOptions.() -> Unit) {
        this.options.options()
    }
}

private val javaBin = Jvm.current().javaHome.resolve("bin")

/**
 * Runs `jdeps` on the specified JAR file to determine its module dependencies.
 *
 * @param jar the path to the JAR file on which to run `jdeps`
 */
fun jdeps(project: Project, jar: String): List<String> {
    return ByteArrayOutputStream().use { os ->
        // Get the standard library modules used by Shuffleboard and its dependencies
        project.exec {
            commandLine = listOf(javaBin.resolve("jdeps").toString(), "--list-deps", jar)
            standardOutput = os
        }
        val out = os.toString(Charset.defaultCharset())
        out.split("\n")
                .filter { it.startsWith("   ") }
                .filter { !it.contains('/') }
                .filter { it == it.toLowerCase() }
                .map { it.substring(3) }
    }
}

private fun JLinkOptions.buildCommandLine(project: Project, jar: String): List<String> {
    val commandBuilder = mutableListOf<String>()
    commandBuilder.add(javaBin.resolve("jlink").toString())

    commandBuilder.add("--add-modules")
    if (modules.isEmpty()) {
        // No user-defined modules, run jdeps and use the modules it finds
        commandBuilder.add(jdeps(project, jar).joinToString(separator = ","))
    } else {
        // Only use the user-specified modules
        commandBuilder.add(modules.joinToString(separator = ","))
    }

    if (modulePath.isNotEmpty()) {
        commandBuilder.add("--module-path")
        commandBuilder.add(modulePath)
    }

    if (bindServices) {
        commandBuilder.add("--bind-services")
    }

    commandBuilder.add("--compress=${compressionLevel.ordinal}")

    if (endianness != JLinkOptions.Endianness.SYSTEM_DEFAULT) {
        commandBuilder.add("--endian")
        commandBuilder.add(endianness.name.toLowerCase())
    }

    if (ignoreSigningInformation) {
        commandBuilder.add("--ignore-signing-information")
    }

    if (excludeHeaderFiles) {
        commandBuilder.add("--no-header-files")
    }

    if (excludeManPages) {
        commandBuilder.add("--no-man-pages")
    }

    if (stripDebug) {
        commandBuilder.add("--strip-debug")
    }

    if (optimizeClassForName) {
        commandBuilder.add("--class-for-name")
    }

    commandBuilder.add("--output")
    commandBuilder.add(output.toString())

    return commandBuilder
}

private fun JLinkOptions.execJLink(project: Project, jar: String) {
    project.exec {
        commandLine = buildCommandLine(project, jar)
    }
}
