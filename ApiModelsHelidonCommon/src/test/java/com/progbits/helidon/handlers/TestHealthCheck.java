package com.progbits.helidon.handlers;

import com.progbits.api.model.ApiObject;
import org.testng.annotations.Test;

/**
 *
 * @author scarr
 */
public class TestHealthCheck {
    @Test
    public void testHealthCheck() {
        HealthcheckHandler hc = new HealthcheckHandler("Testing", "Testing");
        
        hc.registerHealthCheck(HealthcheckHandler.LEVEL_DEFAULT, this::performCheck);
        
        ApiObject objHandlers = hc.gatherHealthchecks(new String[] { HealthcheckHandler.LEVEL_DEFAULT });
        
        boolean bFailed = hc.validateHealth(objHandlers);
        
        assert !bFailed : "We failed a Check";
    }
    
    @Test
    public void testNoHealthCheck() {
        HealthcheckHandler hc = new HealthcheckHandler("Testing", "Testing");
        
        ApiObject objHandlers = hc.gatherHealthchecks(new String[] { HealthcheckHandler.LEVEL_DEFAULT });
        
        boolean bFailed = hc.validateHealth(objHandlers);
        
        assert !bFailed : "We failed a Check";
    }
    
    private ApiObject performCheck() {
        ApiObject retObj = new ApiObject();
        
        retObj.setBoolean(HealthcheckHandler.FIELD_HEALTHCHECK, true);
        
        return retObj;
    }
}
