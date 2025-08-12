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
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MelonAudioSourceManager implements AudioSourceManager, HttpConfigurable {
    private static final Logger log = LoggerFactory.getLogger(MelonAudioSourceManager.class);
    private final HttpInterfaceManager httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();

    private final String sourceName = "melon";
    private final String SEARCH_PREFIX = "msearch:";
    private final String PLAY_PREFIX = "mplay:";
    private final String MELON_SEARCH_URL = "https://www.melon.com/search/song/index.htm";
    private final String MELON_SONG_INFO_REGEX = "https://www\\.melon\\.com/song/detail\\.htm\\?songId=(\\d+)";
    private final Pattern melonSongPattern = Pattern.compile(MELON_SONG_INFO_REGEX);

    public MelonAudioSourceManager() {}

    @Override
    public String getSourceName() {
        return sourceName;
    }

    @Override
    public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) {
        try {
            // msearch:Query
            if (reference.identifier.startsWith(SEARCH_PREFIX)) {
                return this.getSearch(reference.identifier.substring(SEARCH_PREFIX.length()).trim());
            }
            // mplay:Query
            if (reference.identifier.startsWith(PLAY_PREFIX)) {
                return this.getPlay(manager, reference.identifier.substring(PLAY_PREFIX.length()).trim());
            }
            // Melon URL
            Matcher matcher = melonSongPattern.matcher(reference.identifier);
            if (matcher.find() && !matcher.group(1).equals("")) {
                return this.getItem(Integer.parseInt(matcher.group(1)));
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

    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
        return null;
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


    private AudioItem getSearch(String query) throws Exception {
        // Create request instance
        HttpGet searchRequest = new HttpGet(MELON_SEARCH_URL);
        URI qs = new URIBuilder(searchRequest.getURI()) // Create QueryString Builder
                .setParameter("q", urlEncodeUTF8(query))
                .build();
        searchRequest.setURI(qs); // Set QueryString

        var searchResBody = this.fetchBody(searchRequest);
        log.info(searchResBody);
        return new BasicAudioPlaylist("Search results for: " + query, new ArrayList<AudioTrack>(), null, true);
    }

    private AudioItem getPlay(AudioPlayerManager manager, String query) throws Exception {
        AudioItem searchResult = this.getSearch(query);
        if (searchResult instanceof AudioPlaylist playlist) {
            if (!playlist.getTracks().isEmpty()) {
                AudioTrack firstTrack = playlist.getTracks().get(0);
                String ytQuery = firstTrack.getInfo().title + " " + firstTrack.getInfo().author;
                return new AudioReference("ytsearch:" + ytQuery, null);
            }
        }
        return new AudioReference("ytsearch:" + query, null);
    }

    private AudioItem getItem(int songNumber) {
        log.info(String.format("%d", songNumber));
        return null;
    }

//    private AudioTrack parseTrackFromDetails(String detailsHtml) {
//        return;
//    }
//
//    private List<AudioTrack> parseSearchResults(String searchResultHtml) {
//        return;
//    }

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
        CloseableHttpResponse response = httpInterfaceManager.getInterface().execute(httpRequest); // Get page data
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) { // Not OK
            response.close();
            throw new RuntimeException("Failed request code: " + statusCode);
        }

        HttpEntity httpEntity = response.getEntity();
        return EntityUtils.toString(httpEntity, "UTF-8");
    }

    private String urlEncodeUTF8(String str) {
        return URLEncoder.encode(str, StandardCharsets.UTF_8);
    }
}
