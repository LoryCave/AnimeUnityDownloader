package com.deepcave.audw.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;

public class DownloadProgressController {
    @FXML public Label episodeLabel;
    @FXML public Label progressLabel;
    @FXML public ProgressBar progressBar;
    @FXML public Button cancelButton;

    private Stage stage;
    private volatile boolean cancelled = false;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void cancel() {
        cancelled = true;
    }

    @FXML
    protected void onCancel() {
        cancelled = true;
        stage.close();
    }
}
