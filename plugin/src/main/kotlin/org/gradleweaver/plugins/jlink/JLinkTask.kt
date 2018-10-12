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
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import javax.inject.Inject

// Note: must be open so Gradle can create a proxy subclass
open class JLinkTask @Inject constructor(val options: JLinkOptions) : DefaultTask() {

    /**
     * The modules to link. These MUST be on the module path or included in the JDK. If not set, `jdeps` will be run
     * on the output JAR file from [shadowTask] to automatically determine the modules used.
     */
    @Input
    var modules: List<String> = listOf()

    /**
     * Link service provider modules and their dependencies.
     */
    @Input
    var bindServices = false

    /**
     * Enable compression of resources.
     */
    @Input
    var compressionLevel: CompressionLevel = CompressionLevel.NONE

    /**
     * Specifies the byte order of the generated image. The default value is the format of your system's architecture.
     */
    @Input
    var endianness: Endianness = Endianness.SYSTEM_DEFAULT

    /**
     * Suppresses a fatal error when signed modular JARs are linked in the runtime image.
     * The signature-related files of the signed modular JARs are not copied to the runtime image.
     */
    @Input
    var ignoreSigningInformation = false

    /**
     * Specifies the module path.
     */
    @Input
    var modulePath = ""

    /**
     * Excludes header files from the generated image.
     */
    @Input
    var excludeHeaderFiles = false

    /**
     * Excludes man pages from the generated image.
     */
    @Input
    var excludeManPages = false

    /**
     * Strips debug symbols from the generated image.
     */
    @Input
    var stripDebug = false

    /**
     * Optimize `Class.forName` calls to constant class loads.
     */
    @Input
    var optimizeClassForName = false

    @InputFile
    var applicationJarLocation: RegularFileProperty = newInputFile()

    /**
     * The directory in which the jlink image should be build. By default, this is is `${project.buildDir}/jlink`.
     */
    @OutputDirectory
    var jlinkDir: DirectoryProperty = newOutputDirectory()

    init {
        copyFromOptions(options)
        jlinkDir.set(project.buildDir.resolve("jlink"))
    }

    private fun copyFromOptions(options: JLinkOptions) {
        this.modules = options.modules
        this.bindServices = options.bindServices
        this.compressionLevel = options.compressionLevel
        this.endianness = options.endianness
        this.ignoreSigningInformation = options.ignoreSigningInformation
        this.modulePath = options.modulePath
        this.excludeHeaderFiles = options.excludeHeaderFiles
        this.excludeManPages = options.excludeManPages
        this.stripDebug = options.stripDebug
        this.optimizeClassForName = options.optimizeClassForName
        if (options.applicationJar != null) {
            this.applicationJarLocation.set(options.applicationJar!!)
        }
        if (options.jlinkDir != null) {
            this.jlinkDir.set(options.jlinkDir!!)
        }
    }

    @TaskAction
    fun executeJLink() {
        execJLink(project, applicationJarLocation.get().asFile.absolutePath)

        // Copy the application JAR into the jlink bin directory
        project.copy {
            from(applicationJarLocation)
            rename {
                "${project.name.toLowerCase()}.jar"
            }
            into("${jlinkDir.get().asFile}/bin")
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

private fun JLinkTask.buildCommandLine(project: Project, jar: String): List<String> {
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

    if (endianness != JLinkTask.Endianness.SYSTEM_DEFAULT) {
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
    commandBuilder.add(jlinkDir.get().asFile.absolutePath)

    return commandBuilder
}

internal fun JLinkTask.execJLink(project: Project, jar: String) {
    logger.debug("Deleting jlink build directory")
    project.delete(jlinkDir.get())
    project.exec {
        commandLine = buildCommandLine(project, jar)
    }
}
