package com.amazonaws.serverless.proxy.spring;

import com.amazonaws.serverless.proxy.AwsProxyExceptionHandler;
import com.amazonaws.serverless.proxy.ExceptionHandler;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import org.springframework.web.ErrorResponse;

/**
 * This ExceptionHandler implementation enhances the standard AwsProxyExceptionHandler
 * by mapping additional details from org.springframework.web.ErrorResponse
 *
 * As of now this class is identical with SpringAwsProxyExceptionHandler. We may consider
 * moving it to a common module to share it in the future.
 */
public class SpringBootAwsProxyExceptionHandler extends AwsProxyExceptionHandler
        implements ExceptionHandler<AwsProxyResponse> {
    @Override
    public AwsProxyResponse handle(Throwable ex) {
        if (ex instanceof ErrorResponse) {
            return new AwsProxyResponse(((ErrorResponse) ex).getStatusCode().value(),
                    HEADERS, getErrorJson(ex.getMessage()));
        } else {
            return super.handle(ex);
        }
    }

}
