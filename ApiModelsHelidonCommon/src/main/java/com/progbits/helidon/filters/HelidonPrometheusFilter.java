package com.progbits.helidon.filters;

import io.helidon.webserver.http.Filter;
import io.helidon.webserver.http.FilterChain;
import io.helidon.webserver.http.RoutingRequest;
import io.helidon.webserver.http.RoutingResponse;
import io.prometheus.metrics.core.datapoints.Timer;
import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Histogram;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;

public class HelidonPrometheusFilter implements Filter{

    private Histogram histogram = null;
    private Counter statusCounter = null;
    private String metricName = "http_status";

    public HelidonPrometheusFilter() {
        configure();
    }

    public HelidonPrometheusFilter(String metricName) {
        this.metricName = metricName;
        configure();
    }

    private void configure() {
        JvmMetrics.builder().register();

        histogram = Histogram.builder()
            .help("The time taken fulfilling the request")
            .name(metricName)
            .register();

        statusCounter = Counter.builder()
            .name(metricName + "_status_total")
            .help("Count of requests by Status")
            .labelNames("path", "method", "status")
            .register();
    }
    @Override
    public void filter(FilterChain chain, RoutingRequest req, RoutingResponse res) {
        Timer tmr = histogram.startTimer();
        try {
            chain.proceed();
        } finally {
            tmr.observeDuration();
            statusCounter
              .labelValues(req.requestedUri().path().toString(), 
                 req.prologue().method().text(), 
                 String.valueOf(res.status().code()))
              .inc();
        }
    }
    
}
