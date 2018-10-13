package org.gradleweaver.plugin.sample.jlink.javafx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public final class SampleJavafxApp extends Application {
  @Override
  public void start(Stage stage) {
    Label label = new Label("Hello, world!");
    stage.setScene(new Scene(new StackPane(label), 640, 480));
    stage.setTitle("Sample JavaFX Application");
    stage.show();
  }
}
