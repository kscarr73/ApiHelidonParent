package com.progbits.helidon.utils;

import com.progbits.api.config.ConfigProvider;
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
        return returnWebServer(false, apiObjects, gzipEncoding);
    }
    
    /**
     * Return a WebServerBuilder for creating a Helidon WebServer instance
     * 
     * <p>Expects application.yaml in Classpath with a server object</p>
     * <p>Example:<br>WebServerProcessor.returnWebServer(true, true)<br>
     * .router(DefaultRoutes::routing)<br>
     * .build()<br>
     * .start()<br></p>
     * 
     * @param useApiConfig Use Api ConfigProvider instead of Config from Helidon
     * @param apiObjects true/false Setup ApiObject Media handling
     * @param gzipEncoding true/false Setup GZip Handling
     * @return WebServerConfig.Builder with or without ApiObject and Gzip Handling
     */
    public static WebServerConfig.Builder returnWebServer(boolean useApiConfig, boolean apiObjects, boolean gzipEncoding) {
        LogConfig.configureRuntime();
        
        var webServerBuilder = WebServer.builder();
        
        if (!useApiConfig) {
            Config helidonConfig = Config.create();
            
            webServerBuilder.config(helidonConfig.get("server"));
        } else {
            ConfigProvider apiConfig = ConfigProvider.getInstance();
            
            String host = apiConfig.getConfig().getString("server.host", "0.0.0.0");
            Integer port = apiConfig.getConfig().getInteger("server.port", 8080);
            
            webServerBuilder.host(host);
            webServerBuilder.port(port);
        }
        
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
