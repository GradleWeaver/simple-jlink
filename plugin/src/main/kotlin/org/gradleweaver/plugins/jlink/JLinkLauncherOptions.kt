package org.gradleweaver.plugins.jlink

import org.gradle.api.Project
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

// Need to implement Serializable to keep Gradle's task caching mechanism happy
class JLinkLauncherOptions(project: Project) : Serializable {

    companion object {
        private const val serialVersionUID: Long = 0xDEAD_BEEF_CAFE
    }

    private val objectFactory = project.objects

    /**
     * Runtime options for the JVM.
     */
    val vmOptions = objectFactory.listProperty<String>()

    /**
     * The name of the generated launcher script. If not set, the launcher name will be the name of the
     * corresponding project.
     */
    val launcherName = objectFactory.property(project.name)

    // For Groovy DSL support
    fun setLauncherName(launcherName: String) {
        this.launcherName.set(launcherName)
    }

    fun setVmOptions(options: Iterable<String>) {
        vmOptions.set(options.toList())
    }

    private fun writeObject(out: ObjectOutputStream) {
        out.putFields().put("vmOptions", vmOptions.get())
        out.putFields().put("launcherName", launcherName.get())
    }

    private fun readObject(input: ObjectInputStream) {
        val fields = input.readFields()
        vmOptions.set(fields["vmOptions", listOf<String>()] as List<String>)
        launcherName.set(fields["launcherName", null] as String?)
    }

}
