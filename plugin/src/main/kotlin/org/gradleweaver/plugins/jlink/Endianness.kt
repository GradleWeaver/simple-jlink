package org.gradleweaver.plugins.jlink

/**
 * Options for the available byte orders of generated runtime images.
 */
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
