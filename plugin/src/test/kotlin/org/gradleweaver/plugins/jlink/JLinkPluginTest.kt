package org.gradleweaver.plugins.jlink

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

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
}