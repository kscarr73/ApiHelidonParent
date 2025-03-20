package com.progbits.helidon.webclient;

import com.progbits.api.helidon.media.json.ApiModelsJsonMediaSupport;
import com.progbits.api.helidon.media.yaml.ApiModelsYamlMediaSupport;
import com.progbits.api.model.ApiObject;
import io.helidon.common.media.type.MediaTypes;
import io.helidon.config.Config;
import io.helidon.http.HeaderNames;
import io.helidon.http.Method;
import io.helidon.webclient.api.ClientResponseTyped;
import io.helidon.webclient.api.HttpClientRequest;
import io.helidon.webclient.api.HttpClientResponse;
import io.helidon.webclient.api.WebClient;

/**
 *
 * @author scarr
 */
public class WebClientUtil {
    public static WebClient getClient(String baseUri) {
        Config config = Config.create();
        
        return WebClient.builder()
            .config(config.get("client"))
            .baseUri(baseUri)
            .addMediaSupport(new ApiModelsJsonMediaSupport())
            .addMediaSupport(new ApiModelsYamlMediaSupport())
            .build();
    }
    
    public static ApiObject makeHttpCall(WebClient client, String url, String method, ApiObject headers, ApiObject payload) {
        HttpClientRequest req = client.method(Method.create(method));
        
        if (url.startsWith("http:")) {
            req.uri(url);
        } else {
            
        }
        
        if (headers != null) {
            for (var hdr : headers.keySet()) {
                if ("Content-Type".equals(hdr)) {
                    req.contentType(MediaTypes.create(headers.getString(hdr)));
                } else {
                    req.header(HeaderNames.create(hdr), headers.getString(hdr));
                }
            }
            
            if (!headers.isSet("Content-Type")) {
                req.contentType(MediaTypes.APPLICATION_JSON);
            }
        } else {
            req.contentType(MediaTypes.APPLICATION_JSON);
        }
        
        if (payload != null) {
            ClientResponseTyped<ApiObject> resp = req.submit(payload, ApiObject.class);
            
            
            
        }
        
        return null;
    }
}
