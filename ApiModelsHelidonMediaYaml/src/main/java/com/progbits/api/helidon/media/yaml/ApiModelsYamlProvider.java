package com.progbits.api.helidon.media.yaml;

import io.helidon.common.Weighted;
import io.helidon.common.config.Config;
import io.helidon.http.media.MediaSupport;
import io.helidon.http.media.spi.MediaSupportProvider;

/**
 *
 * @author scarr
 */
public class ApiModelsYamlProvider implements MediaSupportProvider, Weighted {

    @Override
    public String configKey() {
        return "ApiModelsYaml";
    }

    @Override
    public MediaSupport create(Config config, String string) {
        return new ApiModelsYamlMediaSupport();
    }

    @Override
    public double weight() {
        return 8;
    }
    
}
