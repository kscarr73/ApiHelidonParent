package com.progbits.helidon.utils;

import com.progbits.api.exception.ApiException;
import com.progbits.api.model.ApiObject;
import com.progbits.helidon.filters.HelidonPrometheusFilter;
import com.progbits.helidon.filters.HelidonSlf4jAccessLogFilter;
import com.progbits.helidon.filters.XFlowIdFilter;
import com.progbits.helidon.handlers.ApiRapiDocHandler;
import com.progbits.helidon.handlers.HealthCheck;
import com.progbits.helidon.handlers.HealthcheckHandler;
import com.progbits.helidon.handlers.PrometheusEndpointHandler;
import io.helidon.http.HttpException;
import io.helidon.webserver.http.HttpRouting;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;

/**
 *
 * @author scarr
 */
public class ApiRouterProcessor {

    public static ApiRouterProcessor builder(HttpRouting.Builder route, String contextPath) {
        return new ApiRouterProcessor(route, contextPath);
    }

    public ApiRouterProcessor(HttpRouting.Builder route, String contextPath) {
        this.route = route;
        this.contextPath = contextPath;
    }

    private final HttpRouting.Builder route;
    private final String contextPath;

    private String xflowIdPrefix = null;
    private boolean healthCheck = false;
    private boolean rapiDoc = true;
    private String prometheusPrefix = null;

    private String healthCheckTitle;
    private String healthCheckSubTitle;
    
    private String errorCode = "code";
    private String errorMessage = "message";

    Map<String, List<HealthCheck>> healthchecks = new HashMap<>();

    public ApiRouterProcessor healthCheck(String title, String subTitle) {
        healthCheckTitle = title;
        healthCheckSubTitle = subTitle;
        healthCheck = true;

        return this;
    }

    public ApiRouterProcessor registerHealthCheck(String level, HealthCheck check) {
        if (!healthchecks.containsKey(level)) {
            healthchecks.put(level, new ArrayList<>());
        }

        healthchecks.get(level).add(check);

        return this;
    }

    public ApiRouterProcessor xflowId(String prefix) {
        xflowIdPrefix = prefix;

        return this;
    }

    public ApiRouterProcessor prometheus(String prefix) {
        prometheusPrefix = prefix;

        return this;
    }
    
    public ApiRouterProcessor apiRapiDoc(boolean value) {
        this.rapiDoc = value;
        
        return this;
    }

    public ApiRouterProcessor errorFields(String code, String message) {
        this.errorCode = code;
        this.errorMessage = message;
        
        return this;
    }
    
    public void process(Logger log) {
        if (xflowIdPrefix != null) {
            route.addFilter(new XFlowIdFilter(xflowIdPrefix));
        }

        route.addFilter(new HelidonSlf4jAccessLogFilter(HelidonSlf4jAccessLogFilter.IGNORE_HEALTH_METRICS, true));

        if (prometheusPrefix != null) {
            route.addFilter(new HelidonPrometheusFilter(prometheusPrefix));
        }

        if (healthCheck) {
            HealthcheckHandler healthcheck = new HealthcheckHandler(healthCheckTitle, healthCheckSubTitle);

            healthcheck.setHealthChecks(healthchecks);

            route.register(contextPath + "/healthcheck", healthcheck);
        }

        if (prometheusPrefix != null) {
            route.register(contextPath + "/metrics", new PrometheusEndpointHandler());
        }
        
        if (rapiDoc) {
            route.register(new ApiRapiDocHandler(contextPath));
        }

        route.error(Exception.class, (req, res, ex) -> {
            ApiObject o = new ApiObject();
            Integer statusCode = 500;
            
            if (ex instanceof HttpException he) {
                statusCode = he.status().code();
            } else if (ex instanceof ApiException apx) {
                statusCode = apx.getCode();
            } 
            
            o.setInteger(errorCode, statusCode);
            res.status(statusCode);
                
            o.setString(errorMessage, ex.getMessage());
            
            res.send(o);

            if (statusCode != 400 && statusCode != 403) {
                log.atError().setCause(ex).log(ex.getMessage() + ": " + req.requestedUri().toUri().toString());
            }
        });
        
    }
}
