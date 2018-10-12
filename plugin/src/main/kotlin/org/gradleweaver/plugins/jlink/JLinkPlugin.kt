package org.gradleweaver.plugins.jlink

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.create

open class JLinkPlugin : Plugin<Project> {

    companion object {
        const val EXTENSION_NAME = "jlink"
        const val JLINK_TASK_GROUP = "JLink"
        const val JLINK_TASK_NAME = "jlinkGenerate"
        const val JLINK_ZIP_TASK_NAME = "jlinkZip"

        private fun capitalizeAndJoinWords(words: String): String {
            return words.split(' ').joinToString(separator = "") { it.capitalize() }
        }

        fun generateJLinkTaskName(configurationName: String): String {
            return "$JLINK_TASK_NAME${capitalizeAndJoinWords(configurationName)}"
        }

        fun generateJLinkZipTaskName(configurationName: String): String {
            return "$JLINK_ZIP_TASK_NAME${capitalizeAndJoinWords(configurationName)}"
        }
    }

    override fun apply(project: Project) {
        val extension = project.extensions.create(EXTENSION_NAME, JLinkExtension::class, project)

        extension.configure.all {
            generateTasks(this, project)
        }
    }

    private fun generateTasks(options: JLinkOptions, project: Project) {
        val jlinkTask = project.tasks.register(generateJLinkTaskName(options.name), JLinkTask::class.java, options).configure {
            group = JLINK_TASK_GROUP
            description = "Generates a native Java runtime image."
        }
        project.tasks.register(generateJLinkZipTaskName(options.name), Zip::class.java) {
            group = JLINK_TASK_GROUP
            description = "Generates a ZIP file of a native Java runtime image."
            from(jlinkTask)
        }
    }

}
