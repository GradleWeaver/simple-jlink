# simple-jlink

A simple Gradle plugin for generating native Java runtime images with the jlink tool introduced in Java 9.
Your applications can be modular, unmodular, and use modular or unomdular libraries. The
plugin will automatically determine which modules to include in the generated image.

See [the jlink Reference Page](https://docs.oracle.com/javase/9/tools/jlink.htm) for details on the
jlink tool and its options, most of which are exposed to plugin users.

```kotlin
plugins {
  application
  id("org.gradleweaver.plugins.jlink")
}

jlink {
  "configuration name" {
    applicationJar = tasks.getByName<Jar>("jar").archivePath // No default value
    jlinkDir = buildDir.resolve("jlink")
    
    // jlink executable options. See the reference page for details
    bindServices = false
    compressionLevel = JLinkTask.CompressionLevel.NONE
    endianess = JLinkTask.Endianness.SYSTEM_DEFAULT
    ignoreSigningInformation = false
    excludeHeaderFiles = false
    excludeManPages = false
    stripDebug = false
    optimizeClassForName = false
    
    // Single method to exclude header files, man pages, and debug symbols
    // Also sets the compression level to ZIP (maximum)
    useMinimalImage()
  }
}
```

See the [samples](samples) directory for more examples on the jlink plugin in action.

## Unavailable Options

`--disable-plugin pluginname`  
`--launcher`  
`--limit-modules`  
`--save-opts`  
`@filename`
