package org.gradleweaver.plugins.jlink

/**
 * Options for the levels of compression available for generated runtime images.
 *
 * @param jlinkValue the integer value corresponding to the compression level expected by `jlink`
 */
enum class CompressionLevel(val jlinkValue: Int) {
    /**
     * Do no compression on the generated image.
     */
    NONE(0),

    /**
     * Share constant string objects.
     */
    CONSTANT_STRING_SHARING(1),

    /**
     * ZIP compression on the generated image.
     */
    ZIP(2)
}
