package com.progbits.helidon.webclient;

import com.progbits.api.exception.ApiClassNotFoundException;
import com.progbits.api.exception.ApiException;
import com.progbits.api.model.ApiObject;
import com.progbits.api.parser.JsonObjectParser;
import com.progbits.api.parser.YamlObjectParser;
import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webclient.api.HttpClientResponse;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webclient.http2.Http2ClientResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 *
 * @author scarr
 */
public class ProcessResponse {

    public static JsonObjectParser JSON_PARSER = new JsonObjectParser(true);
    public static YamlObjectParser YAML_PARSER = new YamlObjectParser(true);

    public static ApiObject processResponse(HttpClientResponse resp) throws ApiException {
        ApiObject respObj = new ApiObject();

        respObj.setInteger("status", resp.status().code());
        respObj.setString("statusText", resp.status().codeText());
        respObj.setBoolean("success", resp.status().family().equals(Status.Family.SUCCESSFUL));
        respObj.setString("statusFamily", resp.status().family().name());

        if (resp.entity().hasEntity()) {
            Optional<String> contentType = resp.headers().first(HeaderNames.CONTENT_TYPE);

            BufferedReader inputStream = null;

            try {
                if (resp instanceof Http2ClientResponse) {
                    inputStream = new BufferedReader(new InputStreamReader(resp.inputStream()));
                } else if (resp instanceof Http1ClientResponse) {
                    Optional<String> contentEncoding = resp.headers().first(HeaderNames.CONTENT_ENCODING);

                    if (contentEncoding.isPresent()) {
                        if ("gzip".equals(contentEncoding.get())) {
                            inputStream = new BufferedReader(new InputStreamReader(new GZIPInputStream(resp.inputStream())));
                        } else if ("deflate".equals(contentEncoding.get())) {
                            inputStream = new BufferedReader(new InputStreamReader(new InflaterInputStream(resp.inputStream())));
                        } else {
                            inputStream = new BufferedReader(new InputStreamReader(resp.inputStream()));
                        }
                    } else {
                        inputStream = new BufferedReader(new InputStreamReader(resp.inputStream()));
                    }
                }

                // Process Payload
                if (contentType.isEmpty()) {
                    processAsString(inputStream, respObj);
                } else if (contentType.get().contains("application/json")) {
                    respObj.setObject("payload", processJson(inputStream));
                } else if (contentType.get().contains("application/yaml")) {
                    respObj.setObject("payload", processYaml(inputStream));
                } else {
                    processAsString(inputStream, respObj);
                }
            } catch (IOException io) {
                // Nothing really to log here
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException io) {
                        // nothing to report here
                    }
                }
            }
        }

        return respObj;
    }

    public static ApiObject processJson(BufferedReader reader) throws ApiException {
        try {
            return JSON_PARSER.parseSingle(reader);
        } catch (ApiClassNotFoundException ac) {
            throw new ApiException(510, ac.getMessage());
        }
    }

    public static ApiObject processYaml(BufferedReader reader) throws ApiException {
        try {
            return YAML_PARSER.parseSingle(reader);
        } catch (ApiClassNotFoundException ac) {
            throw new ApiException(510, ac.getMessage());
        }
    }

    public static void processAsString(BufferedReader reader, ApiObject respObj) throws ApiException {
        String strValue = reader.lines().collect(Collectors.joining("\n"));

        if (strValue.startsWith("{") || strValue.startsWith("[")) {
            respObj.setObject("payload", processJson(new BufferedReader(new StringReader(strValue))));
        } else {
            respObj.setString("payload", strValue);
        }
    }
}
