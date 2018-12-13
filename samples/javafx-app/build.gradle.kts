import org.gradleweaver.plugins.jlink.JLinkTask
import org.gradle.internal.os.OperatingSystem

plugins {
    id ("com.zyxist.chainsaw") version "0.3.1"
}

repositories {
    mavenCentral()
}

dependencies {
    compile(javafx("javafx-base"))
    compile(javafx("javafx-controls"))
    compile(javafx("javafx-fxml"))
    compile(javafx("javafx-graphics"))
}

/**
 * Creates a JavaFX dependency for the current platform.  Ideally this would be defined in its own Gradle plugin,
 * but for this sample project we'll just define it here.
 *
 * @param name    the name of the JavaFX artifact, e.g. `javafx-base`, `javafx-controls`
 * @param version the version of the artifact to depend on. Defaults to `11`
 */
fun DependencyHandler.javafx(name: String, version: String = "11"): Dependency {
    val classifier = when (OperatingSystem.current()) {
        OperatingSystem.WINDOWS -> "win"
        OperatingSystem.MAC_OS -> "mac"
        OperatingSystem.LINUX -> "linux"
        else -> throw UnsupportedOperationException("Unsupported OS: ${OperatingSystem.current()}")
    }
    return create("org.openjfx:$name:$version:$classifier")
}

jlink {
    "release image" {
        useMinimalImage()
        launcher {
            vmOptions.addAll(application.applicationDefaultJvmArgs)
            vmOptions.addAll("-Xms512M", "-Xmx2G")
            launcherName.set("SampleJavaFxApp")
        }
    }
}

application {
    mainClassName = "org.gradleweaver.plugin.sample.jlink.javafx.Main"
}
