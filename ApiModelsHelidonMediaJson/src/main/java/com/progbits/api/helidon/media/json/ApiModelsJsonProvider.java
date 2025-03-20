package com.progbits.api.helidon.media.json;

import io.helidon.common.Weighted;
import io.helidon.common.config.Config;
import io.helidon.http.media.MediaSupport;
import io.helidon.http.media.spi.MediaSupportProvider;

/**
 *
 * @author scarr
 */
public class ApiModelsJsonProvider implements MediaSupportProvider, Weighted {

    @Override
    public String configKey() {
        return "ApiModelsJson";
    }

    @Override
    public MediaSupport create(Config config, String string) {
        return new ApiModelsJsonMediaSupport();
    }

    @Override
    public double weight() {
        return 8;
    }
    
}
