module com.example.screenshotandpaint {
    requires javafx.controls;
    requires javafx.fxml;
    requires kotlin.stdlib;
    requires javafx.swing;


    opens com.example.screenshotandpaint to javafx.fxml;
    exports com.example.screenshotandpaint;
}