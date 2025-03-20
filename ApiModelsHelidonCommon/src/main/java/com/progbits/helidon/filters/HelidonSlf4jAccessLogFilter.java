package com.progbits.helidon.filters;

import io.helidon.webserver.http.Filter;
import io.helidon.webserver.http.FilterChain;
import io.helidon.webserver.http.RoutingRequest;
import io.helidon.webserver.http.RoutingResponse;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;

/**
 * 
 * @author scarr
 */
public class HelidonSlf4jAccessLogFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(HelidonSlf4jAccessLogFilter.class);
    private Pattern ignoreRegEx = null;

    public HelidonSlf4jAccessLogFilter() {
    }
    
    public HelidonSlf4jAccessLogFilter(String ignorePattern) {
        ignoreRegEx = Pattern.compile(ignorePattern);
    }
    
    @Override
    public void filter(FilterChain chain, RoutingRequest req, RoutingResponse res) {
        long startTime = System.currentTimeMillis();
        
        try {
            chain.proceed();
        } finally {
            if (ignoreRegEx == null || !ignoreRegEx.matcher(req.requestedUri().toUri().toString()).matches()) {
                LoggingEventBuilder builder = LOG.atInfo()
                    .addKeyValue("status", res.status().code())
                    .addKeyValue("length", res.bytesWritten())
                    .addKeyValue("requestUri", req.requestedUri().toUri().toString())
                    .addKeyValue("speed", System.currentTimeMillis() - startTime)
                    .addKeyValue("timestamp", startTime)
                    .addKeyValue("method", req.prologue().method().toString())
                    .addKeyValue("clientip", req.remotePeer().address().toString())
                    .addKeyValue("reqhost", req.localPeer().host())
                    .addKeyValue("reqproto", req.requestedUri().scheme())
                    .addKeyValue("request", String.format("%s %s %s", req.prologue().method().toString(), 
                        req.requestedUri().toUri().toString(),
                        !req.query().isEmpty() ? "?" + req.query().rawValue() : ""))
                    .addKeyValue("sourceip", req.localPeer().address().toString());

                if (req.headers().size() > 0) {
                    req.headers().forEach((h) -> { 
                        if (!"authorization".equalsIgnoreCase(h.name())) {
                            builder.addKeyValue("hdr_" + h.name(), h.get());
                        }
                    });
                }

                Optional<String> xflowId = res.headers().value(XFlowIdFilter.XFLOWID);

                if (xflowId.isPresent()) {
                    builder.addKeyValue("flowid", xflowId.get());
                }

                builder.log();
            }
        }
    }
    
}
