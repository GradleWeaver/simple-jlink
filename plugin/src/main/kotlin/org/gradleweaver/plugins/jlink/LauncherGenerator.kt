package org.gradleweaver.plugins.jlink

import java.io.File
import java.net.URL

/**
 * Generates native launch scripts for the application using a jlink-generated runtime image.
 */
class LauncherGenerator {

    companion object {
        private const val VM_OPTIONS_KEY = "JLINK_VM_OPTIONS"
        private const val MODULE_NAME_KEY = "MODULE_NAME"
        private const val MAIN_CLASS_KEY = "MAIN_CLASS_NAME"
        private const val JAR_FILE_KEY = "JAR_NAME"
    }

    /**
     * Generates a script file for launching the application. The script file will be executable and can be called
     * from any directory.
     *
     * @param os           the operating system to generate a script for
     * @param scriptText   the contents of the script file
     * @param dir          the directory into which the script file should be generated
     * @param launcherName the name of the launcher script, without a file extension
     *
     * @return the generated script file
     */
    fun generateScriptFile(os: OperatingSystem, scriptText: String, dir: File, launcherName: String): File {
        val scriptFileName = os.getScriptName(launcherName)
        val scriptFile = dir.resolve(scriptFileName)
        scriptFile.writeText(scriptText)
        scriptFile.setExecutable(true, false)
        return scriptFile
    }

    /**
     * Generates the contents of a script file to launch an application from a JAR file.
     *
     * @param os        the operating system to generate a script for
     * @param vmOptions optional launch options for the virtual machine
     * @param jarName   the name of the JAR file, with extension, relative to the script file
     *
     * @return the contents of a platform-specific launch script for a JAR file
     */
    fun generateJarScript(os: OperatingSystem, vmOptions: String = "", jarName: String): String {
        if (jarName.isBlank()) {
            throw IllegalArgumentException("Jar name must be specified")
        }
        if (!jarName.endsWith(".jar")) {
            throw IllegalArgumentException("File name must end with '.jar'")
        }
        val scriptTemplate = resolveScriptFile(os, LaunchType.JAR).readText()
        return scriptTemplate.fillTemplate(mapOf(VM_OPTIONS_KEY to vmOptions, JAR_FILE_KEY to jarName))
    }

    /**
     * Generates the contents of a script file to launch a modular application with fully modular dependencies.
     *
     * @param os            the operating system to generate a script for
     * @param vmOptions     optional launch options for the virtual machine
     * @param moduleName    the name of the application module
     * @param mainClassName the fully qualified name of the application main class
     */
    fun generateModuleLaunchScript(os: OperatingSystem, vmOptions: String = "", moduleName: String, mainClassName: String): String {
        if (moduleName.isBlank()) {
            throw IllegalArgumentException("Module name must be specified")
        }
        if (mainClassName.isBlank()) {
            throw java.lang.IllegalArgumentException("Main class must be specified")
        }
        val scriptTemplate = resolveScriptFile(os, LaunchType.MODULE).readText()
        return scriptTemplate.fillTemplate(mapOf(VM_OPTIONS_KEY to vmOptions, MODULE_NAME_KEY to moduleName, MAIN_CLASS_KEY to mainClassName))
    }

    private fun resolveScriptFile(os: OperatingSystem, launchType: LaunchType): URL {
        val type = when (launchType) {
            LaunchType.JAR -> "jar"
            LaunchType.MODULE -> "module"
        }
        val s = "runscripts/" + when {
            os.isWindows -> "windows-$type-launch.bat"
            else -> "unix-$type-launch"
        }
        return javaClass.getResource(s)
    }

    private fun String.fillTemplate(values: Map<String, Any>): String {
        var target = this
        for (entry in values) {
            target = target.replace("{{${entry.key}}}", entry.value.toString())
        }
        return target
    }
}

enum class LaunchType {
    JAR,
    MODULE
}
