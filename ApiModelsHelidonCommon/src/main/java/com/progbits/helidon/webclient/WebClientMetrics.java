package com.progbits.helidon.webclient;

import io.helidon.webclient.api.WebClientServiceRequest;
import io.helidon.webclient.api.WebClientServiceResponse;
import io.helidon.webclient.spi.WebClientService;
import io.prometheus.metrics.core.datapoints.Timer;
import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Histogram;
import io.prometheus.metrics.model.snapshots.Unit;

/**
 *
 * @author scarr
 */
public class WebClientMetrics implements WebClientService {

    private static final Counter total;
    private static final Counter statuses;
    private static final Histogram buckets;
    
    public WebClientMetrics() {
        
    }
    
    @Override
    public WebClientServiceResponse handle(Chain chain, WebClientServiceRequest clientRequest) {
        WebClientServiceResponse resp = null;
        
        Timer tmr = buckets.labelValues(clientRequest.uri().path().rawPathNoParams()).startTimer();
        
        try {
            total.labelValues(clientRequest.uri().path().rawPathNoParams()).inc();
            resp = chain.proceed(clientRequest);
        } finally {
            tmr.observeDuration();
            tmr.close();
            
            if (resp != null) {
                statuses.labelValues(clientRequest.uri().path().rawPathNoParams(), resp.status().codeText()).inc();
            }
        }
        
        return resp;
    }
    
    static {
        total = Counter.builder()
            .name("webclient_totals")
            .help("Web Client Total Count")
            .labelNames("path")
            .register();
        
        statuses = Counter.builder()
            .name("webclient_status")
            .help("Web Client Count By Status")
            .labelNames("path", "status")
            .register();
        
        buckets = Histogram.builder()
            .name("webclient_duration_seconds")
            .help("Web Client Duration in Seconds")
            .unit(Unit.SECONDS)
            .labelNames("path")
            .register();
    }
}
