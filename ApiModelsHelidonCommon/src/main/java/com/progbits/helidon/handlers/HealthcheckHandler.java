package com.progbits.helidon.handlers;

import com.progbits.api.exception.ApiException;
import com.progbits.api.model.ApiObject;
import com.progbits.api.utils.ApiResources;
import io.helidon.common.media.type.MediaTypes;
import io.helidon.http.HeaderNames;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HealthcheckHandler implements HttpService {

    private static final Logger LOG = LoggerFactory.getLogger(HealthcheckHandler.class);

    public static final String LEVEL_DEFAULT = "DEFAULT";
    public static final String LEVEL_HIGH = "HIGH";
    public static final String LEVEL_MEDIUM = "MEDIUM";
    public static final String LEVEL_LOW = "LOW";

    public static final String FIELD_HEALTHCHECK = "healthcheck";
    public static final String FIELD_PROGRAM = "program";
    public static final String FIELD_PRIORITY = "priority";
    public static final String FIELD_COLOR = "color";
    public static final String FIELD_STATUS = "status";

    private String htmlFile = null;
    private String programName = "Default";
    private String subTitle = "Default";

    Map<String, List<HealthCheck>> healthchecks = new HashMap<>();

    public HealthcheckHandler(String name, String subTitle) {
        try {
            programName = name;
            this.subTitle = subTitle;
            htmlFile = ApiResources.getInstance().getResourceAsString("healthcheck.html");
        } catch (ApiException ex) {
            LOG.error("Healthcheck Could NOT find healthcheck.html");
        }
    }

    public void registerHealthCheck(String level, HealthCheck check) {
        if (!healthchecks.containsKey(level)) {
            healthchecks.put(level, new ArrayList<>());
        }

        healthchecks.get(level).add(check);
    }

    @Override
    public void routing(HttpRules rules) {
        rules.get(this::performHealthcheck);
    }

    private void performHealthcheck(ServerRequest req, ServerResponse resp) {
        ApiObject objHealth;

        if (req.query().contains("details")) {
            objHealth = gatherHealthchecks(new String[]{LEVEL_DEFAULT, LEVEL_HIGH, LEVEL_MEDIUM, LEVEL_LOW});

            resp.status(200);
            resp.header(HeaderNames.CONTENT_TYPE, "text/html");
            resp.send(createHtml(objHealth));
        } else if (req.query().contains("warn")) {
            objHealth = gatherHealthchecks(new String[]{LEVEL_DEFAULT, LEVEL_HIGH});

            boolean bFailed = validateHealth(objHealth);

            if (bFailed) {
                resp.status(503);
                resp.header(HeaderNames.CONTENT_TYPE, "text/plain");
                resp.send("Fail");
            } else {
                resp.status(200);
                resp.header(HeaderNames.CONTENT_TYPE, "text/plain");
                resp.send("Ok");
            }
        } else {
            objHealth = gatherHealthchecks(new String[]{LEVEL_DEFAULT});
            
            boolean bFailed = validateHealth(objHealth);

            if (bFailed) {
                resp.status(503);
                resp.header(HeaderNames.CONTENT_TYPE, "text/plain");
                resp.send("Fail");
            } else {
                resp.status(200);
                resp.header(HeaderNames.CONTENT_TYPE, "text/plain");
                resp.send("Ok");
            }
        }
    }
    
    private boolean validateHealth(ApiObject objHealth) {
        boolean bFailed = false;

        if (objHealth.isSet(FIELD_HEALTHCHECK)) {
            for (var entry : objHealth.getList(FIELD_HEALTHCHECK)) {
                bFailed = !entry.getBoolean(FIELD_HEALTHCHECK);

                if (bFailed) {
                    break;
                } 
            }
        } else {
            bFailed = true;
        }

        return bFailed;
    }
    private static final String sLine = "<tr>\n"
        + "                                <td>%%PROGRAM%%</td>\n"
        + "                                <td>%%PRIORITY%%</td>\n"
        + "                                <td class=\"table-%%CLASS%%\">%%STATUS%%</td>\n"
        + "                            </tr>";

    private ApiObject gatherHealthchecks(String[] levels) {
        String[] lclLevels = levels;

        if (lclLevels.length == 0) {
            lclLevels = new String[]{LEVEL_DEFAULT};
        }

        ApiObject resp = new ApiObject();
        resp.createList(FIELD_HEALTHCHECK);

        for (var level : lclLevels) {
            if (healthchecks.containsKey(level)) {
                for (var healthcheck : healthchecks.get(level)) {
                    resp.getList(FIELD_HEALTHCHECK).add(healthcheck.process());
                }
            }
        }

        return resp;
    }

    private String createHtml(ApiObject objHealth) {
        if (htmlFile == null) {
            return "";
        }

        String output = htmlFile;

        output = output.replaceAll("%%NAME%%", programName);
        output = output.replaceAll("%%SUBTITLE%%", subTitle);
        output = output.replace("%%BUILD_VERSION%%", "NA");
        output = output.replace("%%BUILD_DATE%%", "NA");

        if (objHealth.isSet(FIELD_HEALTHCHECK)) {
            StringBuilder sb = new StringBuilder();

            for (var entry : objHealth.getList(FIELD_HEALTHCHECK)) {
                String lclLine = sLine;

                lclLine = lclLine.replace("%%PROGRAM%%", entry.getString(FIELD_PROGRAM));
                lclLine = lclLine.replace("%%PRIORITY%%", entry.getString(FIELD_PRIORITY));
                lclLine = lclLine.replace("%%CLASS%%", entry.getBoolean(FIELD_HEALTHCHECK) ? "success" : "danger");
                lclLine = lclLine.replace("%%STATUS%%", entry.getString(FIELD_STATUS));

                sb.append(lclLine);
            }

            output = output.replace("%%PROGRAMS%%", sb.toString());
        }

        return output;
    }

}
