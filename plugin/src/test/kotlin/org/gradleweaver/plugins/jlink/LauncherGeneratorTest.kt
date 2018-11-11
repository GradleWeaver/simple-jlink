package org.gradleweaver.plugins.jlink

import org.gradle.internal.os.OperatingSystem
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows

class LauncherGeneratorTest {

    private val generator: LauncherGenerator = LauncherGenerator()

    @Test
    fun testLinuxJar() {
        val vmOptions = "-Xmx16g -Xms512m"
        val jarName = "my-app.jar"
        val scriptText = generator.generateJarScript(os = OperatingSystem.LINUX, vmOptions = vmOptions, jarName = jarName)

        assertAll({
            assertTrue(scriptText.contains("JLINK_VM_OPTIONS=\"$vmOptions\""), "Script text had incorrect VM options")
        }, { assertTrue(scriptText.contains("-jar \$DIR/$jarName"), "Script does not launch jar") })
    }

    @Test
    fun testLinuxModule() {
        val scriptText = generator.generateModuleLaunchScript(os = OperatingSystem.LINUX, moduleName = "mod", mainClassName = "main")
        println(scriptText)
    }

    @Test
    fun testLinuxModuleNoModuleName() {
        assertThrows<IllegalArgumentException> {
            generator.generateModuleLaunchScript(os = OperatingSystem.LINUX, moduleName = "", mainClassName = "x")
        }
    }

    @Test
    fun testLinuxModuleNoMainClass() {
        assertThrows<IllegalArgumentException> {
            generator.generateModuleLaunchScript(os = OperatingSystem.LINUX, moduleName = "x", mainClassName = "")
        }
    }
}
