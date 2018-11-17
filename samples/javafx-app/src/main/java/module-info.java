module org.gradleweaver.plugin.sample.jlink.javafx {
  requires javafx.base;
  requires javafx.controls;
  requires javafx.fxml;
  requires javafx.graphics;

  exports org.gradleweaver.plugin.sample.jlink.javafx to javafx.graphics;
}
