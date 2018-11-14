package org.gradleweaver.plugins.jlink

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Compression
import org.gradle.api.tasks.bundling.Tar
import org.gradle.api.tasks.bundling.Zip
import org.gradle.internal.os.OperatingSystem
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

        fun generateJLinkTaskName(configurationName: String): String {
            return "$JLINK_TASK_NAME${capitalizeAndJoinWords(configurationName)}"
        }

        fun generateJLinkArchiveTaskName(configurationName: String): String {
            return "$JLINK_ARCHIVE_TASK_NAME${capitalizeAndJoinWords(configurationName)}"
        }
    }

    override fun apply(project: Project) {
        val extension = project.extensions.create(EXTENSION_NAME, JLinkExtension::class, project)

        extension.configure.all {
            generateTasks(this, project)
        }
    }

    private fun generateTasks(options: JLinkOptions, project: Project) {
        val jlinkTaskName = generateJLinkTaskName(options.name)
        val archiveTaskName = generateJLinkArchiveTaskName(options.name)
        project.tasks.register(jlinkTaskName, JLinkTask::class.java).configure {
            group = JLINK_TASK_GROUP
            description = "Generates a native Java runtime image for '${options.name}'."
            copyFromOptions(options)
        }
        if (OperatingSystem.current().isWindows) {
            project.tasks.register(archiveTaskName, Zip::class.java) {
                group = JLINK_TASK_GROUP
                description = "Generates a .zip archive file of a native Java runtime image for '${options.name}'."
                from(project.tasks.getByName(jlinkTaskName).outputs)
            }
        } else if (OperatingSystem.current().isUnix) {
            project.tasks.register(archiveTaskName, Tar::class.java) {
                group = JLINK_TASK_GROUP
                description = "Generates a .tar.gz archive file of a native Java runtime image for '${options.name}'."
                from(project.tasks.getByName(jlinkTaskName).outputs)
                compression = Compression.GZIP
                extension = "tar.gz"
            }
        }
    }

    private fun JLinkTask.copyFromOptions(options: JLinkOptions) {
        with(project) {
            imageName.set(provider { options.name })
            bindServices.set(provider { options.bindServices })
            compressionLevel.set(provider { options.compressionLevel })
            endianness.set(provider { options.endianness })
            ignoreSigningInformation.set(provider { options.ignoreSigningInformation })
            excludeHeaderFiles.set(provider { options.excludeHeaderFiles })
            excludeManPages.set(provider { options.excludeManPages })
            stripDebug.set(provider { options.stripDebug })
            optimizeClassForName.set(provider { options.optimizeClassForName })
            extraModules.set(provider { options.extraModules })
            launcherOptions.set(provider { options.launcherOptions })

            // Some workarounds to allow the options to have the JAR file and jlink dir specified as File objects
            // instead of Gradle Property objects
            applicationJarLocation.set(layout.file(provider { options.applicationJar?.relativeTo(projectDir) }))
            jlinkDir.set(layout.projectDirectory.dir(provider { options.jlinkDir?.relativeTo(projectDir)?.path }))
        }
    }

}
