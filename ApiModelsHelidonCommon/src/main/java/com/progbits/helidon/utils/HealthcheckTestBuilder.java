package com.progbits.helidon.utils;

import com.progbits.api.model.ApiObject;
import com.progbits.helidon.handlers.HealthcheckHandler;

/**
 *
 * @author scarr
 */
public class HealthcheckTestBuilder {

    public enum PRIORITY {
        DEFAULT,HIGH,MEDIUM,LOW
    }
    
    private boolean healthcheck = false;
    private PRIORITY priority = PRIORITY.DEFAULT;
    private String title;
    private String failureColor;
    private String failureStatus;
    private String successColor;
    private String successStatus;
    
    public static HealthcheckTestBuilder builder() {
        return new HealthcheckTestBuilder();
    }
    
    public HealthcheckTestBuilder healthcheck(boolean value) {
        this.healthcheck = value;
        
        return this;
    }
    
    public HealthcheckTestBuilder title(String value) {
        this.title = value;
        
        return this;
    }
    
    public HealthcheckTestBuilder priority(PRIORITY value) {
        this.priority = value;
        
        return this;
    }
    
    public HealthcheckTestBuilder failure(String color, String status) {
        this.failureColor = color;
        this.failureStatus = status;
        
        return this;
    }
    
    public HealthcheckTestBuilder success(String color, String status) {
        this.successColor = color;
        this.successStatus = status;
        
        return this;
    }
    
    public ApiObject build() {
        ApiObject objRet = new ApiObject();

        objRet.setString(HealthcheckHandler.FIELD_PROGRAM, title);
        objRet.setString(HealthcheckHandler.FIELD_PRIORITY, priority.name());

        // Test the Database connection
        objRet.setBoolean(HealthcheckHandler.FIELD_HEALTHCHECK, healthcheck);

        if (objRet.isSet(HealthcheckHandler.FIELD_HEALTHCHECK)) {
            objRet.setString(HealthcheckHandler.FIELD_COLOR, successColor);
            objRet.setString(HealthcheckHandler.FIELD_STATUS, successStatus);
        } else {
            objRet.setString(HealthcheckHandler.FIELD_COLOR, failureColor);
            objRet.setString(HealthcheckHandler.FIELD_STATUS, failureStatus);
        }

        return objRet;
    }
}
