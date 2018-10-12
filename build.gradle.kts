buildscript {
    dependencies {
        classpath("org.gradleweaver.plugins:simple-jlink:+")
    }
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

apply(from = "gradle/wrapper.gradle.kts")
