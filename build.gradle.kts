import org.gradleweaver.plugins.jlink.JLinkTask

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
        apply(plugin = "org.gradleweaver.simple-jlink")
    }
}

tasks.named<Task>("check") {
    dependsOn(gradle.includedBuild("simple-jlink").task(":check"))
    dependsOn(findProject(":samples:javafx-app")!!.tasks.withType<JLinkTask>())
    dependsOn(findProject(":samples:simple-jar")!!.tasks.withType<JLinkTask>())
    dependsOn(findProject(":samples:groovy-dsl")!!.tasks.withType<JLinkTask>())
    dependsOn(findProject(":samples:modular-app")!!.tasks.withType<JLinkTask>())
}

apply(from = "gradle/wrapper.gradle.kts")
