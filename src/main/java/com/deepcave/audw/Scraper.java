package com.deepcave.audw;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.deepcave.audw.models.Episode;
import com.deepcave.audw.models.Record;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Scraper {
    private static final ObjectMapper mapper = new ObjectMapper();
    private File downloadDir = null;
    private boolean dividedFolders = true;

    public void setDownloadDir(File dir) {
        this.downloadDir = dir;
    }

    public void setDivided(boolean divided) {
        this.dividedFolders = divided;
    }

    public interface DownloadCallback {
        void onEpisodeStart(int current, int total, String filename);
        void onProgress(long downloaded, long totalBytes);
        void onEpisodeComplete(String filename);
    }

    public List<Record> searchAnime(String key) {
        String baseurl = "https://www.animeunity.so/archivio?title=";

        try {
            Connection req = Jsoup.connect(baseurl+ key);
            Document doc = req.get();

            Elements elements = doc.select("archivio");
            String records = elements.getFirst().attr("records");

            return mapper.readValue(records, new TypeReference<>() { });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Episode> getEpisodes(String anime) {
        String baseurl = "https://www.animeunity.so/anime/"+ anime;

        try {
            Connection req = Jsoup.connect(baseurl);
            Document doc = req.get();

            Elements video_player = doc.select("video-player");
            String ep = video_player.attr("episodes");

           return mapper.readValue(ep, new TypeReference<>() { });
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void downloadAllAnime(String anime, DownloadCallback callback, BooleanSupplier isCancelled) {
        List<Episode> episodes = getEpisodes(anime);
        downloadAnime(anime, episodes, callback, isCancelled);
    }

    @SuppressWarnings("deprecation")
    public void downloadAnime(String anime, List<Episode> episodes, DownloadCallback callback, BooleanSupplier isCancelled) {
        String baseurl = "https://www.animeunity.so/anime/"+ anime;

        File baseDir = downloadDir != null ? downloadDir : new File(".");
        File animeFolder = dividedFolders ? new File(baseDir, anime) : baseDir;
        animeFolder.mkdirs();

        try {
            int total = episodes.size();

            for (int i = 0; i < total; i++) {
                if (isCancelled.getAsBoolean()) break;
                Episode episode = episodes.get(i);

                Connection ep_cnn = Jsoup.connect(baseurl+ "/"+ episode.getId().toString());
                Document ep_doc = ep_cnn.get();

                Elements ep_video_player = ep_doc.select("video-player");
                String embed_url = ep_video_player.attr("embed_url");

                Connection embed_conn = Jsoup.connect(embed_url);
                Document embed = embed_conn.get();

                Elements script = embed.getElementsByTag("script");
                String scriptContent = "";
                for (int j = 0; j < script.size(); j++) {
                    try {
                        Element scripttemp = script.get(j);
                        if (scripttemp.dataNodes().getFirst().getWholeData().trim().contains("window.downloadUrl")) {
                            scriptContent = scripttemp.dataNodes().getFirst().getWholeData().trim();
                            break;
                        }
                    } catch (Exception e) { }
                }

                Pattern urlPattern = Pattern.compile("window\\.downloadUrl\\s*=\\s*'([^']+)'");
                Matcher matcher = urlPattern.matcher(scriptContent);
                if (!matcher.find()) {
                    System.err.println("Download URL not found for episode " + (i + 1));
                    continue;
                }
                String dw_url = matcher.group(1);

                Pattern filenamePattern = Pattern.compile("[?&]filename=([^&]+)");
                Matcher filenameMatcher = filenamePattern.matcher(dw_url);
                String filename = filenameMatcher.find() ? filenameMatcher.group(1) : "episode_" + (i + 1) + ".mp4";

                File outputFile = new File(animeFolder, filename);
                if (outputFile.exists()) {
                    if (callback != null) callback.onEpisodeComplete(filename);
                    continue;
                }

                if (callback != null) callback.onEpisodeStart(i + 1, total, filename);

                HttpURLConnection connection = (HttpURLConnection) new URL(dw_url).openConnection();
                connection.connect();
                long totalBytes = connection.getContentLengthLong();

                boolean episodeCancelled = false;
                try (InputStream in = connection.getInputStream();
                    FileOutputStream fos = new FileOutputStream(outputFile)) {
                    byte[] buffer = new byte[8192];
                    long dl = 0;
                    int read;
                    int lastPct = -1;
                    
                    while ((read = in.read(buffer)) != -1) {
                        if (isCancelled.getAsBoolean()) {
                            episodeCancelled = true;
                            break;
                        }

                        fos.write(buffer, 0, read);
                        dl += read;
                        if (callback != null) {
                            int pct = totalBytes > 0 ? (int) (dl * 100 / totalBytes) : -1;
                            if (pct != lastPct) {
                                lastPct = pct;
                                callback.onProgress(dl, totalBytes);
                            }
                        }
                    }
                }

                if (episodeCancelled) {
                    outputFile.delete();
                    break;
                }

                if (callback != null)
                    callback.onEpisodeComplete(filename);
            }
        } catch (IOException e) {
            if (!isCancelled.getAsBoolean()) {
                throw new RuntimeException(e);
            }
        }
    }
}
