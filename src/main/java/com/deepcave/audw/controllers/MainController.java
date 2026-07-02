package com.deepcave.audw.controllers;

import java.io.IOException;
import java.util.List;

import com.deepcave.audw.AppSettings;
import com.deepcave.audw.Scraper;
import com.deepcave.audw.models.Episode;
import com.deepcave.audw.models.Record;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MainController {
    private final Scraper scraper;
    public ListView<Record> sideList;
    public ImageView selAnimeCover;
    public TextField searchKey;
    public Button searchButton;
    public Button downloadButton;
    public Button episodesButton;
    public Label titleLabel;
    public Label typeLabel;
    public Label plotLabel;
    public Label dirLabel;
    public MenuItem closeMenu;
    public MenuItem aboutMenu;
    public MenuItem settingsMenu;

    public MainController() {
        this.scraper = new Scraper();
    }

    @FXML
    public void initialize() {
        sideList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Record item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitle_eng());
            }
        });
        applySettings();
    }

    private void applySettings() {
        String dir = AppSettings.getDownloadDir();
        if (dir != null) {
            scraper.setDownloadDir(new java.io.File(dir));
            dirLabel.setText("Download dir: " + dir);
        }
        scraper.setDivided(AppSettings.isDivided());
    }

    @FXML
    protected void onSearch() {
        if(!searchKey.getText().isBlank()) {
            titleLabel.setText("");
            typeLabel.setText("");
            plotLabel.setText("");
            selAnimeCover.setImage(null);

            downloadButton.setDisable(true);
            episodesButton.setDisable(true);

            List<Record> records = scraper.searchAnime(searchKey.getText());

            sideList.setItems(FXCollections.observableArrayList(records));
        }
    }

    @FXML
    protected void selectItem() {
        Record selected = sideList.getSelectionModel().getSelectedItem();

        if (selected != null) {
            titleLabel.setText(selected.getTitle_eng());
            typeLabel.setText(selected.getType()+ " • "+ selected.getEpisodes_count()+ " episodi");
            plotLabel.setText(selected.getPlot());

            selAnimeCover.setImage(new Image(selected.getImageurl(), true));

            downloadButton.setDisable(false);

            if(selected.getEpisodes_count() > 1) 
                episodesButton.setDisable(false);
            else
                episodesButton.setDisable(true);
        }
    }

    @FXML
    protected void dwDir() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Seleziona cartella di download");
        Stage stage = (Stage) sideList.getScene().getWindow();
        java.io.File selected = chooser.showDialog(stage);
        if (selected != null) {
            AppSettings.setDownloadDir(selected.getAbsolutePath());
            scraper.setDownloadDir(selected);
            dirLabel.setText("Download dir: " + selected.getAbsolutePath());
        }
    }

    @FXML
    protected void openHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Informazioni");
        alert.setHeaderText("AnimeUnity Downloader");
        alert.setContentText("""
            Repository GitHub: https://github.com/LoryCave/audw
            Licenza: Creative Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0)
        """);
        alert.setResizable(true);
        alert.showAndWait();
    }

    @FXML
    protected void closeApp() {
        Stage stage = (Stage) sideList.getScene().getWindow();
        if (stage != null)
            stage.close();
            
        Platform.exit();
    }

    @FXML
    protected void openSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/deepcave/audw/settings-view.fxml"));
            Parent root = loader.load();
            SettingsController settingsController = loader.getController();

            Stage settingsStage = new Stage();
            settingsStage.initModality(Modality.APPLICATION_MODAL);
            settingsStage.setTitle("Impostazioni");
            settingsStage.setScene(new Scene(root));
            settingsStage.setResizable(false);
            settingsController.setStage(settingsStage);
            settingsStage.setOnCloseRequest(e -> settingsController.cancel());
            settingsStage.showAndWait();
            applySettings();
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    @FXML
    protected void downloadAll() {
        Record selected = sideList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/deepcave/audw/download-progress-view.fxml"));
            Parent root = loader.load();
            DownloadProgressController progressController = loader.getController();

            Stage progressStage = new Stage();
            progressStage.initModality(Modality.APPLICATION_MODAL);
            progressStage.setTitle("Download in corso...");
            progressStage.setScene(new Scene(root));
            progressStage.setResizable(false);
            progressController.setStage(progressStage);
            progressStage.setOnCloseRequest(e -> progressController.cancel());

            String animeId = selected.getId().toString() + "-" + selected.getSlug();

            Thread downloadThread = new Thread(() -> {
                try {
                    scraper.downloadAllAnime(animeId, new Scraper.DownloadCallback() {
                        @Override
                        public void onEpisodeStart(int current, int total, String filename) {
                            Platform.runLater(() -> {
                                progressController.episodeLabel.setText(
                                    "Episodio " + current + "/" + total + ": " + filename);
                                progressController.progressBar.setProgress(0);
                                progressController.progressLabel.setText("");
                            });
                        }

                        @Override
                        public void onProgress(long downloaded, long totalBytes) {
                            Platform.runLater(() -> {
                                if (totalBytes > 0) {
                                    double pct = (double) downloaded / totalBytes;
                                    progressController.progressBar.setProgress(pct);
                                    progressController.progressLabel.setText(String.format(
                                        "%d / %d MB (%.0f%%)",
                                        downloaded / 1_048_576, totalBytes / 1_048_576, pct * 100));
                                } else {
                                    progressController.progressBar.setProgress(-1);
                                    progressController.progressLabel.setText(
                                        String.format("%d MB scaricati", downloaded / 1_048_576));
                                }
                            });
                        }

                        @Override
                        public void onEpisodeComplete(String filename) { }
                    }, progressController::isCancelled);

                    Platform.runLater(progressStage::close);
                } catch (Exception ex) {
                    System.err.println(ex);
                    Throwable cause = ex.getCause();
                    String msg = (cause != null && cause.getMessage() != null) ? cause.getMessage()
                               : (ex.getMessage() != null && !ex.getMessage().isEmpty() ? ex.getMessage() : ex.toString());
                    Platform.runLater(() -> {
                        progressStage.close();
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Errore");
                            alert.setHeaderText("Errore durante il download");
                            alert.setContentText(msg);
                            alert.setResizable(true);
                            alert.showAndWait();
                        });
                    });
                }
            });
            downloadThread.setDaemon(true);
            downloadThread.start();

            progressStage.showAndWait();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Errore");
            alert.setHeaderText("Impossibile aprire la finestra di download");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    protected void showEpisodes() {
        Record selected = sideList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/deepcave/audw/episodes-view.fxml"));
            Parent root = loader.load();
            EpisodesController episodesController = loader.getController();

            String animeId = selected.getId().toString() + "-" + selected.getSlug();
            List<Episode> episodes = scraper.getEpisodes(animeId);
            episodesController.setEpisodes(episodes);

            Stage episodesStage = new Stage();
            episodesStage.initModality(Modality.APPLICATION_MODAL);
            episodesStage.setTitle("Seleziona episodi");
            episodesStage.setScene(new Scene(root));
            episodesStage.setResizable(false);
            episodesController.setStage(episodesStage);
            episodesStage.showAndWait();

            List<Episode> selectedEpisodes = episodesController.getSelectedEpisodes();
            if (selectedEpisodes == null || selectedEpisodes.isEmpty()) return;

            FXMLLoader progressLoader = new FXMLLoader(getClass().getResource("/com/deepcave/audw/download-progress-view.fxml"));
            Parent progressRoot = progressLoader.load();
            DownloadProgressController progressController = progressLoader.getController();

            Stage progressStage = new Stage();
            progressStage.initModality(Modality.APPLICATION_MODAL);
            progressStage.setTitle("Download in corso...");
            progressStage.setScene(new Scene(progressRoot));
            progressStage.setResizable(false);
            progressController.setStage(progressStage);
            progressStage.setOnCloseRequest(e -> progressController.cancel());

            Thread downloadThread = new Thread(() -> {
                try {
                    scraper.downloadAnime(animeId, selectedEpisodes, new Scraper.DownloadCallback() {
                        @Override
                        public void onEpisodeStart(int current, int total, String filename) {
                            Platform.runLater(() -> {
                                progressController.episodeLabel.setText(
                                    "Episodio " + current + "/" + total + ": " + filename);
                                progressController.progressBar.setProgress(0);
                                progressController.progressLabel.setText("");
                            });
                        }

                        @Override
                        public void onProgress(long downloaded, long totalBytes) {
                            Platform.runLater(() -> {
                                if (totalBytes > 0) {
                                    double pct = (double) downloaded / totalBytes;
                                    progressController.progressBar.setProgress(pct);
                                    progressController.progressLabel.setText(String.format(
                                        "%d / %d MB (%.0f%%)",
                                        downloaded / 1_048_576, totalBytes / 1_048_576, pct * 100));
                                } else {
                                    progressController.progressBar.setProgress(-1);
                                    progressController.progressLabel.setText(
                                        String.format("%d MB scaricati", downloaded / 1_048_576));
                                }
                            });
                        }

                        @Override
                        public void onEpisodeComplete(String filename) { }
                    }, progressController::isCancelled);

                    Platform.runLater(progressStage::close);
                } catch (Exception ex) {
                    System.err.println(ex);
                    Throwable cause = ex.getCause();
                    String msg = (cause != null && cause.getMessage() != null) ? cause.getMessage()
                               : (ex.getMessage() != null && !ex.getMessage().isEmpty() ? ex.getMessage() : ex.toString());
                    Platform.runLater(() -> {
                        progressStage.close();
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Errore");
                            alert.setHeaderText("Errore durante il download");
                            alert.setContentText(msg);
                            alert.setResizable(true);
                            alert.showAndWait();
                        });
                    });
                }
            });
            downloadThread.setDaemon(true);
            downloadThread.start();
            progressStage.showAndWait();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Errore");
            alert.setHeaderText("Impossibile aprire la finestra di selezione episodi");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}
