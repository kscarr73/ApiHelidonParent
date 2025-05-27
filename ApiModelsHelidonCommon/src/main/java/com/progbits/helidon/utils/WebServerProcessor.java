package com.progbits.helidon.utils;

import com.progbits.api.helidon.media.json.ApiModelsJsonMediaSupport;
import com.progbits.api.helidon.media.yaml.ApiModelsYamlMediaSupport;
import io.helidon.config.Config;
import io.helidon.http.encoding.ContentEncodingContext;
import io.helidon.http.encoding.gzip.GzipEncoding;
import io.helidon.http.media.MediaContext;
import io.helidon.logging.common.LogConfig;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.WebServerConfig;

/**
 *
 * @author scarr
 */
public class WebServerProcessor {
    /**
     * Return a WebServerBuilder for creating a Helidon WebServer instance
     * 
     * <p>Expects application.yaml in Classpath with a server object</p>
     * <p>Example:<br>WebServerProcessor.returnWebServer(true, true)<br>
     * .router(DefaultRoutes::routing)<br>
     * .build()<br>
     * .start()<br></p>
     * 
     * @param apiObjects true/false Setup ApiObject Media handling
     * @param gzipEncoding true/false Setup GZip Handling
     * @return WebServerConfig.Builder with or without ApiObject and Gzip Handling
     */
    public static WebServerConfig.Builder returnWebServer(boolean apiObjects, boolean gzipEncoding) {
        Config helidonConfig = Config.create();
        
        LogConfig.configureRuntime();
        
        var webServerBuilder = WebServer.builder();
        
        webServerBuilder.config(helidonConfig.get("server"));
        
        if (apiObjects) {
            var mediaCtx = MediaContext.builder()
                .mediaSupportsDiscoverServices(false)
                .addMediaSupport(new ApiModelsYamlMediaSupport())
                .addMediaSupport(new ApiModelsJsonMediaSupport(true))
                .build();
            
            webServerBuilder.mediaContext(mediaCtx);
        }

        if (gzipEncoding) {
            var contentEncoding = ContentEncodingContext.builder()
                .contentEncodingsDiscoverServices(false)
                .addContentEncoding(GzipEncoding.create())
                .build();

            webServerBuilder.contentEncoding(contentEncoding);
        }
        
            
        return webServerBuilder;
    }
}
