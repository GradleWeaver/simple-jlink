import org.apache.tools.ant.taskdefs.optional.jlink.JlinkTask
import org.gradleweaver.plugins.jlink.JLinkTask

plugins {
    `application`
}

jlink {
    "release image" {
        useMinimalImage()
        applicationJar = tasks.getByName<Jar>("jar").archivePath
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
