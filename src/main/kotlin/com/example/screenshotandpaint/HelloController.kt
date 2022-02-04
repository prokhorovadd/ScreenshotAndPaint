package com.example.screenshotandpaint

import javafx.embed.swing.SwingFXUtils
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.geometry.Rectangle2D
import javafx.scene.SnapshotParameters
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.CheckBox
import javafx.scene.control.ColorPicker
import javafx.scene.control.ScrollPane
import javafx.scene.control.Slider
import javafx.scene.image.WritableImage
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import javafx.scene.robot.Robot
import javafx.stage.FileChooser
import javafx.stage.Screen
import javafx.stage.Stage
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import javax.imageio.ImageIO


class HelloController {
    @FXML
    private lateinit var hideAppCheckbox: CheckBox

    @FXML
    private lateinit var scrollView: ScrollPane;

    @FXML
    private lateinit var pingSlider: Slider

    @FXML
    private lateinit var brushSizeSlider: Slider

    @FXML
    private lateinit var brushColorPicker: ColorPicker

    @FXML
    private lateinit var imgCanvas: Canvas

    @FXML
    private lateinit var drawCanvas: Canvas

    @FXML
    private lateinit var cropCanvas: Canvas

    private lateinit var imgGC: GraphicsContext
    private lateinit var drawGC: GraphicsContext
    private lateinit var cropGC: GraphicsContext

    var stage: Stage?= null
    private var waiting: Long = 2
    private var brushSize = 5.0
    private var cutWidth = 0
    private var cutHeight = 0
    private var cutX = 0.0
    private var cutY = 0.0

    fun initialize() {
        drawGC = drawCanvas.graphicsContext2D
        drawCanvas.onMouseDragged = onMouseDrawEvent
        drawCanvas.onMouseClicked = onMouseDrawEvent

        cropCanvas.onMousePressed = cropOnMousePressed
        cropCanvas.onMouseDragged = cropOnMouseDragged
        cropCanvas.onMouseReleased = cropOnMouseReleased
        cropGC = cropCanvas.graphicsContext2D

        imgGC = imgCanvas.graphicsContext2D
        imgGC.isImageSmoothing = false

        pingSlider.setOnMouseReleased {
            waiting = pingSlider.value.toLong()
        }
        brushSizeSlider.setOnMouseReleased {
            brushSize = brushSizeSlider.value
        }
        brushColorPicker.setOnAction {
            drawGC.fill = brushColorPicker.value
        }

        if (!File(System.getenv("USERPROFILE") + "\\Desktop\\Results\\" + "path.txt").exists()) {
            File(System.getenv("USERPROFILE") + "\\Desktop\\Results").mkdir()
            File(System.getenv("USERPROFILE") + "\\Desktop\\Results\\" + "path.txt").createNewFile()
        }
    }

    private var onMouseDrawEvent = EventHandler<MouseEvent> { e ->
        val x = e.x - brushSize / 2
        val y = e.y - brushSize / 2

        if (e.button == MouseButton.PRIMARY) {
            drawGC.fillRect(x, y, brushSize, brushSize)
        } else if (e.button == MouseButton.SECONDARY){
            drawGC.clearRect(x, y, brushSize, brushSize)
        }
    }

    private var cropOnMousePressed = EventHandler<MouseEvent> { e ->
        cutX = e.x
        cutY = e.y
    }

    private var cropOnMouseDragged = EventHandler<MouseEvent> { e ->
        cropGC.fill = Color.BLACK
        cropGC.fillRect(0.0, 0.0, cropCanvas.width, cropCanvas.height)
        cropGC.fill = Color.WHITE
        cropGC.fillRect(cutX, cutY, e.x - cutX, e.y - cutY)
    }

    @FXML
    private fun screenshotButtonPressed() {
        takeScreenshot()
    }

    private fun takeScreenshot() {
        if (hideAppCheckbox.isSelected) {
            stage?.isIconified = true
        }

        Thread.sleep(300 + waiting * 1000)
        val screenSize = Screen.getPrimary().bounds
        val rectangle = Rectangle2D(0.0, 0.0, screenSize.width, screenSize.height)
        val image = Robot().getScreenCapture(null, rectangle)
        resize(image.width, image.height)
        drawGC.clearRect(0.0, 0.0, drawCanvas.width, drawCanvas.height)
        imgGC.drawImage(image, 0.0, 0.0)
        stage?.isIconified = false
    }

    private fun saveToDirectory(path: String) {
        val i = path.lastIndexOf('\\')
        val finalPath = path.substring(0, i + 1);
        File(System.getenv("USERPROFILE") + "\\Desktop\\Results\\" + "path.txt").writeText(finalPath)
    }

