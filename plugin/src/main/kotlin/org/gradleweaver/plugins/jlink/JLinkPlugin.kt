package org.gradleweaver.plugins.jlink

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Compression
import org.gradle.api.tasks.bundling.Tar
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.create

open class JLinkPlugin : Plugin<Project> {

    companion object {
        const val EXTENSION_NAME = "jlink"
        const val JLINK_TASK_GROUP = "JLink"
        const val JLINK_TASK_NAME = "jlinkGenerate"
        const val JLINK_ARCHIVE_TASK_NAME = "jlinkArchive"

        private fun capitalizeAndJoinWords(words: String): String {
            return words.split(' ').joinToString(separator = "") { it.capitalize() }
        }

        @JvmStatic
        fun generateJLinkTaskName(configurationName: String): String {
            return "$JLINK_TASK_NAME${capitalizeAndJoinWords(configurationName)}"
        }

        @JvmStatic
        fun generateJLinkArchiveTaskName(type: String, configurationName: String): String {
            return "$JLINK_ARCHIVE_TASK_NAME$type${capitalizeAndJoinWords(configurationName)}"
        }
    }

    override fun apply(project: Project) {
        val extension = project.extensions.create(EXTENSION_NAME, JLinkExtension::class, project)

        extension.jlinkConfigurations.all {
            generateTasks(this, project)
        }
    }

    private fun generateTasks(options: JLinkOptions, project: Project) {
        val jlinkTaskName = generateJLinkTaskName(options.name)
        project.tasks.register(jlinkTaskName, JLinkTask::class.java).configure {
            group = JLINK_TASK_GROUP
            description = "Generates a native Java runtime image for '${options.name}'."
            copyFromOptions(options)
        }
        project.tasks.register(generateJLinkArchiveTaskName("Zip", options.name), Zip::class.java) {
            group = JLINK_TASK_GROUP
            description = "Generates a .zip archive file of a native Java runtime image for '${options.name}'."
            from(project.tasks.getByName(jlinkTaskName).outputs)
        }
        project.tasks.register(generateJLinkArchiveTaskName("Tar", options.name), Tar::class.java) {
            group = JLINK_TASK_GROUP
            description = "Generates a .tar.gz archive file of a native Java runtime image for '${options.name}'."
            from(project.tasks.getByName(jlinkTaskName).outputs)
            compression = Compression.GZIP
            extension = "tar.gz"
        }
    }

}
