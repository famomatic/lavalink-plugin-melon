package cafe.cocochino.melonPlugin;

import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
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

    public void process(LocalAudioTrackExecutor executor) throws Exception {

    }


}