    @FXML
    private fun onKeyPressed(event: KeyEvent) {
        if (event.isControlDown && event.code == KeyCode.S && event.isShiftDown) {
            saveImage(false)
        } else if (event.isControlDown && event.code == KeyCode.S) {
            saveImage(true)
        } else if (event.isControlDown && event.code == KeyCode.O) {
            takeScreenshot()
        }
    }

    private fun resize(width: Double, height: Double) {
        imgCanvas.width = width
        imgCanvas.height = height
        scrollView.prefWidth = width
        scrollView.prefHeight = height + 100
        drawCanvas.width = width
        drawCanvas.height = height
        cropCanvas.width = width
        cropCanvas.height = height
    }

    private fun imposeCanvas(): WritableImage {
        val w = imgCanvas.width
        val h = imgCanvas.height
        val resultCanvas = Canvas(w, h)
        resultCanvas.graphicsContext2D.isImageSmoothing = false
        val params = SnapshotParameters()
        params.fill = Color.TRANSPARENT

        var imageBuffer = imgCanvas.snapshot(params, null)
        resultCanvas.graphicsContext2D.drawImage(imageBuffer, 0.0, 0.0)
        imageBuffer = drawCanvas.snapshot(params, null)
        resultCanvas.graphicsContext2D.drawImage(imageBuffer, 0.0, 0.0)
        imageBuffer = resultCanvas.snapshot(params, null)
        return imageBuffer
    }

    @FXML
    private fun cutButtonPressed() {
        cropCanvas.isDisable = false
        cropCanvas.opacity = 0.3
        cropGC.fillRect(0.0, 0.0, cropCanvas.width, cropCanvas.height)
    }

    private fun saveCroppedImage() {
        val image = imposeCanvas()
        val croppedImage = WritableImage(image.pixelReader, cutX.toInt(), cutY.toInt(), cutWidth, cutHeight)
        resize(cutWidth.toDouble(), cutHeight.toDouble())
        imgGC.drawImage(croppedImage, 0.0, 0.0)
    }

    private var cropOnMouseReleased = EventHandler<MouseEvent> { e ->
        cutWidth = e.x.toInt() - cutX.toInt()
        cutHeight = e.y.toInt() - cutY.toInt()

        if (cutWidth > drawCanvas.width - cutX) {
            cutWidth = drawCanvas.width.toInt() - cutX.toInt()
        }
        if (cutHeight > drawCanvas.height - cutY) {
            cutHeight = drawCanvas.height.toInt() - cutY.toInt()
        }

        cropCanvas.isDisable = true
        cropCanvas.opacity = 0.0
        saveCroppedImage()
    }

    @FXML
    private fun saveImage() {
        saveImage(false)
    }

    private fun saveImage(fastSaveKey: Boolean) {
        var directory = File(System.getenv("USERPROFILE") +
                "\\Desktop\\Results\\" + "path.txt").readText()

        if (directory == "") {
            directory = ".\\"
        }

        var path = directory + LocalDateTime.now().toLocalDate() + LocalDateTime.now().second + ".png"
        // fileChooser передает сразу с .png
        var file: File? = null

        if (!fastSaveKey) {
            val fileChooser = FileChooser()
            fileChooser.initialDirectory = File(directory)
            fileChooser.extensionFilters.addAll(FileChooser.ExtensionFilter("Png image *.png", "*.png"))
            file = fileChooser.showSaveDialog(null)

            if (file != null) {
                saveToDirectory(file.path)
                path = file.path
            }
        }

        if (file != null || fastSaveKey) {
            val imageBuffer = imposeCanvas()
            val imageToWrite = SwingFXUtils.fromFXImage(imageBuffer, null)
            try {
                ImageIO.write(imageToWrite, "png", File(path))
            } catch (ex: IOException) {
                print(ex)
            }
        }
    }

    @FXML
    private fun openImage() {
        val directory = File(System.getenv("USERPROFILE") + "\\Desktop\\Results\\" + "path.txt").readText()
        val fileChooser = FileChooser()
        fileChooser.initialDirectory = File(directory)
        fileChooser.extensionFilters.addAll(
            FileChooser.ExtensionFilter("Png image *.png", "*.png"),
            FileChooser.ExtensionFilter("Jpg image *.jpg", "*.jpg"),
            FileChooser.ExtensionFilter("Jpeg image *.jpeg", "*.jpeg")
        )
        val file = fileChooser.showSaveDialog(null)

        if (file != null) {
            val image = SwingFXUtils.toFXImage(ImageIO.read(file), null)
            resize(image.width, image.height)
            imgGC.drawImage(image, 0.0, 0.0)
        }
    }

    @FXML
    private fun closeApp() {
        stage?.close();
    }
}