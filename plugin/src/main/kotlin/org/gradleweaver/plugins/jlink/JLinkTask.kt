package org.gradleweaver.plugins.jlink

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.jvm.Jvm
import org.gradle.kotlin.dsl.property
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import javax.inject.Inject

// Note: must be open so Gradle can create a proxy subclass
open class JLinkTask @Inject constructor(val options: JLinkOptions) : DefaultTask() {

    private val objectFactory = project.objects
    
    /**
     * The modules to link. These MUST be on the module path or included in the JDK. If not set, `jdeps` will be run
     * on the output JAR file from [shadowTask] to automatically determine the modules used.
     */
    @get:Input
    val modules: ListProperty<String> = objectFactory.listProperty(String::class.java)

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
    val ignoreSigningInformation  = objectFactory.property<Boolean>()

    /**
     * Specifies the module path.
     */
    @get:Input
    val modulePath = objectFactory.property<String>()

    /**
     * Excludes header files from the generated image.
     */
    @get:Input
    val excludeHeaderFiles  = objectFactory.property<Boolean>()

    /**
     * Excludes man pages from the generated image.
     */
    @get:Input
    val excludeManPages  = objectFactory.property<Boolean>()

    /**
     * Strips debug symbols from the generated image.
     */
    @get:Input
    val stripDebug  = objectFactory.property<Boolean>()

    /**
     * Optimize `Class.forName` calls to constant class loads.
     */
    @get:Input
    val optimizeClassForName  = objectFactory.property<Boolean>()

    @get:InputFile
    val applicationJarLocation: RegularFileProperty = newInputFile()

    /**
     * The directory in which the jlink image should be build. By default, this is is `${project.buildDir}/jlink`.
     */
    @get:OutputDirectory
    val jlinkDir: DirectoryProperty = newOutputDirectory()

    init {
        copyFromOptions(options)
        jlinkDir.set(project.buildDir.resolve("jlink"))
    }

    private fun copyFromOptions(options: JLinkOptions) {
        this.modules.set(options.modules)
        this.bindServices.set(options.bindServices)
        this.compressionLevel.set(options.compressionLevel)
        this.endianness.set(options.endianness)
        this.ignoreSigningInformation.set(options.ignoreSigningInformation)
        this.modulePath.set(options.modulePath)
        this.excludeHeaderFiles.set(options.excludeHeaderFiles)
        this.excludeManPages.set(options.excludeManPages)
        this.stripDebug.set(options.stripDebug)
        this.optimizeClassForName.set(options.optimizeClassForName)
        if (options.applicationJar != null) {
            this.applicationJarLocation.set(options.applicationJar!!)
        }
        if (options.jlinkDir != null) {
            this.jlinkDir.set(options.jlinkDir!!)
        }
    }

    @TaskAction
    fun executeJLink() {
        copyFromOptions(options)
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
    if (!modules.isPresent) {
        // No user-defined modules, run jdeps and use the modules it finds
        commandBuilder.add(jdeps(project, jar).joinToString(separator = ","))
    } else {
        // Only use the user-specified modules
        commandBuilder.add(modules.get().joinToString(separator = ","))
    }

    if (modulePath.getOrElse("").isNotEmpty()) {
        commandBuilder.add("--module-path")
        commandBuilder.add(modulePath.get())
    }

    if (bindServices.getOrElse(false)) {
        commandBuilder.add("--bind-services")
    }

    commandBuilder.add("--compress=${compressionLevel.get().ordinal}")

    if (endianness.get() != JLinkTask.Endianness.SYSTEM_DEFAULT) {
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

internal fun JLinkTask.execJLink(project: Project, jar: String) {
    logger.debug("Deleting jlink build directory")
    project.delete(jlinkDir.get())
    project.exec {
        commandLine = buildCommandLine(project, jar)
    }
}
