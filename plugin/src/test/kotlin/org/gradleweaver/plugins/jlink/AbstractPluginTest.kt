package org.gradleweaver.plugins.jlink

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.TextUtil.normaliseFileSeparators
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junitpioneer.jupiter.TempDirectory
import java.io.File
import java.nio.file.Path
import java.util.*

@ExtendWith(TempDirectory::class)
abstract class AbstractPluginTest {

    private lateinit var tempDir: Path

    @BeforeEach
    fun beforeEach(@TempDirectory.TempDir tempDir: Path) {
        this.tempDir = tempDir
    }

    val projectRoot: File
        get() = tempDir.toFile().resolve("plugin-test").apply { mkdirs() }

    protected fun pluginsBlock() =
        """
        plugins {
            id("org.gradleweaver.simple-jlink")
        }
        """.trimIndent()

    protected fun buildscriptBlockWithUnderTestPlugin() =
        """
        buildscript {
            repositories { maven { setUrl("$testRepositoryPath") } }
            dependencies {
                classpath("org.gradleweaver.plugins:simple-jlink:${testProperties["version"]}")
            }
        }
        """.trimIndent()

    protected
    fun pluginsBlockWithKotlinJvmPlugin() =
        """
        plugins {
            id("org.jetbrains.kotlin.jvm") version "${testProperties["kotlinVersion"]}"
        }
        """.trimIndent()

    protected
    fun kotlinExtensionAccessor() =
        """
        fun `${JLinkPlugin.EXTENSION_NAME}`(configure: ${JLinkExtension::class.qualifiedName}.() -> Unit): Unit =
            (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("${JLinkPlugin.EXTENSION_NAME}", configure)
        """.trimIndent()

    protected
    fun build(vararg arguments: String): BuildResult =
            gradleRunnerFor(*arguments).forwardOutput().build()

    protected
    fun buildAndFail(vararg arguments: String): BuildResult =
            gradleRunnerFor(*arguments).forwardOutput().buildAndFail()

    protected
    fun gradleRunnerFor(vararg arguments: String): GradleRunner =
            GradleRunner.create()
                    .withProjectDir(projectRoot)
                    .withArguments(arguments.toList())
                    .withPluginClasspath()

    private
    val testRepositoryPath
        get() = normaliseFileSeparators(File("build/plugin-test-repository").absolutePath)

    protected
    val testProperties: Properties by lazy {
        javaClass.getResourceAsStream("/test.properties").use {
            Properties().apply { load(it) }
        }
    }

    private
    fun File.createSourceFile(sourceFilePath: String, contents: String) {
        val sourceFile = resolve(sourceFilePath)
        sourceFile.parentFile.mkdirs()
        sourceFile.writeText(contents)
    }

    fun File.buildFile() = resolve("build.gradle")
    fun File.buildKotlinFile() = resolve("build.gradle.kts")
    fun File.settingsFile() = resolve("settings.gradle")
}
