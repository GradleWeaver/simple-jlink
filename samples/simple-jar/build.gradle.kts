import org.apache.tools.ant.taskdefs.optional.jlink.JlinkTask
import org.gradleweaver.plugins.jlink.JLinkTask

plugins {
    `application`
}

jlink {
    "release image" {
        applicationJar.set(provider { tasks.getByName<Jar>("jar").archivePath })
        useMinimalImage()
    }
}

tasks.withType<JLinkTask>().configureEach {
    if (name == "jlinkGenerateReleaseImage") {
        dependsOn("jar")
    }
}

application {
    mainClassName = "org.gradleweaver.plugin.sample.jlink.SampleApp"
}

tasks.withType<Jar>().configureEach {
    manifest {
        attributes("Main-Class" to application.mainClassName)
    }
}
