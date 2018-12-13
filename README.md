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
    // jlink executable options. See the reference page for details
    bindServices.set(false)
    compressionLevel.set(JLinkTask.CompressionLevel.NONE)
    endianess.set(JLinkTask.Endianness.SYSTEM_DEFAULT)
    ignoreSigningInformation.set(false)
    excludeHeaderFiles.set(false)
    excludeManPages.set(false)
    stripDebug.set(false)
    optimizeClassForName.set(false)
    
    // Extra modules to link that are not discovered by jdeps
    extraModules.addAll("module1", "module2", ...)
    
    // Single method to exclude header files, man pages, and debug symbols
    // Also sets the compression level to ZIP (maximum)
    useMinimalImage()
    
    // Generate a launcher script
    launcher {
      launcherName.set("LauncherName")
      vmOptions.addAll("-Xms512m", "-Xmx4G")
    }
  }
}
```

See the [samples](samples) directory for more examples on the jlink plugin in action.

## Unavailable Options

| Option | Reason |
|---|---|
`--disable-plugin pluginname` | All JLink plugins are available, though not necessarily used  
`--limit-modules` | Only necessary modules are linked. This option may be enabled in a future release.
`--save-opts` | Options are stored in the build script!  
`@filename` | Options are stored in the build script!

## JLink Tasks

| Task | Description |
|---|---|
`jlinkGenerate{configuration}` | Generates the JLink image for the given configuration name
`jlinkArchiveZip{configuration}` | Generates a `.zip` compressed archive containing the entire JLink image
`jlinkArchiveTar{configuration}` | Generates a `.tar` uncompressed archive containing the entire JLink image

`{configuration}` is generated based on the name of the configuration given in the build script.
If a configuration name is multiple words, each word will be capitalized and the whitespace removed
to create the name of the JLink task. In the example above, setting the configuration name to `"configuration name"`
will result in `jlinkGenerateConfigurationName` for the task to generate the JLink image for that
configuration, and `jlinkArchive[Zip|Tar]ConfigurationName` to create a zip or tar archive for that image.

Generated JLink images are created in `build/jlink/{configuration-name}`. The configuration name is exactly
as specified in the build script.

Generated archives are created in `build/distributions/{project-name}-{configuration}`, eg `example-project-release.zip`.

