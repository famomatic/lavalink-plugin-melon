package cafe.cocochino.melonPlugin;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpConfigurable;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.track.*;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MelonAudioSourceManager implements AudioSourceManager, HttpConfigurable {
    private static final Logger log = LoggerFactory.getLogger(MelonAudioSourceManager.class);
    private final HttpInterfaceManager httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();

    private final String sourceName = "melon";
    private final String PLAY_PREFIX = "mplay:";
    private final String MELON_BASE_URL = "https://www.melon.com/";
    private final String MELON_SONG_INFO_REGEX = "https://www\\.melon\\.com/song/detail\\.htm\\?songId=(\\d+)";
    private final String MELON_ALBUM_INFO_REGEX = "https://www\\.melon\\.com/album/detail\\.htm\\?albumId=(\\d+)";
    private final Pattern melonSongPattern = Pattern.compile(MELON_SONG_INFO_REGEX);
    private final Pattern melonAlbumPattern = Pattern.compile(MELON_ALBUM_INFO_REGEX);
    private final Pattern kakaoSongPattern = Pattern.compile("songId=(\\d+)");
    private final Pattern kakaoAlbumPattern = Pattern.compile("albumId=(\\d+)");

    private AudioPlayerManager playerManager;
    private final AtomicBoolean sessionInitialized = new AtomicBoolean(false);

    public MelonAudioSourceManager() {
        this.httpInterfaceManager.configureBuilder(builder ->
                builder.setDefaultCookieStore(new BasicCookieStore()));
    }

    @Override
    public String getSourceName() {
        return sourceName;
    }

    @Override
    public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) {
        try {
            this.playerManager = manager;
            // mplay:Query
            if (reference.identifier.startsWith(PLAY_PREFIX)) {
                return this.getPlay(manager, reference.identifier.substring(PLAY_PREFIX.length()).trim());
            }
            // Melon Song URL
            Matcher matcher = melonSongPattern.matcher(reference.identifier);
            if (matcher.find() && !matcher.group(1).equals("")) {
                return this.getItem(Integer.parseInt(matcher.group(1)));
            }

            // Melon Album URL
            matcher = melonAlbumPattern.matcher(reference.identifier);
            if (matcher.find() && !matcher.group(1).equals("")) {
                return this.getAlbum(Integer.parseInt(matcher.group(1)));
            }

            // Kakao share links
            if (reference.identifier.contains("kko.kakao.com")) {
                return this.getKakaoItem(reference.identifier);
            }
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
        return null;
    }

    @Override
    public boolean isTrackEncodable(AudioTrack track) {
        return true;
    }

    @Override
    public void encodeTrack(AudioTrack track, DataOutput output) throws IOException {
        if (track instanceof MelonAudioTrack melonTrack) {
            output.writeUTF(melonTrack.getArtworkURL() != null ? melonTrack.getArtworkURL() : "");
        } else {
            output.writeUTF("");
        }
    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
        String artwork = input.readUTF();
        return new MelonAudioTrack(trackInfo, artwork, this);
    }

    public AudioPlayerManager getPlayerManager() {
        return this.playerManager;
    }

    @Override
    public void shutdown() {
        try {
            this.httpInterfaceManager.close();
        } catch (IOException e) {
            log.error("Failed to close HTTP interface manager", e);
        }
    }

    @Override
    public void configureRequests(Function<RequestConfig, RequestConfig> configurator) {
        this.httpInterfaceManager.configureRequests(configurator);
    }

    @Override
    public void configureBuilder(Consumer<HttpClientBuilder> configurator) {
        this.httpInterfaceManager.configureBuilder(configurator);
    }


    private AudioItem getPlay(AudioPlayerManager manager, String query) {
        AudioItem ytResult = manager.loadItemSync("ytsearch:" + query);

        if (ytResult instanceof AudioPlaylist ytPlaylist && !ytPlaylist.getTracks().isEmpty()) {
            return ytPlaylist.getTracks().get(0);
        }

        return ytResult;
    }

    private AudioItem getItem(int songNumber) throws Exception {
        String url = String.format("https://www.melon.com/song/detail.htm?songId=%d", songNumber);
        HttpGet get = new HttpGet(url);
        applyDefaultHeaders(get);

        var body = this.fetchBody(get);
        AudioTrack track = parseTrackFromDetails(body, songNumber, url);
        return track;
    }

    private AudioItem getAlbum(int albumNumber) throws Exception {
        String url = String.format("https://www.melon.com/album/detail.htm?albumId=%d", albumNumber);
        HttpGet get = new HttpGet(url);
        applyDefaultHeaders(get);

        var body = this.fetchBody(get);
        Document doc = Jsoup.parse(body);
        String title = doc.selectFirst("meta[property=og:title]") != null ?
                doc.selectFirst("meta[property=og:title]").attr("content") : "Album " + albumNumber;
        List<AudioTrack> tracks = parseTrackRows(doc);
        return new BasicAudioPlaylist(title, tracks, null, false);
    }

    private AudioItem getKakaoItem(String url) throws Exception {
        HttpGet get = new HttpGet(url);
        applyDefaultHeaders(get);

        var body = this.fetchBody(get);
        Document doc = Jsoup.parse(body);
        Element ogUrl = doc.selectFirst("meta[property=og:url]");
        if (ogUrl != null) {
            String target = ogUrl.attr("content");
            Matcher matcher = kakaoSongPattern.matcher(target);
            if (matcher.find()) {
                return this.getItem(Integer.parseInt(matcher.group(1)));
            }
            matcher = kakaoAlbumPattern.matcher(target);
            if (matcher.find()) {
                return this.getAlbum(Integer.parseInt(matcher.group(1)));
            }
        }
        return null;
    }

    private AudioTrack parseTrackFromDetails(String detailsHtml, int songNumber, String url) {
        Document doc = Jsoup.parse(detailsHtml);

        // Prefer page elements for clean title/artist information
        String title = "";
        Element titleEl = doc.selectFirst("div.song_name");
        if (titleEl != null) {
            title = titleEl.ownText().trim();
        }

        String artist = "";
        Element artistEl = doc.selectFirst("div.artist a.artist_name");
        if (artistEl == null) {
            artistEl = doc.selectFirst("div.profile-common .user-name");
        }
        if (artistEl != null) {
            artist = artistEl.text().trim();
        }

        // Fallback to meta tags when elements are missing
        Element metaTitle = doc.selectFirst("meta[property=og:title]");
        if ((title.isEmpty() || artist.isEmpty()) && metaTitle != null) {
            String content = metaTitle.attr("content");
            String[] parts = content.split(" - ", 2);
            if (title.isEmpty()) {
                title = parts[0].trim();
            }
            if (artist.isEmpty() && parts.length > 1) {
                artist = parts[1].trim();
            }
        }

        if (artist.isEmpty()) {
            Element artistMeta = doc.selectFirst("meta[property=og:author]");
            if (artistMeta != null) {
                artist = artistMeta.attr("content");
            }
        }

        String artwork = doc.selectFirst("meta[property=og:image]") != null ?
                doc.selectFirst("meta[property=og:image]").attr("content") : "";
        return createTrack(title, artist, String.valueOf(songNumber), url, artwork);
    }

    private List<AudioTrack> parseTrackRows(Document doc) {
        List<AudioTrack> tracks = new ArrayList<>();
        Elements rows = doc.select("tr[data-song-no]");
        for (Element row : rows) {
            String songId = row.attr("data-song-no");

            Element titleEl = row.selectFirst("div.ellipsis.rank01 a, div.ellipsis.rank01 span a");
            String title = titleEl != null ? titleEl.text() : "";

            // Artist markup occasionally omits the span wrapper, so be flexible
            Element artistEl = row.selectFirst("div.ellipsis.rank02 a, div.ellipsis.rank02 span a");
            String artist = artistEl != null ? artistEl.text() : "";

            // Skip rows without essential info to avoid bad search queries
            if (title.isEmpty() || artist.isEmpty()) {
                continue;
            }

            Element imgEl = row.selectFirst("a.image_typeAll img");
            String artwork = imgEl != null ? imgEl.attr("src") : "";
            String uri = "https://www.melon.com/song/detail.htm?songId=" + songId;
            tracks.add(createTrack(title, artist, songId, uri, artwork));
        }
        return tracks;
    }

    private AudioTrack createTrack(String title, String artist, String identifier, String uri, String artworkURL) {
        return new MelonAudioTrack(
                new AudioTrackInfo(
                        title,
                        artist,
                        0,
                        identifier,
                        false,
                        uri
                ),
                artworkURL,
                this
        );
    }

    private String fetchBody(HttpGet httpRequest) throws IOException {
        if (sessionInitialized.compareAndSet(false, true)) {
            initSession();
        }
        // Ensure default headers are present for all requests
        applyDefaultHeaders(httpRequest);
        CloseableHttpResponse response = httpInterfaceManager.getInterface().execute(httpRequest); // Get page data
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) { // Not OK
            response.close();
            throw new RuntimeException("Failed request code: " + statusCode);
        }

        HttpEntity httpEntity = response.getEntity();
        return EntityUtils.toString(httpEntity, "UTF-8");
    }

    private void initSession() throws IOException {
        HttpGet get = new HttpGet(MELON_BASE_URL);
        applyDefaultHeaders(get);
        CloseableHttpResponse response = httpInterfaceManager.getInterface().execute(get);
        EntityUtils.consume(response.getEntity());
        response.close();
    }

    private void applyDefaultHeaders(HttpGet request) {
        if (!request.containsHeader("User-Agent")) {
            request.setHeader("User-Agent", "Mozilla/5.0");
        }
        if (!request.containsHeader("Referer")) {
            request.setHeader("Referer", "https://www.melon.com/");
        }
        if (!request.containsHeader("Accept-Language")) {
            request.setHeader("Accept-Language", "ko-KR,ko;q=0.9");
        }
        if (!request.containsHeader("Accept")) {
            request.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8");
        }
        if (!request.containsHeader("Accept-Encoding")) {
            request.setHeader("Accept-Encoding", "gzip, deflate, br");
        }
        if (!request.containsHeader("Connection")) {
            request.setHeader("Connection", "keep-alive");
        }
    }

}
