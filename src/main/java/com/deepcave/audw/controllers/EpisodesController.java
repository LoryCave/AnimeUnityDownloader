package com.deepcave.audw.controllers;

import java.util.List;

import com.deepcave.audw.models.Episode;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class EpisodesController {
    @FXML private RadioButton singleRadio;
    @FXML private RadioButton rangeRadio;
    @FXML private HBox singlePane;
    @FXML private HBox rangePane;
    @FXML private Spinner<Integer> singleSpinner;
    @FXML private Spinner<Integer> fromSpinner;
    @FXML private Spinner<Integer> toSpinner;

    private Stage stage;
    private List<Episode> episodes;
    private List<Episode> selectedEpisodes;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setEpisodes(List<Episode> episodes) {
        this.episodes = episodes;
        int total = episodes.size();
        singleSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, total, 1));
        fromSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, total, 1));
        toSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, total, total));
    }

    public List<Episode> getSelectedEpisodes() {
        return selectedEpisodes;
    }

    @FXML
    protected void onModeChange() {
        boolean range = rangeRadio.isSelected();
        rangePane.setVisible(range);
        rangePane.setManaged(range);
        singlePane.setVisible(!range);
        singlePane.setManaged(!range);
    }

    @FXML
    protected void onDownload() {
        if (singleRadio.isSelected()) {
            int index = singleSpinner.getValue() - 1;
            selectedEpisodes = List.of(episodes.get(index));
        } else {
            int from = fromSpinner.getValue() - 1;
            int to = toSpinner.getValue();
            if (from >= to) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Selezione non valida");
                alert.setHeaderText(null);
                alert.setContentText("L'episodio iniziale non può essere maggiore di quello finale.");
                alert.showAndWait();
                return;
            }
            selectedEpisodes = List.copyOf(episodes.subList(from, to));
        }
        stage.close();
    }

    @FXML
    protected void onCancel() {
        stage.close();
    }
}
