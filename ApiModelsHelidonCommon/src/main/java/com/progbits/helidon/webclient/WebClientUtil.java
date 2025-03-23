package com.progbits.helidon.webclient;

import com.progbits.api.exception.ApiException;
import com.progbits.api.helidon.media.json.ApiModelsJsonMediaSupport;
import com.progbits.api.helidon.media.yaml.ApiModelsYamlMediaSupport;
import com.progbits.api.model.ApiObject;
import com.progbits.helidon.filters.XFlowIdFilter;
import io.helidon.common.media.type.MediaTypes;
import io.helidon.config.Config;
import io.helidon.http.HeaderNames;
import io.helidon.http.Method;
import io.helidon.http.Status;
import io.helidon.http.encoding.ContentEncodingContextConfig;
import io.helidon.http.encoding.deflate.DeflateEncoding;
import io.helidon.http.encoding.gzip.GzipEncoding;
import io.helidon.webclient.api.ClientResponseTyped;
import io.helidon.webclient.api.HttpClientRequest;
import io.helidon.webclient.api.WebClient;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Objects;
import org.slf4j.MDC;

/**
 *
 * @author scarr
 */
public class WebClientUtil {

    /**
     * Return a WebClient with ApiObject media Support.
     *
     * @param baseUri Optional sets a Base URI for the WebClient
     *
     * @return The Created WebClient
     */
    public static WebClient getClient(String baseUri) {
        return getClient(baseUri, false, false);
    }

    /**
     * Return a WebClient with ApiObject media support
     *
     * Prometheus metrics includes:
     * <ul>
     * <li>webclient_totals: Counter - path is included in totals</li>
     * <li>webclient_status: Counter - Count of Each Status by Path and
     * Status</li>
     * <li>webclient_duration_seconds: Histogram - Duration by Path</li>
     * </ul>
     *
     * @param baseUri Optional sets a Base URI for the WebClient
     * @param includePrometheus If TRUE, will return a WebClient with Prometheus
     * metrics
     *
     * @return The Created WebClient
     */
    public static WebClient getClient(String baseUri, boolean includePrometheus, boolean enableCompression) {
        Config config = Config.create();

        var webClient = WebClient.builder()
            .config(config.get("client"))
            .addMediaSupport(new ApiModelsJsonMediaSupport())
            .addMediaSupport(new ApiModelsYamlMediaSupport());

        if (baseUri != null) {
            webClient.baseUri(baseUri);
        }

        if (includePrometheus) {
            webClient.addService(new WebClientMetrics());
        }

        if (enableCompression) {
            webClient.addHeader(HeaderNames.ACCEPT_ENCODING, "gzip, deflate");
            webClient.contentEncoding(
                ContentEncodingContextConfig.builder()
                    .addContentEncoding(GzipEncoding.create())
                    .addContentEncoding(DeflateEncoding.create())
                    .build()
            );
        }
        
        return webClient.build();
    }

    /**
     * Make an HTTP Call.
     *
     * Defaults to Content-Type application/json
     *
     * @param client WebClient to use for the Call
     * @param url Full URL to ignore baseUri. A Fragment is considered a url
     * that does not start with http:
     * @param method The Method for the Call. GET, POST, PUT, etc...
     * @param authorization OPTIONAL The String used for the Authorization
     * header.
     * @param errorField OPTIONAL The errorField to pull from a FAILURE.
     * Example: message, root[0].message for list
     * @param props OPTIONAL header to add Headers to the call. `params` to add
     * Query Params to the call
     * @param payload OPTIONAL Payload to send. If `formdata: true` is set, then
     * the request is sent application/x-www-form-urlencoded
     *
     * @return The Response ApiObject
     *
     * @throws ApiException Thrown if Call is not Successful (2xx)
     */
    public static ApiObject makeHttpCall(WebClient client, String url, String method, String authorization, String errorField, ApiObject props, ApiObject payload) throws ApiException {
        ClientResponseTyped<ApiObject> resp = makeHttpCallWithResp(client, url, method, authorization, props, payload);

        if (resp.status().family() == Status.Family.SUCCESSFUL) {
            return resp.entity();
        } else {
            throw new ApiException(resp.status().code(), resp.entity().getString(errorField));
        }
    }

    /**
     * Make an HTTP Call.
     *
     * Defaults to Content-Type application/json
     *
     * @param client WebClient to use for the Call
     * @param url Full URL to ignore baseUri. A Fragment is considered a url
     * that does not start with http:
     * @param method The Method for the Call. GET, POST, PUT, etc...
     * @param authorization OPTIONAL The String used for the Authorization
     * header.
     * @param props header to add Headers to the call. `params` to add Query
     * Params to the call
     * @param payload Payload to send. If `formdata: true` is set, then the
     * request is sent application/x-www-form-urlencoded, otherwise sent as
     * ApiObject with Content-Type JSON or YAML.
     *
     * @return ClientResponseTyped The entity will be the ApiObject returned.
     * You have full access to the Response object
     */
    public static ClientResponseTyped<ApiObject> makeHttpCallWithResp(WebClient client, String url, String method, String authorization, ApiObject props, ApiObject payload) {
        HttpClientRequest req = client.method(Method.create(method));

        if (url.startsWith("http:")) {
            req.uri(url);
        } else {
            req.path(url);
        }

        if (authorization != null) {
            req.header(HeaderNames.AUTHORIZATION, authorization);
        }

        if (props != null) {
            if (props.isSet("headers")) {
                for (var hdr : props.getObject("headers").keySet()) {
                    if ("Content-Type".equals(hdr)) {
                        req.contentType(MediaTypes.create(props.getObject("headers").getString(hdr)));
                    }
                    if ("Accept".equals(hdr)) {
                        req.accept(MediaTypes.create(props.getObject("headers").getString(hdr)));
                    } else {
                        req.header(HeaderNames.create(hdr), props.getObject("headers").getString(hdr));
                    }
                }
            }

            if (props.isSet("params")) {
                for (var prop : props.getObject("params").keySet()) {
                    if (props.getObject("params").getType(prop) == ApiObject.TYPE_STRINGARRAY) {
                        req.queryParam(prop, (String[]) props.getObject("params").getStringArray(prop).toArray());
                    } else {
                        req.queryParam(prop, props.getObject("params").getString(prop));
                    }
                }
            }

            if (!props.isSet("headers.Content-Type")) {
                req.contentType(MediaTypes.APPLICATION_JSON);
            }
        } else {
            req.contentType(MediaTypes.APPLICATION_JSON);
        }

        String flowId = MDC.get("flowid");

        if (flowId != null) {
            req.header(XFlowIdFilter.XFLOWID, flowId);
        }

        ClientResponseTyped<ApiObject> resp;

        if (payload != null) {
            if (payload.containsKey("formdata")) {
                req.contentType(MediaTypes.APPLICATION_FORM_URLENCODED);

                StringBuilder sbForm = new StringBuilder();
                boolean isFirst = true;

                for (var entry : payload.keySet()) {
                    if (!"formdata".equals(entry)) {
                        if (!isFirst) {
                            sbForm.append("&");
                        } else {
                            isFirst = false;
                        }

                        Object objValue = payload.get(entry);

                        if (objValue != null) {
                            sbForm.append(entry).append("=").append(URLEncoder.encode(Objects.toString(objValue), Charset.forName("UTF-8")));
                        }
                    }
                }

                resp = req.submit(sbForm.toString(), ApiObject.class);
            } else {
                resp = req.submit(payload, ApiObject.class);
            }
        } else {
            resp = req.request(ApiObject.class);
        }

        return resp;
    }
}
