import org.apache.tools.ant.taskdefs.optional.jlink.JlinkTask
import org.gradleweaver.plugins.jlink.JLinkTask

plugins {
  id ("com.zyxist.chainsaw") version "0.3.1"
}

jlink {
  "release image" {
    useMinimalImage()
  }
}

application {
  mainClassName = "org.gradleweaver.plugin.sample.jlink.ModularApp"
}
