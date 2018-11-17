package org.gradleweaver.plugins.jlink

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.module.ModuleFinder
import java.lang.module.ModuleReference
import java.nio.charset.Charset
import java.nio.file.Path
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

    /**
     * The directory into which the jlink image is generated. This defaults to build/jlink/[imageName]
     */
    @get:OutputDirectory
    val jlinkDir = newOutputDirectory(project.layout.buildDirectory.dir(imageName.map { name -> "jlink/$name" }))

    private fun newOutputDirectory(provider: Provider<Directory>): DirectoryProperty {
        val dir = newOutputDirectory()
        dir.set(provider)
        return dir
    }

    /**
     * The location of the project jar file.
     */
    private val projectJar: File
        get() = project.tasks.getByName<Jar>("jar").archivePath

    /**
     * The project's module.
     */
    private val projectModule: ModuleReference
        get() = ModuleFinder.of(projectJar.toPath())
            .findAll()
            .first()

    @TaskAction
    fun executeJLink() {
        jlinkDir.set(getJlinkTargetDir())
        execJLink(project)

        // Copy the application JAR into the jlink bin directory if it is nonmodular
        if (projectModule.descriptor().isAutomatic) {
            project.copy {
                from(projectJar)
                rename {
                    "${project.name.toLowerCase()}.jar"
                }
                into("${getJlinkTargetDir()}/bin")
            }
        }

        generateLauncherScript()
    }

    @Suppress("USELESS_ELVIS")
    private fun generateLauncherScript() {
        if (launcherOptions.isNotPresent) {
            return
        }
        val launcherOptions = launcherOptions.get()

        val os = OperatingSystem.current()
        val vmOpts = launcherOptions.vmOptions.get().joinToString(separator = " ")

        val allDependenciesModular = allDependenciesModular()

        val applicationModule = projectModule
        val scriptText = if (allDependenciesModular && applicationModule.descriptor().isAutomatic) {
            // Application is not modular, generate a script to launch the jar file
            launcherGenerator.generateJarScript(os, vmOpts, projectJar.name)
        } else {
            // Application _is_ modular
            val mod = applicationModule.descriptor()
            if (mod.isAutomatic) {
                throw IllegalStateException("The application is not modular!")
            }
            val moduleName = mod.name()
            val mainClassName = project.extensions.getByType(JavaApplication::class.java).mainClassName
                ?: throw IllegalStateException("No main class name specified in the application plugin")
            launcherGenerator.generateModuleLaunchScript(os, vmOpts, moduleName, mainClassName)
        }

        launcherGenerator.generateScriptFile(os, scriptText, getJlinkTargetDir().resolve("bin"), launcherOptions.launcherName.get())
    }

    private fun allDependenciesModular(): Boolean {
        val dependencyFiles = project.configurations.getByName("runtime").files.map { it.toPath() }
        return ModuleFinder.of(*dependencyFiles.toTypedArray())
            .findAll()
            .size == dependencyFiles.size
    }

    private fun getJlinkTargetDir(): File {
        return project.buildDir.resolve("jlink").resolve(imageName.get())
    }

    private fun buildCommandLine(project: Project): List<String> {
        val dependencyModules = getDependencyModules(project)
        val commandBuilder = mutableListOf<String>()
        commandBuilder.add(javaBin.resolve("jlink").toString())
        commandBuilder.add("--add-modules")
        val applicationModuleName = if (!projectModule.descriptor().isAutomatic) {
            projectModule.descriptor().name()
        } else {
            ""
        }
        commandBuilder.add(
                listOf(
                        jdeps(project, project.buildDir.resolve("classes").absolutePath).joinToString(separator = ","),
                        dependencyModules.joinToString(separator = ",") { it.descriptor().name() },
                        extraModules.getOrElse(listOf()).joinToString(separator = ","),
                        applicationModuleName
                ).joinToString(separator = ",").replace(Regex("""(,+)|(,$)"""), ",")
        )

        // Use the automatic module path, determined from the project dependencies, as well as the application JAR
        // (if it is modular)
        val files = project.configurations.getByName("runtime").toList()
        if (files.isNotEmpty() || !projectModule.descriptor().isAutomatic) {
            commandBuilder.add("--module-path")
            val path = mutableListOf<String>()
            if (files.isNotEmpty()) {
                path.add(dependencyModules.asPath())
            }
            if (!projectModule.descriptor().isAutomatic) {
                path.add(projectModule.location().get().path)
            }
            commandBuilder.add(path.joinToString(separator = System.getProperty("path.separator")))
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

private fun isModularJar(path: Path): Boolean {
    return ModuleFinder.of(path)
        .findAll()
        .stream()
        .map { !it.descriptor().isAutomatic }
        .findAny()
        .orElse(false)
}
