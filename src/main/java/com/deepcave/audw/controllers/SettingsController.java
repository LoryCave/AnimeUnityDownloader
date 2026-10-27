package com.deepcave.audw.controllers;

import com.deepcave.audw.AppSettings;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class SettingsController {
    private Stage stage;

    @FXML CheckBox divideChbx;
    @FXML TextField dwTxtField;
    @FXML TextField txt_url;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    public void initialize() {
        String savedDir = AppSettings.getDownloadDir();
        String downloadUrl = AppSettings.getDownloadUrl();

        if (savedDir != null) {
            dwTxtField.setText(savedDir);
        }
        txt_url.setText(downloadUrl);
        divideChbx.setSelected(AppSettings.isDivided());
    }

    @FXML
    protected void changeDivided() {
        AppSettings.setDivided(divideChbx.isSelected());
    }

    @FXML
    protected void chooseDwDir() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Seleziona cartella di download");
        String current = AppSettings.getDownloadDir();
        if (current != null) {
            java.io.File currentDir = new java.io.File(current);
            if (currentDir.exists()) chooser.setInitialDirectory(currentDir);
        }
        java.io.File selected = chooser.showDialog(stage);
        if (selected != null) {
            AppSettings.setDownloadDir(selected.getAbsolutePath());
            dwTxtField.setText(selected.getAbsolutePath());
        }
    }

    @FXML
    protected void saveDownloadUrl() {
        String url = txt_url.getText();
        AppSettings.setDownloadUrl(url);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("URL salvato");
        alert.setHeaderText(null);
        alert.setContentText("Il nuovo URL è stato salvato:\n" + url);
        alert.showAndWait();
    }

    @FXML
    protected void cancel() {
        stage.close();
    }
}
