package com.progbits.api.helidon.media.json;

import com.progbits.api.exception.ApiClassNotFoundException;
import com.progbits.api.exception.ApiException;
import com.progbits.api.parser.JsonObjectParser;
import io.helidon.common.GenericType;
import io.helidon.http.Headers;
import io.helidon.http.HttpMediaType;
import io.helidon.http.media.EntityReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 *
 * @author scarr
 */
class ApiJsonReader<T> implements EntityReader<T> {

    private static final JsonObjectParser parser = new JsonObjectParser(true);

    @Override
    public T read(GenericType<T> gt, InputStream in, Headers reqHdrs) {
        return read(gt, in, contentTypeCharset(reqHdrs));
    }

    @Override
    public T read(GenericType<T> gt, InputStream in, Headers reqHdrs, Headers respHdrs) {
        return read(gt, in, contentTypeCharset(respHdrs));
    }

    private T read(GenericType<T> type, InputStream in, Charset charset) {
        try (Reader r = new InputStreamReader(in, charset)) {
            return type.cast(parser.parseSingle(r));
        } catch (ApiException | ApiClassNotFoundException ae) {
            throw new RuntimeException(ae.getMessage());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Charset contentTypeCharset(Headers headers) {
        return headers.contentType()
            .flatMap(HttpMediaType::charset)
            .map(Charset::forName)
            .orElse(StandardCharsets.UTF_8);
    }
}
