package com.progbits.api.helidon.media.yaml;

import io.helidon.http.Header;
import io.helidon.http.HeaderNames;
import io.helidon.http.HeaderValues;

/**
 *
 * @author scarr
 */
public class ApiYamlConstants {
    public static Header CONTENT_TYPE_YAML = HeaderValues.createCached(HeaderNames.CONTENT_TYPE, true, true, "application/yaml");
}
