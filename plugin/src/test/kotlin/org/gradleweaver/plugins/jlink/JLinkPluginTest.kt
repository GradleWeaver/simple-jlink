package org.gradleweaver.plugins.jlink

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class JLinkPluginTest: AbstractPluginTest() {
    @Test
    fun `can access the jlink extension`() {
        projectRoot.apply {
            buildKotlinFile().writeText(
                    """
                ${buildscriptBlockWithUnderTestPlugin()}

                ${pluginsBlockWithKotlinJvmPlugin()}

                apply(plugin = "org.gradleweaver.plugins.simple-jlink")

                // This extension should have been added by the accessor below.
                jlink {
                    assert(this is ${JLinkExtension::class.qualifiedName})
                    configure {
                        assert(this is ${NamedDomainObjectCollection::class.qualifiedName}<*>)
                    }
                }

                ${kotlinExtensionAccessor()}
                """.trimIndent()
            )
        }
        build("tasks").apply {
            assertEquals(TaskOutcome.SUCCESS, task(":tasks")!!.outcome)
        }
    }

    @Test
    fun `jlink execution`() {
        projectRoot.apply {
            buildKotlinFile().writeText(
                    """
                import org.gradleweaver.plugins.jlink.*

                ${buildscriptBlockWithUnderTestPlugin()}

                ${pluginsBlockWithKotlinJvmPlugin()}

                apply(plugin = "org.gradleweaver.plugins.simple-jlink")

                jlink {
                    "example" {
                        useMinimalImage()
                        applicationJar = file("test.jar")
                        modules = listOf("java.base") // To avoid calling jdeps on an empty file
                    }
                }

                ${kotlinExtensionAccessor()}
                    """.trimIndent()
            )

            resolve("test.jar").createNewFile()

            val taskName = JLinkPlugin.generateJLinkTaskName("example")

            build(taskName).apply {
                assertEquals(TaskOutcome.SUCCESS, task(":$taskName")!!.outcome)
                // Check that the jlink image was generated and contains the jar file
                val jlinkDir = resolve("build/jlink")
                assertTrue(jlinkDir.exists(), "jlink directory was not created")
                assertTrue(jlinkDir.resolve("bin").exists(), "jlink bin directory was not created")
                assertTrue(jlinkDir.resolve("bin/java").isFile, "The Java executable was not created")
                assertTrue(jlinkDir.resolve("bin/plugin-test.jar").isFile, "The application JAR was not copied")

                // Check that the specified modules were added to the image
                // We can't check the modules themselves - they're all globbed together in a single binary blob
                // But we can check the presence of their legal notices
                assertTrue(jlinkDir.resolve("legal/java.base").exists(), "The java.base module was not linked")

                // There's a bunch more files that get generated by jlink, but if the `java` executable is present
                // then we can be reasonably certain that the rest of the jlink image is also present
            }

            // No changes were made to the jlink configuration or its input files
            // Make sure the task doesn't run unnecessarily
            build (taskName).apply {
                assertEquals(TaskOutcome.UP_TO_DATE, task(":$taskName")!!.outcome,
                        "The custom jlink task was not updated and should not have run")
            }
        }
    }

    @ParameterizedTest
    @CsvSource(
            "simple,jlinkGenerateSimple,jlinkZipSimple",
            "Capitalized,jlinkGenerateCapitalized,jlinkZipCapitalized",
            "spaces in the name,jlinkGenerateSpacesInTheName,jlinkZipSpacesInTheName"
    )
    fun `generated task names are correct`(configName: String, expectedJLinkTaskName: String, expectedJLinkZipTaskName: String) {
        assertAll(configName,
                Executable { assertEquals(expectedJLinkTaskName, JLinkPlugin.generateJLinkTaskName(configName)) },
                Executable { assertEquals(expectedJLinkZipTaskName, JLinkPlugin.generateJLinkZipTaskName(configName)) }
        )
    }

}