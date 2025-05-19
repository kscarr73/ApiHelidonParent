package com.progbits.helidon.handlers;

import com.progbits.api.exception.ApiException;
import com.progbits.api.utils.ApiResources;
import io.helidon.http.HeaderNames;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

/**
 *
 * @author scarr
 */
public class ApiRapiDocHandler implements HttpService {

    private final String contextPath;

    public ApiRapiDocHandler(String contextPath) {
        this.contextPath = contextPath;
    }

    private String apiDoc;
    private String rapiDocHtml;

    @Override
    public void routing(HttpRules rules) {
        try {
            apiDoc = ApiResources.getInstance().getResourceAsString("api-doc.yaml");
            rapiDocHtml = ApiResources.getInstance().getResourceAsString("rapidoc.html");

            rapiDocHtml = rapiDocHtml.replace("%%REPLACEME%%", contextPath + "/api-doc.yaml");
        } catch (ApiException ex) {
            // Nothing to do here yet
        }

        rules.get(contextPath + "/api", this::getApiHtml);
        rules.get(contextPath + "/api-doc.yaml", this::getApiDoc);
    }

    private void getApiHtml(ServerRequest req, ServerResponse resp) throws Exception {
        resp.header(HeaderNames.CONTENT_TYPE, "text/html");
        resp.send(rapiDocHtml);
    }

    private void getApiDoc(ServerRequest req, ServerResponse resp) throws Exception {
        resp.header(HeaderNames.CONTENT_TYPE, "application/x-yaml");
        resp.send(apiDoc);
    }
}
