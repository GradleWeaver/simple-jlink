package org.gradleweaver.plugins.jlink

import org.gradle.api.Action
import org.gradle.api.Project

class JLinkOptions(private val project: Project, val name: String) {

    private val objectFactory = project.objects

    /**
     * Extra modules to add to the image that cannot be automatically detected by jdeps, such as those referenced
     * reflectively (e.g. using `Class.forName`). Typically, only modules from the JDK need to be explicitly specified;
     * modules from library dependencies are always added regardless of what jdeps detects.
     */
    val extraModules = objectFactory.listProperty<String>()

    /**
     * Link service provider modules and their dependencies.
     */
    val bindServices = objectFactory.property(false)

    /**
     * Enable compression of resources.
     */
    val compressionLevel = objectFactory.property(CompressionLevel.NONE)

    /**
     * Specifies the byte order of the generated image. The default value is the format of your system's architecture.
     */
    val endianness = objectFactory.property(Endianness.SYSTEM_DEFAULT)

    /**
     * Suppresses a fatal error when signed modular JARs are linked in the runtime image.
     * The signature-related files of the signed modular JARs are not copied to the runtime image.
     */
    val ignoreSigningInformation = objectFactory.property(false)

    /**
     * Excludes header files from the generated image.
     */
    val excludeHeaderFiles = objectFactory.property(false)

    /**
     * Excludes man pages from the generated image.
     */
    val excludeManPages = objectFactory.property(false)

    /**
     * Strips debug symbols from the generated image.
     */
    val stripDebug = objectFactory.property(false)

    /**
     * Optimize `Class.forName` calls to constant class loads.
     */
    val optimizeClassForName = objectFactory.property(false)

    /**
     * VM options for the launcher script. Set via the DSL method [launcher].
     */
    internal val launcherVmOptions = objectFactory.listProperty<String>()

    /**
     * The name of the launcher script. Set via the DSL method [launcher].
     */
    internal val launcherName = objectFactory.property<String>()

    /**
     * Configures the options to minimize the size of generated runtime images.
     */
    fun useMinimalImage() {
        compressionLevel.set(CompressionLevel.ZIP)
        excludeHeaderFiles.set(true)
        excludeManPages.set(true)
        stripDebug.set(true)
    }

    /**
     * Configures the application launch script.
     */
    fun launcher(launcherConfigurationAction: Action<in JLinkLauncherOptions>) {
        val tmp = JLinkLauncherOptions(project)
        launcherConfigurationAction.execute(tmp)
        launcherVmOptions.set(tmp.vmOptions)
        launcherName.set(tmp.launcherName)
    }

}
