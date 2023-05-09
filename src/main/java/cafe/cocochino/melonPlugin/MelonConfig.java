package cafe.cocochino.melonPlugin;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "plugins.melon")
@Component
public class MelonConfig {
    private boolean isEnabled = true;

    public boolean getEnabled() {
        return this.isEnabled;
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }

}