package com.amazonaws.serverless.runtime;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

import java.time.Instant;

public class InvocationContext implements Context {
    private static final String FUNCTION_NAME_ENV_VAR = "AWS_LAMBDA_FUNCTION_NAME";
    private static final String FUNCTION_LOG_STREAM_ENV_VAR = "AWS_LAMBDA_LOG_STREAM_NAME";
    private static final String FUNCTION_LOG_GROUP_ENV_VAR = "AWS_LAMBDA_LOG_GROUP_NAME";
    private static final String FUNCTION_VERSION_ENV_VAR = "AWS_LAMBDA_FUNCTION_VERSION";
    private static final String FUNCTION_MEMORY_ENV_VAR = "AWS_LAMBDA_FUNCTION_MEMORY_SIZE";

    private static String functionName;
    private static String logGroupName;
    private static String logStreamName;
    private static String functionVersion;
    private static int memoryLimitMb;

    public static void prepareContext() {
        functionName = System.getenv(FUNCTION_NAME_ENV_VAR);
        logGroupName = System.getenv(FUNCTION_LOG_GROUP_ENV_VAR);
        logStreamName = System.getenv(FUNCTION_LOG_STREAM_ENV_VAR);
        functionVersion = System.getenv(FUNCTION_VERSION_ENV_VAR);
        memoryLimitMb = Integer.parseInt(System.getenv(FUNCTION_MEMORY_ENV_VAR));
    }

    private String awsRequestId;
    private long deadlineMs;
    private String functionArn;
    private String traceId;

    public InvocationContext(String reqId, long deadline, String arn, String trace) {
        awsRequestId = reqId;
        deadlineMs = deadline;
        functionArn = arn;
        traceId = trace;
    }

    @Override
    public String getAwsRequestId() {
        return awsRequestId;
    }

    @Override
    public String getLogGroupName() {
        return logGroupName;
    }

    @Override
    public String getLogStreamName() {
        return logStreamName;
    }

    @Override
    public String getFunctionName() {
        return functionName;
    }

    @Override
    public String getFunctionVersion() {
        return functionVersion;
    }

    @Override
    public String getInvokedFunctionArn() {
        return functionArn;
    }

    @Override
    public CognitoIdentity getIdentity() {
        return null;
    }

    @Override
    public ClientContext getClientContext() {
        return null;
    }

    @Override
    public int getRemainingTimeInMillis() {
        return Math.toIntExact(deadlineMs - Instant.now().toEpochMilli());
    }

    @Override
    public int getMemoryLimitInMB() {
        return memoryLimitMb;
    }

    @Override
    public LambdaLogger getLogger() {
        return null;
    }

    public String getTraceId() {
        return traceId;
    }
}
