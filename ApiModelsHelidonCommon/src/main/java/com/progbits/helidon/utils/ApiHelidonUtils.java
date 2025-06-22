package com.progbits.helidon.utils;

import com.progbits.api.model.ApiObject;
import io.helidon.http.HeaderName;
import io.helidon.http.HttpException;
import io.helidon.http.Status;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import java.util.List;

/**
 *
 * @author scarr
 */
public class ApiHelidonUtils {
    public static String getPathVar(ServerRequest req, String field, boolean required) throws HttpException {
        if (req.path().pathParameters().contains(field)) {
            return req.path().pathParameters().get(field);
        } else {
            if (required) {
                throw new HttpException(field + " IS REQUIRED", Status.BAD_REQUEST_400);
            } else {
                return null;
            }
        }
    }
    
    public static String getQueryVar(ServerRequest req, String field, boolean required) throws HttpException {
        if (req.query().contains(field)) {
            return req.query().get(field);
        } else {
            if (required) {
                throw new HttpException(field + " IS REQUIRED", Status.BAD_REQUEST_400);
            } else {
                return null;
            }
        }
    }
    
    public static List<String> getQueryVarAsList(ServerRequest req, String field, boolean required) throws HttpException {
        if (req.query().contains(field)) {
            return req.query().all(field);
        } else {
            if (required) {
                throw new HttpException(field + " IS REQUIRED", Status.BAD_REQUEST_400);
            } else {
                return null;
            }
        }
    }
    
    public static <N> N getQueryVar(Class<N> type, ServerRequest req, String field, boolean required) throws HttpException {
        if (req.query().contains(field)) {
            try {
                if (type.equals(String.class)) {
                    return type.cast(req.query().get(field));
                } else if (type.equals(Integer.class)) {
                    Integer i = Integer.parseInt(req.query().get(field));

                    return type.cast(i);
                } else if (type.equals(Double.class)) {
                    Double d = Double.parseDouble(req.query().get(field));

                    return type.cast(d);
                }

                return null;
            } catch (NumberFormatException nfe) {
                throw new HttpException(field + " Is Not a Number", Status.BAD_REQUEST_400);
            }
        } else {
            if (required) {
                throw new HttpException(field + " IS REQUIRED", Status.BAD_REQUEST_400);
            } else {
                return null;
            }
        }
    }
    
    public static String getHeader(ServerRequest req, HeaderName header, boolean required) throws HttpException {
        if (req.headers().contains(header)) {
            return req.headers().get(header).get();
        } else {
            if (required) {
                throw new HttpException(header.toString() + " HEADER IS REQUIRED", Status.BAD_REQUEST_400);
            } else {
                return null;
            }
        }
    }
    
    public static void send(ServerResponse resp, Integer code, ApiObject subject) throws HttpException {
        if (subject == null) {
            resp.status(204);
            resp.send();
        } else {
            resp.status(code);
            resp.send(subject);
        }
    }
}
