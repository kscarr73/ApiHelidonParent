package com.progbits.helidon.filters;

import io.helidon.webserver.http.Filter;
import io.helidon.webserver.http.FilterChain;
import io.helidon.webserver.http.RoutingRequest;
import io.helidon.webserver.http.RoutingResponse;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
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
    public static final String IGNORE_HEALTH_METRICS = ".*(/healthcheck|/metrics)";

    private Pattern ignoreRegEx = null;

    private static final LinkedBlockingQueue<LoggingEventBuilder> queue = new LinkedBlockingQueue<>();
    private boolean _useQueue = false;

    /**
     * Create a Helidon Access Log with Slf4j Map values.
     *
     */
    public HelidonSlf4jAccessLogFilter() {
    }

    /**
     * Create a Helidon Access Log with Slf4j Map Values, with optional Ignored
     * paths.
     *
     * <ul>
     * <li>status: The Status of the Response</li>
     * <li>length: The length of the Response</li>
     * <li>requestUri: The full URL that was requested</li>
     * <li>speed: The time it took for the Request</li>
     * <li>timestamp: When the request started</li>
     * <li>method: The Request Method</li>
     * <li>path: The path without host and query</li>
     * <li>clientip: The IP the request came from</li>
     * <li>reqhost: The Host where the Request was received</li>
     * <li>reqproto: The HTTP Protocol used</li>
     * <li>request: METHOD + URI + Query</li>
     * <li>sourceip: The IP where the Request was received</li>
     * <li>hdr_{header}: Entry for each header that was in the Request.
     * Authorization header is NEVER set.</li>
     * <li>flowid: The X-FlowId header sent in the request, or the flowid that
     * was generated.</li>
     * </ul>
     *
     * @param ignorePattern Pattern used to ignore certain request urls. Used to
     * ignore healthcheck or metrics calls.
     */
    public HelidonSlf4jAccessLogFilter(String ignorePattern) {
        ignoreRegEx = Pattern.compile(ignorePattern);
    }

    /**
     * Create a Helidon Access Log with Slf4j Map Values, with optional Ignored
     * paths.
     *
     * <ul>
     * <li>status: The Status of the Response</li>
     * <li>length: The length of the Response</li>
     * <li>requestUri: The full URL that was requested</li>
     * <li>speed: The time it took for the Request</li>
     * <li>timestamp: When the request started</li>
     * <li>method: The Request Method</li>
     * <li>path: The path without host and query</li>
     * <li>clientip: The IP the request came from</li>
     * <li>reqhost: The Host where the Request was received</li>
     * <li>reqproto: The HTTP Protocol used</li>
     * <li>request: METHOD + URI + Query</li>
     * <li>sourceip: The IP where the Request was received</li>
     * <li>hdr_{header}: Entry for each header that was in the Request.
     * Authorization header is NEVER set.</li>
     * <li>flowid: The X-FlowId header sent in the request, or the flowid that
     * was generated.</li>
     * </ul>
     *
     * @param ignorePattern Pattern used to ignore certain request urls. Used to
     * ignore healthcheck or metrics calls.
     * @param useQueue Use ASYNC queue for access logs.  May speed up requests
     */
    public HelidonSlf4jAccessLogFilter(String ignorePattern, boolean useQueue) {
        ignoreRegEx = Pattern.compile(ignorePattern);
        _useQueue = useQueue;

        if (_useQueue) {
            Thread.ofPlatform()
                .name("Access Log Queue")
                .daemon(true)
                .start(this::processQueue);
        }
    }

    private void processQueue() {
        while (!(Thread.currentThread().isInterrupted())) {
            try {
                var entry = queue.take();

                entry.log();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void filter(FilterChain chain, RoutingRequest req, RoutingResponse res) {
        long startTime = System.currentTimeMillis();

        try {
            chain.proceed();
        } finally {
            if (ignoreRegEx == null || !ignoreRegEx.matcher(req.requestedUri().path().rawPathNoParams()).matches()) {
                LoggingEventBuilder builder = LOG.atInfo()
                    .addKeyValue("status", res.status().code())
                    .addKeyValue("length", res.bytesWritten())
                    .addKeyValue("requestUri", req.requestedUri().toUri().toString())
                    .addKeyValue("speed", System.currentTimeMillis() - startTime)
                    .addKeyValue("timestamp", startTime)
                    .addKeyValue("method", req.prologue().method().toString())
                    .addKeyValue("path", req.path().rawPathNoParams())
                    .addKeyValue("clientip", req.remotePeer().address().toString())
                    .addKeyValue("reqhost", req.localPeer().host())
                    .addKeyValue("reqproto", req.requestedUri().scheme())
                    .addKeyValue("request", String.format("%s %s", req.prologue().method().toString(),
                        req.requestedUri().path().toString()))
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

                if (_useQueue) {
                    queue.add(builder);
                } else {
                    builder.log();
                }
            }
        }
    }

}
