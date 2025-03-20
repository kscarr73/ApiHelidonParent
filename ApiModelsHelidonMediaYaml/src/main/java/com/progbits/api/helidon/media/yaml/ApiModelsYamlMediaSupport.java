package com.progbits.api.helidon.media.yaml;

import com.progbits.api.model.ApiObject;
import io.helidon.common.GenericType;
import io.helidon.common.media.type.MediaTypes;
import io.helidon.http.HeaderNames;
import io.helidon.http.Headers;
import static com.progbits.api.helidon.media.yaml.ApiYamlConstants.CONTENT_TYPE_YAML;
import io.helidon.http.HttpMediaType;
import io.helidon.http.WritableHeaders;
import io.helidon.http.media.EntityReader;
import io.helidon.http.media.EntityWriter;
import io.helidon.http.media.MediaSupport;

/**
 *
 * @author scarr
 */
public class ApiModelsYamlMediaSupport implements MediaSupport {

    private static final GenericType<ApiObject> API_TYPE = GenericType.create(ApiObject.class);

    private final String name = "ApiModelsYaml";
    private final ApiYamlWriter writer = new ApiYamlWriter();
    private final ApiYamlReader reader = new ApiYamlReader();

    @Override
    public <T> ReaderResponse<T> reader(GenericType<T> type, Headers requestHeaders) {
        if (requestHeaders.contentType()
            .map(it -> it.test(MediaTypes.APPLICATION_YAML))
            .orElse(true)) {
            if (type.equals(API_TYPE)) {
                // leave this to JSON-P
                return ReaderResponse.unsupported();
            }
            return new ReaderResponse<>(SupportLevel.COMPATIBLE, this::reader);
        }

        return ReaderResponse.unsupported();
    }

    @Override
    public <T> WriterResponse<T> writer(GenericType<T> type, Headers requestHeaders, WritableHeaders<?> responseHeaders) {
        if (!API_TYPE.equals(type)) {
            return WriterResponse.unsupported();
        }

        // check if accepted
        for (HttpMediaType acceptedType : requestHeaders.acceptedTypes()) {
            if (acceptedType.test(MediaTypes.APPLICATION_YAML)) {
                return new WriterResponse<>(SupportLevel.COMPATIBLE, this::writer);
            }
        }

        if (requestHeaders.acceptedTypes().isEmpty()) {
            return new WriterResponse<>(SupportLevel.COMPATIBLE, this::writer);
        }

        return WriterResponse.unsupported();
    }

    @Override
    public <T> ReaderResponse<T> reader(GenericType<T> type, Headers requestHeaders, Headers responseHeaders) {
        if (!API_TYPE.equals(type)) {
            return ReaderResponse.unsupported();
        }

        // check if accepted
        for (HttpMediaType acceptedType : requestHeaders.acceptedTypes()) {
            if (acceptedType.test(MediaTypes.APPLICATION_YAML) || acceptedType.mediaType().isWildcardType()) {
                return new ReaderResponse<>(SupportLevel.COMPATIBLE, this::reader);
            }
        }

        if (requestHeaders.acceptedTypes().isEmpty()) {
            return new ReaderResponse<>(SupportLevel.COMPATIBLE, this::reader);
        }

        return ReaderResponse.unsupported();
    }

    @Override
    public <T> WriterResponse<T> writer(GenericType<T> type, WritableHeaders<?> requestHeaders) {
        if (!API_TYPE.equals(type)) {
            return WriterResponse.unsupported();
        }
        if (requestHeaders.contains(HeaderNames.CONTENT_TYPE)) {
            if (requestHeaders.contains(CONTENT_TYPE_YAML)) {
                return new WriterResponse<>(SupportLevel.COMPATIBLE, this::writer);
            }
        } else {
            return new WriterResponse<>(SupportLevel.SUPPORTED, this::writer);
        }
        return WriterResponse.unsupported();
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public String type() {
        return this.name;
    }

    <T> EntityReader<T> reader() {
        return reader;
    }

    <T> EntityWriter<T> writer() {
        return writer;
    }
}
