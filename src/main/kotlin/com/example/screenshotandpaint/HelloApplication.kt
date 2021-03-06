package com.example.screenshotandpaint

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage

class HelloApplication : Application() {
    override fun start(stage: Stage) {
        val fxmlLoader = FXMLLoader(HelloApplication::class.java.getResource("hello-view.fxml"))
        val scene = Scene(fxmlLoader.load(), 1280.0, 720.0)
        fxmlLoader.getController<HelloController>().stage = stage
        stage.title = "ScreenshotApp"
        stage.scene = scene
        stage.show()
    }
}


fun main() {
    Application.launch(HelloApplication::class.java)
}