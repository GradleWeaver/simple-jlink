package org.gradleweaver.plugins.jlink

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class JLinkPluginTest : AbstractPluginTest() {
    @Test
    fun `can access the jlink extension`() {
        projectRoot.apply {
            buildKotlinFile().writeText(
                    """
                ${pluginsBlock()}

                // This extension should have been added by the accessor below.
                jlink {
                    assert(this is ${JLinkExtension::class.qualifiedName})
                    jlinkConfigurations {
                        assert(this is ${NamedDomainObjectCollection::class.qualifiedName}<*>)
                    }
                }
                """.trimIndent()
            )
        }
        build("tasks").apply {
            assertEquals(TaskOutcome.SUCCESS, task(":tasks")!!.outcome)
        }
    }

    @ParameterizedTest
    @CsvSource(
            "simple,jlinkGenerateSimple,jlinkArchiveZipSimple",
            "Capitalized,jlinkGenerateCapitalized,jlinkArchiveZipCapitalized",
            "spaces in the name,jlinkGenerateSpacesInTheName,jlinkArchiveZipSpacesInTheName"
    )
    fun `generated task names are correct`(configName: String, expectedJLinkTaskName: String, expectedJLinkZipTaskName: String) {
        assertAll(configName,
                { assertEquals(expectedJLinkTaskName, JLinkPlugin.generateJLinkTaskName(configName)) },
                { assertEquals(expectedJLinkZipTaskName, JLinkPlugin.generateJLinkArchiveTaskName("Zip", configName)) }
        )
    }

}
