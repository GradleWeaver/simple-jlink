package org.gradleweaver.plugin.sample.jlink.javafx;

import javafx.application.Application;

/**
 * Note: while this class is not strictly required, the JVM assumes that the JavaFX modules are on the module path when
 * it attempts to load the application class specified by "JavaFX-Main-Class" in the manifest. It also fails
 * if the application class has a main() method that calls launch. Additionally, Gradle does not add the JavaFX modules
 * to the module path when running, causing errors at runtime since the JavaFX modules are not present on the module
 * path.
 *
 * The workaround, then, is to create a trivial main class that only calls {@link Application#launch}. This main class
 * is specified in both the Gradle application plugin configuration and the JAR manifest.
 */
public final class Main {
  public static void main(String[] args) {
    // Force GTK 2 on Linux, since JavaFX defaults to GTK 3 and has issues on some Linux distros not named Ubuntu
    System.setProperty("jdk.gtk.version", "2");
    Application.launch(SampleJavafxApp.class);
  }
}
