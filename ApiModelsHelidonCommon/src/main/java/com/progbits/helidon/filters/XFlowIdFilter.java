package com.progbits.helidon.filters;

import io.helidon.http.HeaderName;
import io.helidon.http.HeaderNames;
import io.helidon.webserver.http.Filter;
import io.helidon.webserver.http.FilterChain;
import io.helidon.webserver.http.RoutingRequest;
import io.helidon.webserver.http.RoutingResponse;
import java.util.UUID;
import org.slf4j.MDC;

/**
 *
 * @author scarr
 */
public class XFlowIdFilter implements Filter {

    private final String xflowPrefix;

    /**
     * Set a XFlow Filter with a specific prefix, used for creating FlowId
     * 
     * @param xflowPrefix The prefix to use.  Appends a `-` to the Prefix
     */
    public XFlowIdFilter(String xflowPrefix) {
        this.xflowPrefix = xflowPrefix + "-";
    }
 
    public static final HeaderName XFLOWID = HeaderNames.create("X-Flow-Id");
    
    /**
     * Processes Header: X-Flow-Id, if it is not found, it creates it.
     * 
     * Adds the X-Flow-Id header to the Response
     * 
     * The flowid is stored in MDC as well, so it can be used in Standard Logs.
     * 
     * @param fc The chain of filters
     * @param req The Request to Process
     * @param resp The Response to Update
     */
    @Override
    public void filter(FilterChain fc, RoutingRequest req, RoutingResponse resp) {
        if (req.headers().contains(XFLOWID)) {
            resp.header(XFLOWID, req.headers().value(XFLOWID).get());
            
            MDC.put("flowid", req.headers().value(XFLOWID).get());
        } else {
            String newFlowId = xflowPrefix + UUID.randomUUID().toString();
            resp.header(XFLOWID, newFlowId);
            
            MDC.put("flowid", newFlowId);
        }
        
        fc.proceed();
    }
    
}
