package cafe.cocochino.melonPlugin;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import dev.arbjerg.lavalink.api.AudioPlayerManagerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MelonPlugin implements AudioPlayerManagerConfiguration {
    private static final Logger log = LoggerFactory.getLogger(MelonPlugin.class);

    private final MelonConfig melonConfig;

    public MelonPlugin(MelonConfig melonPluginConfig) {
        this.melonConfig = melonPluginConfig;
        log.info("Loading melon plugin...");
    }
    @Override
    public AudioPlayerManager configure(AudioPlayerManager manager) {
        log.info("MelonPlugin.configure called... setup AudioSourceManagers...");
        if (melonConfig.getEnabled()) {
            log.info("Registering Melon source manager...");
            var melonSourceManager = new MelonAudioSourceManager();
            manager.registerSourceManager(melonSourceManager);
        }
        return manager;
    }
}
