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
}

apply(from = "gradle/wrapper.gradle.kts")
