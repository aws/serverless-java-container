package com.amazonaws.serverless.proxy.model;


/***
 * Context passed by ALB proxy events
 */
public class AlbContext {
    private String targetGroupArn;


    public String getTargetGroupArn() {
        return targetGroupArn;
    }


    public void setTargetGroupArn(String targetGroupArn) {
        this.targetGroupArn = targetGroupArn;
    }
}
