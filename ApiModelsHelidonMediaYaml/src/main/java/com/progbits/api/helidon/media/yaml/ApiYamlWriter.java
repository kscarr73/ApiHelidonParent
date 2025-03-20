package com.progbits.api.helidon.media.yaml;

import com.progbits.api.exception.ApiException;
import com.progbits.api.model.ApiObject;
import com.progbits.api.writer.YamlObjectWriter;
import io.helidon.common.GenericType;
import io.helidon.http.HeaderValues;
import io.helidon.http.Headers;
import io.helidon.http.WritableHeaders;
import io.helidon.http.media.EntityWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;

/**
 *
 * @author scarr
 */
public class ApiYamlWriter<T> implements EntityWriter<T> {

    private static final YamlObjectWriter writer = new YamlObjectWriter(true);

    @Override
    public void write(GenericType<T> gt, T t, OutputStream out, Headers reqHdrs, WritableHeaders<?> respHdrs) {
        respHdrs.setIfAbsent(ApiYamlConstants.CONTENT_TYPE_YAML);
        
        write(gt, t, out);
    }

    @Override
    public void write(GenericType<T> gt, T t, OutputStream out, WritableHeaders<?> headers) {
        headers.setIfAbsent(ApiYamlConstants.CONTENT_TYPE_YAML);
        
        write(gt, t, out);
    }

    private void write(GenericType<T> type, T object, OutputStream out) {
        try (out) {
            String strOut = writer.writeSingle((ApiObject) object);
            
            out.write(strOut.getBytes());
        } catch (ApiException ae) {
            throw new RuntimeException(ae.getMessage());
        } catch (IOException io) {
            throw new UncheckedIOException(io);
        }
    }
}
