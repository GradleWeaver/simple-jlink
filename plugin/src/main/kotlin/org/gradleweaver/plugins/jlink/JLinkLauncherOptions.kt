package org.gradleweaver.plugins.jlink

class JLinkLauncherOptions {

    /**
     * Runtime options for the JVM.
     */
    var vmOptions = mutableListOf<String>()

    /**
     * The name of the generated launcher script. If not set, the launcher name will be the name of the
     * corresponding project.
     */
    var launcherName: String? = null

    /**
     * The name of the application module. Only needs to be set if the application is modular _and_ all dependencies
     * are modular.
     */
    var applicationModuleName: String? = null

    /**
     * The fully qualified name of the main application class. Only needs to be set if the application is modular
     * _and_ all dependencies are modular.
     */
    var mainClassName: String? = null

    fun setVmOptions(options: Iterable<String>) {
        vmOptions = options.toMutableList()
    }

    /**
     * Adds VM options to the launch script.
     */
    fun vmOptions(vararg options: String) {
        vmOptions.addAll(options)
    }

    /**
     * Adds VM options to the launch script.
     */
    fun vmOptions(options: Collection<String>) {
        vmOptions.addAll(options)
    }

}