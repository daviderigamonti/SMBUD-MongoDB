package com.smbud.app;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.smbud.app.db.Verify;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;

public class GUIController {
    @FXML
    private HBox resultBox;
    @FXML
    private Label resultLabel, noImageLabel;
    @FXML
    private TextField filePath, tempSSN;
    @FXML
    private ImageView imagePreview;

    File file = null;
    String code = null;

    @FXML
    public void initialize() {
        imagePreview.managedProperty().bind(imagePreview.visibleProperty());
        noImageLabel.managedProperty().bind(noImageLabel.visibleProperty());
        resultBox.managedProperty().bind(resultBox.visibleProperty());
        resultLabel.visibleProperty().bind(resultBox.visibleProperty());
        imagePreview.setVisible(false);
        resultBox.setVisible(false);
    }

    public void chooseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All image formats", "*.jpg", "*.jpeg", "*.png", "*.bmp"),
                new FileChooser.ExtensionFilter("JPEG (*.jpg;*.jpeg)", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("PNG (*.png)", "*.png"),
                new FileChooser.ExtensionFilter("BMP (*.bmp)", "*.bmp"));
        file = fileChooser.showOpenDialog(filePath.getScene().getWindow());

        try {
            if (file == null)
                throw new Exception();

            code = readQR();

            filePath.setText(file.getPath());
            imagePreview.setImage(new Image(file.toURI().toURL().toExternalForm()));
            imagePreview.setVisible(true);
            noImageLabel.setVisible(false);

        } catch(Exception e ) {
            file = null;
            code = null;
            filePath.setText("");
            imagePreview.setImage(null);
            imagePreview.setVisible(false);
            noImageLabel.setVisible(true);

            Alert alert = new Alert(Alert.AlertType.WARNING, "Submit a valid image", ButtonType.OK);
            alert.setHeaderText("Invalid QR code source");
            alert.showAndWait();
        }
        resultBox.setVisible(false);
    }

    public void verifyQR() {
        if(file != null && code != null) {
            String result = "QR code not valid";
            String style = "-fx-border-color:crimson; -fx-background-color: red;";
            if(Verify.getInstance().executeVerification(tempSSN.getText())) {
                result = "Valid QR code";
                style = "-fx-border-color:green; -fx-background-color: limegreen;";
            }
            resultLabel.setText(result + ": " + code);
            resultLabel.setStyle(style);
            resultBox.setVisible(true);
        }
    }

    private String readQR() throws Exception
    {

        Map<DecodeHintType, Object> hintMap = new HashMap<>();
        hintMap.put(DecodeHintType.POSSIBLE_FORMATS, new Vector<>(Collections.singletonList(BarcodeFormat.QR_CODE)));
        hintMap.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);

        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(
                new BufferedImageLuminanceSource(ImageIO.read(new FileInputStream(file.getAbsolutePath())))));

        Result result = new MultiFormatReader().decode(binaryBitmap, hintMap);

        return result.getText();
    }

}