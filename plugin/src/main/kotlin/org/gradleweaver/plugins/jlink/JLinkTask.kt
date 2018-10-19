package org.gradleweaver.plugins.jlink

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.jvm.Jvm
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property
import java.io.ByteArrayOutputStream
import java.lang.module.ModuleFinder
import java.lang.module.ModuleReference
import java.nio.charset.Charset
import java.nio.file.Paths

// Note: must be open so Gradle can create a proxy subclass
open class JLinkTask : DefaultTask() {

    private val objectFactory = project.objects

    /**
     * Link service provider modules and their dependencies.
     */
    @get:Input
    val bindServices = objectFactory.property<Boolean>()

    /**
     * Enable compression of resources.
     */
    @get:Input
    val compressionLevel = objectFactory.property<CompressionLevel>()

    /**
     * Specifies the byte order of the generated image. The default value is the format of your system's architecture.
     */
    @get:Input
    val endianness = objectFactory.property<Endianness>()

    /**
     * Suppresses a fatal error when signed modular JARs are linked in the runtime image.
     * The signature-related files of the signed modular JARs are not copied to the runtime image.
     */
    @get:Input
    val ignoreSigningInformation = objectFactory.property<Boolean>()

    /**
     * Excludes header files from the generated image.
     */
    @get:Input
    val excludeHeaderFiles = objectFactory.property<Boolean>()

    /**
     * Excludes man pages from the generated image.
     */
    @get:Input
    val excludeManPages = objectFactory.property<Boolean>()

    /**
     * Strips debug symbols from the generated image.
     */
    @get:Input
    val stripDebug = objectFactory.property<Boolean>()

    /**
     * Optimize `Class.forName` calls to constant class loads.
     */
    @get:Input
    val optimizeClassForName = objectFactory.property<Boolean>()

    @get:Input
    val extraModules = objectFactory.listProperty<String>()

    @get:InputFile
    val applicationJarLocation: RegularFileProperty = newInputFile()

    /**
     * The directory in which the jlink image should be build. By default, this is is `${project.buildDir}/jlink`.
     */
    @get:OutputDirectory
    val jlinkDir: DirectoryProperty = newOutputDirectory()

    init {
        jlinkDir.set(project.buildDir.resolve("jlink"))
    }

    @TaskAction
    fun executeJLink() {
        execJLink(project)

        // Copy the application JAR into the jlink bin directory, if specified
        if (applicationJarLocation.isPresent) {
            project.copy {
                from(applicationJarLocation)
                rename {
                    "${project.name.toLowerCase()}.jar"
                }
                into("${jlinkDir.get().asFile}/bin")
            }
        }
    }

    enum class CompressionLevel {
        /**
         * Do no compression on the generated image.
         */
        NONE,

        /**
         * Share constant string objects.
         */
        CONSTANT_STRING_SHARING,

        /**
         * ZIP compression on the generated image.
         */
        ZIP
    }

    enum class Endianness {
        /**
         * Use the endianness of the build system.
         */
        SYSTEM_DEFAULT,

        /**
         * Force little-endian byte order in the generated image.
         */
        LITTLE,

        /**
         * Force big-endian byte order in the generated image.
         */
        BIG
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
        // Get the standard library modules used by the project and its dependencies
        project.exec {
            val modulePath = getDependencyModules(project).asPath()
            val commandBuilder = mutableListOf(javaBin.resolve("jdeps").toString(), "--print-module-deps")
            if (modulePath.isNotEmpty()) {
                commandBuilder.add("--module-path")
                commandBuilder.add(modulePath)
            }
            commandBuilder.add(jar)
            commandLine = commandBuilder
            standardOutput = os
        }
        val out = os.toString(Charset.defaultCharset().name())
        out.split("\n")
                .takeWhile { it.isNotBlank() }
    }
}

private fun JLinkTask.buildCommandLine(project: Project): List<String> {
    val dependencyModules = getDependencyModules(project)
    val commandBuilder = mutableListOf<String>()
    commandBuilder.add(javaBin.resolve("jlink").toString())
    commandBuilder.add("--add-modules")
    commandBuilder.add(
            listOf(
                    jdeps(project, project.buildDir.resolve("classes").absolutePath).joinToString(separator = ","),
                    dependencyModules.joinToString(separator = ",") { it.descriptor().name() },
                    extraModules.getOrElse(listOf()).joinToString(separator = ",")
            ).joinToString(separator = ",")
    )

    // Use the automatic module path, determined from the project dependencies
    val files = project.configurations.getByName("runtime").toList()
    if (files.isNotEmpty()) {
        commandBuilder.add("--module-path")
        commandBuilder.add(dependencyModules.asPath())
    }

    if (bindServices.getOrElse(false)) {
        commandBuilder.add("--bind-services")
    }

    if (compressionLevel.isPresent) {
        commandBuilder.add("--compress=${compressionLevel.get().ordinal}")
    }

    if (endianness.getOrElse(JLinkTask.Endianness.SYSTEM_DEFAULT) != JLinkTask.Endianness.SYSTEM_DEFAULT) {
        commandBuilder.add("--endian")
        commandBuilder.add(endianness.get().name.toLowerCase())
    }

    if (ignoreSigningInformation.getOrElse(false)) {
        commandBuilder.add("--ignore-signing-information")
    }

    if (excludeHeaderFiles.getOrElse(false)) {
        commandBuilder.add("--no-header-files")
    }

    if (excludeManPages.getOrElse(false)) {
        commandBuilder.add("--no-man-pages")
    }

    if (stripDebug.getOrElse(false)) {
        commandBuilder.add("--strip-debug")
    }

    if (optimizeClassForName.getOrElse(false)) {
        commandBuilder.add("--class-for-name")
    }

    commandBuilder.add("--output")
    commandBuilder.add(jlinkDir.get().asFile.absolutePath)

    return commandBuilder
}

internal fun JLinkTask.execJLink(project: Project) {
    logger.debug("Deleting jlink build directory")
    project.delete(jlinkDir.get())
    project.exec {
        commandLine = buildCommandLine(project)
    }
}

private fun Iterable<ModuleReference>.asPath() =
        joinToString(separator = System.getProperty("path.separator")) {
            it.location().get().toString()
        }

/**
 * Extracts data about all the modules required by a project's dependency libraries, including those libraries if they
 * are modularized.
 */
private fun getDependencyModules(project: Project): Collection<ModuleReference> {
    if (project.configurations.findByName("runtime") == null) {
        return listOf()
    }
    val dependencyFilesList = project.configurations.getByName("runtime")
            .map { it.absolutePath }
            .filter { it.endsWith(".jar") || it.endsWith(".jmod") }
            .map { it -> Paths.get(it) }
            .toTypedArray()
    val allModuleReferences = ModuleFinder.of(*dependencyFilesList).findAll()
    return allModuleReferences
            .filter { !it.descriptor().isAutomatic } // Can't add automatic modules to a jlink image
}
