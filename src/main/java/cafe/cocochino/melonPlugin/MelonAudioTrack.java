package cafe.cocochino.melonPlugin;

import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.*;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;

public class MelonAudioTrack extends DelegatedAudioTrack {
    private final MelonAudioSourceManager sourceManager;
    private final String artworkURL;

    public MelonAudioTrack(AudioTrackInfo trackInfo, String artworkURL, MelonAudioSourceManager sourceManager) {
        super(trackInfo);
        this.artworkURL = artworkURL;
        this.sourceManager = sourceManager;
    }

    public String getArtworkURL() {
        return artworkURL;
    }

    @Override
    public AudioSourceManager getSourceManager() {
        return this.sourceManager;
    }

    @Override
    public void process(LocalAudioTrackExecutor executor) throws Exception {
        String query = this.getInfo().title + " " + this.getInfo().author;
        AudioItem item = sourceManager.getPlayerManager().loadItemSync("ytsearch:" + query);
        AudioTrack delegate = null;
        if (item instanceof AudioPlaylist playlist && !playlist.getTracks().isEmpty()) {
            delegate = playlist.getTracks().get(0);
        } else if (item instanceof AudioTrack track) {
            delegate = track;
        }

        if (delegate == null) {
            throw new FriendlyException("No matching track found for query: " + query,
                    FriendlyException.Severity.COMMON, null);
        }

        processDelegate((InternalAudioTrack) delegate, executor);
    }
}
