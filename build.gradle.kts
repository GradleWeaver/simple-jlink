buildscript {
    dependencies {
        classpath("org.gradleweaver.plugins:simple-jlink:+")
    }
}
plugins {
    `lifecycle-base`
}

allprojects {
    repositories {
        jcenter()
    }
}

project(":samples") {
    subprojects {
        apply(plugin = "org.gradleweaver.plugins.simple-jlink")
    }
}

tasks.named<Task>("check") {
    dependsOn(gradle.includedBuild("simple-jlink").task(":check"))
    dependsOn(findProject(":samples:javafx-app")!!.tasks.getByName("jlinkGenerateReleaseImage"))
    dependsOn(findProject(":samples:simple-jar")!!.tasks.getByName("jlinkGenerateReleaseImage"))
}

apply(from = "gradle/wrapper.gradle.kts")
