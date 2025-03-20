package com.progbits.helidon.handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import io.helidon.http.HeaderNames;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import io.prometheus.metrics.expositionformats.ExpositionFormatWriter;
import io.prometheus.metrics.expositionformats.ExpositionFormats;
import io.prometheus.metrics.model.registry.PrometheusRegistry;

public class PrometheusEndpointHandler implements HttpService {
    public static final String DEFAULT_PATH = "/metrics";
    
    private PrometheusRegistry registry;
    private ExpositionFormats prometheusFormats;

    public PrometheusEndpointHandler() {
        prometheusFormats = ExpositionFormats.init();
        registry = PrometheusRegistry.defaultRegistry;
    }

    @Override
    public void routing(HttpRules rules) {
        rules.get(this::handleMetrics);
    }

    private void handleMetrics(ServerRequest req, ServerResponse resp) {
        ExpositionFormatWriter writer = prometheusFormats.findWriter(req.headers().value(HeaderNames.ACCEPT).get());

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            writer.write(out, registry.scrape());
        } catch (IOException iox) {
            // nothing really to do here
        }

        resp.header(HeaderNames.CONTENT_TYPE, writer.getContentType());
        resp.send(out.toByteArray());
    }

}
