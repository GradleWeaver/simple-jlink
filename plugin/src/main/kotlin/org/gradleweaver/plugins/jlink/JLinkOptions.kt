package org.gradleweaver.plugins.jlink

import org.gradleweaver.plugins.jlink.JLinkTask.CompressionLevel
import org.gradleweaver.plugins.jlink.JLinkTask.Endianness
import java.io.File

class JLinkOptions(val name: String) {

    /**
     * The application JAR. This will be copied into the generated jlink image's `bin` directory, next to the
     * `java` executable.
     */
    var applicationJar: File? = null

    /**
     * The directory into which the jlink image will be generated. This option is not required; if it is not set,
     * the jlink image will be generated in `${project.dir}/build/jlink`
     */
    var jlinkDir: File? = null

    /**
     * The modules to link. These MUST be on the module path or included in the JDK. If not set, `jdeps` will be run
     * on the [applicationJar] to automatically determine the modules used.
     */
    var modules: List<String> = listOf()

    /**
     * Link service provider modules and their dependencies.
     */
    var bindServices = false

    /**
     * Enable compression of resources.
     */
    var compressionLevel: CompressionLevel = CompressionLevel.NONE

    /**
     * Specifies the byte order of the generated image. The default value is the format of your system's architecture.
     */
    var endianness: Endianness = Endianness.SYSTEM_DEFAULT

    /**
     * Suppresses a fatal error when signed modular JARs are linked in the runtime image.
     * The signature-related files of the signed modular JARs are not copied to the runtime image.
     */
    var ignoreSigningInformation = false

    /**
     * Specifies the module path.
     */
    var modulePath = ""

    /**
     * Excludes header files from the generated image.
     */
    var excludeHeaderFiles = false

    /**
     * Excludes man pages from the generated image.
     */
    var excludeManPages = false

    /**
     * Strips debug symbols from the generated image.
     */
    var stripDebug = false

    /**
     * Optimize `Class.forName` calls to constant class loads.
     */
    var optimizeClassForName = false

    /**
     * Configures the options to minimize the size of generated runtime images.
     */
    fun useMinimalImage() {
        compressionLevel = CompressionLevel.ZIP
        excludeHeaderFiles = true
        excludeManPages = true
        stripDebug = true
    }
}
