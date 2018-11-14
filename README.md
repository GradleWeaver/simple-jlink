# simple-jlink

A simple Gradle plugin for generating native Java runtime images with the jlink tool introduced in Java 9.
Your applications can be modular, unmodular, and use modular or unomdular libraries. The
plugin will automatically determine which modules to include in the generated image.

Note: this plugin must be run on JDK 10 or higher. JDK 9 is not supported.

See [the jlink Reference Page](https://docs.oracle.com/javase/10/tools/jlink.htm) for details on the
jlink tool and its options, most of which are exposed to plugin users.

```kotlin
plugins {
  application
  id("org.gradleweaver.plugins.jlink")
}

jlink {
  "configuration name" {
    applicationJar = tasks.getByName<Jar>("jar").archivePath // No default value
    
    // jlink executable options. See the reference page for details
    bindServices = false
    compressionLevel = JLinkTask.CompressionLevel.NONE
    endianess = JLinkTask.Endianness.SYSTEM_DEFAULT
    ignoreSigningInformation = false
    excludeHeaderFiles = false
    excludeManPages = false
    stripDebug = false
    optimizeClassForName = false
    
    // Extra modules to link that are not discovered by jdeps
    extraModules = listOf("module1", "module2", ...)
    
    // Single method to exclude header files, man pages, and debug symbols
    // Also sets the compression level to ZIP (maximum)
    useMinimalImage()
    
    // Generate a launcher script
    launcher {
      launcherName = "LauncherName"
      vmOptions = listOf("-Xms512m", "-Xmx4G")
      
      // Only required if the app and all its dependencies are modular
      applicationModuleName = "com.example.app"
      mainClassName = "com.example.app.Main"
    }
  }
}
```

See the [samples](samples) directory for more examples on the jlink plugin in action.

## Unavailable Options

`--disable-plugin pluginname`  
`--limit-modules`  
`--save-opts`  
`@filename`
