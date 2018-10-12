package org.gradleweaver.plugins.jlink

class JLinkOptions(val name: String) {

    /**
     * The modules to link. These MUST be on the module path or included in the JDK. If not set, `jdeps` will be run
     * on the output JAR file from [shadowTask] to automatically determine the modules used.
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
     * Specifies the location of the generated runtime image. By default, this is `${project.dir}/build/jlink`.
     */
    var output: Any = "build/jlink/"

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