package org.gradleweaver.plugins.jlink

/**
 * Options for the levels of compression available for generated runtime images.
 */
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
