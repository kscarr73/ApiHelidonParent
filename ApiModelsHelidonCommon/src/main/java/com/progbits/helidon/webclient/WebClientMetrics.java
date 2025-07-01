package com.progbits.helidon.webclient;

import com.progbits.api.model.ApiObject;
import io.helidon.webclient.api.WebClientServiceRequest;
import io.helidon.webclient.api.WebClientServiceResponse;
import io.helidon.webclient.spi.WebClientService;
import io.prometheus.metrics.core.datapoints.Timer;
import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Histogram;
import io.prometheus.metrics.model.snapshots.Unit;
import java.util.List;

/**
 * 
 * @author scarr
 */
public class WebClientMetrics implements WebClientService {

    private static final Counter total;
    private static final Counter statuses;
    private static final Histogram buckets;
    
    private final ApiObject config;
    private final List<String> truncateUrls;
    
    public WebClientMetrics() {
        config = new ApiObject();
        
        truncateUrls = null;
    }
    
    public WebClientMetrics(ApiObject config) {
        this.config = config;
        
        truncateUrls = config.getStringArray("truncateUrls");
    }
    
    @Override
    public WebClientServiceResponse handle(Chain chain, WebClientServiceRequest clientRequest) {
        WebClientServiceResponse resp = null;
        
        String urlPath = clientRequest.uri().path().rawPathNoParams();
        
        if (truncateUrls != null && !truncateUrls.isEmpty()) {
            for (var test : truncateUrls) {
                if (urlPath.startsWith(test)) {
                    urlPath = test;
                    break;
                }
            }
        }
        
        
        Timer tmr = buckets.labelValues(urlPath, clientRequest.method().text()).startTimer();
        
        try {
            total.labelValues(urlPath, clientRequest.method().text()).inc();
            resp = chain.proceed(clientRequest);
        } finally {
            tmr.observeDuration();
            tmr.close();
            
            if (resp != null) {
                statuses.labelValues(urlPath, resp.status().codeText(), clientRequest.method().text()).inc();
            }
        }
        
        return resp;
    }
    
    static {
        total = Counter.builder()
            .name("webclient_totals")
            .help("Web Client Total Count")
            .labelNames("path", "method")
            .register();
        
        statuses = Counter.builder()
            .name("webclient_status")
            .help("Web Client Count By Status")
            .labelNames("path", "status", "method")
            .register();
        
        buckets = Histogram.builder()
            .name("webclient_duration_seconds")
            .help("Web Client Duration in Seconds")
            .unit(Unit.SECONDS)
            .labelNames("path", "method")
            .register();
    }
}
