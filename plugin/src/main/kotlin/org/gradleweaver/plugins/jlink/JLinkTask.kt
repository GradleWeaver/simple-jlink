package org.gradleweaver.plugins.jlink

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.module.ModuleFinder
import java.lang.module.ModuleReference
import java.nio.charset.Charset
import java.nio.file.Paths

// Note: must be open so Gradle can create a proxy subclass
open class JLinkTask : DefaultTask() {

    private val objectFactory = project.objects
    private val launcherGenerator = LauncherGenerator()

    /**
     * The name of the generated image.
     */
    @get:Input
    val imageName = objectFactory.property<String>()

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

    /**
     * Options for generated launcher scripts. This is an optional property, and if it is not set, then no launcher
     * script will be generated.
     */
    @get:Input
    @get:Optional
    val launcherOptions = objectFactory.property<JLinkLauncherOptions>()

    @get:Input
    val extraModules = objectFactory.listProperty<String>()

    @get:InputFile
    val applicationJarLocation: RegularFileProperty = newInputFile()

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
                into("${getJlinkTargetDir()}/bin")
            }
        }

        generateLauncherScript()
    }

    private fun generateLauncherScript() {
        val launcherOptions = this.launcherOptions.orNull
        if (launcherOptions == null) {
            return
        }

        val os = OperatingSystem.current()
        val vmOpts = launcherOptions.getVmOptions().joinToString(separator = " ")

        val scriptText = if (applicationJarLocation.isPresent) {
            launcherGenerator.generateJarScript(os, vmOpts, applicationJarLocation.get().asFile.name)
        } else {
            if (launcherOptions.applicationModuleName.isNotPresent()) {
                throw IllegalStateException("Application module must be specified")
            }
            if (launcherOptions.mainClassName.isNotPresent()) {
                throw IllegalStateException("Application main class must be specified")
            }
            launcherGenerator.generateModuleLaunchScript(os, vmOpts, launcherOptions.getApplicationModuleName(), launcherOptions.getMainClassName())
        }

        launcherGenerator.generateScriptFile(os, scriptText, getJlinkTargetDir().resolve("bin"), launcherOptions.getLauncherName())
    }

    private fun getJlinkTargetDir(): File {
        return project.buildDir.resolve("jlink").resolve(imageName.get())
    }

    private fun buildCommandLine(project: Project): List<String> {
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
        // TODO: Add application jar, if it is set
        val files = project.configurations.getByName("runtime").toList()
        if (files.isNotEmpty()) {
            commandBuilder.add("--module-path")
            commandBuilder.add(dependencyModules.asPath())
        }

        if (bindServices.get()) {
            commandBuilder.add("--bind-services")
        }

        commandBuilder.add("--compress=${compressionLevel.get().jlinkValue}")

        if (endianness.getOrElse(Endianness.SYSTEM_DEFAULT) != Endianness.SYSTEM_DEFAULT) {
            commandBuilder.add("--endian")
            commandBuilder.add(endianness.get().name.toLowerCase())
        }

        if (ignoreSigningInformation.get()) {
            commandBuilder.add("--ignore-signing-information")
        }

        if (excludeHeaderFiles.get()) {
            commandBuilder.add("--no-header-files")
        }

        if (excludeManPages.get()) {
            commandBuilder.add("--no-man-pages")
        }

        if (stripDebug.get()) {
            commandBuilder.add("--strip-debug")
        }

        if (optimizeClassForName.get()) {
            commandBuilder.add("--class-for-name")
        }

        commandBuilder.add("--output")
        commandBuilder.add(getJlinkTargetDir().absolutePath)

        return commandBuilder
    }

    private fun execJLink(project: Project) {
        logger.debug("Deleting jlink build directory")
        project.delete(getJlinkTargetDir())
        project.exec {
            commandLine = buildCommandLine(project)
        }
    }

    internal fun copyFromOptions(options: JLinkOptions) {
        imageName.set(options.name)
        bindServices.set(options.bindServices)
        compressionLevel.set(options.compressionLevel)
        endianness.set(options.endianness)
        ignoreSigningInformation.set(options.ignoreSigningInformation)
        excludeHeaderFiles.set(options.excludeHeaderFiles)
        excludeManPages.set(options.excludeManPages)
        stripDebug.set(options.stripDebug)
        optimizeClassForName.set(options.optimizeClassForName)
        extraModules.set(options.extraModules)
        launcherOptions.set(options.launcherOptions)

        // Workaround to bind our RegularFileProperty to the Property<File> used by JLinkOptions
        applicationJarLocation.set(project.layout.file(options.applicationJar))
    }

}

private val javaBin by lazy {
    File(System.getProperty("java.home")).resolve("bin")
}

/**
 * Runs `jdeps` on the specified JAR file to determine its module dependencies.
 *
 * @param jar the path to the JAR file on which to run `jdeps`
 */
private fun jdeps(project: Project, jar: String): List<String> {
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
