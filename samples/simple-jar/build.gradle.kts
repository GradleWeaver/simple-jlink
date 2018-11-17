import org.apache.tools.ant.taskdefs.optional.jlink.JlinkTask
import org.gradleweaver.plugins.jlink.JLinkTask

jlink {
    "release image" {
        useMinimalImage()
    }
}

application {
    mainClassName = "org.gradleweaver.plugin.sample.jlink.SampleApp"
}
