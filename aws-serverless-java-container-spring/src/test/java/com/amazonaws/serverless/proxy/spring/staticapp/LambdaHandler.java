 package com.amazonaws.serverless.proxy.spring.staticapp;


 import com.amazonaws.serverless.exceptions.ContainerInitializationException;
 import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
 import com.amazonaws.serverless.proxy.spring.SpringLambdaContainerHandler;
 import com.amazonaws.services.lambda.runtime.Context;
 import com.amazonaws.services.lambda.runtime.RequestHandler;

 import com.amazonaws.services.lambda.runtime.events.apigateway.APIGatewayProxyRequestEvent;
 import org.springframework.web.context.support.XmlWebApplicationContext;


 public class LambdaHandler
   implements RequestHandler<APIGatewayProxyRequestEvent, AwsProxyResponse>
 {
   SpringLambdaContainerHandler<APIGatewayProxyRequestEvent, AwsProxyResponse> handler;
   boolean isinitialized = false;
 
   public AwsProxyResponse handleRequest(APIGatewayProxyRequestEvent awsProxyRequest, Context context)
   {
        if (!isinitialized) {
            isinitialized = true;
            try {
            	XmlWebApplicationContext wc = new XmlWebApplicationContext();
            	wc.setConfigLocation("classpath:/staticAppContext.xml");
                handler = SpringLambdaContainerHandler.getAwsProxyHandler(wc);
            } catch (ContainerInitializationException e) {
                e.printStackTrace();
                return null;
            }
        }
        AwsProxyResponse res = handler.proxy(awsProxyRequest, context);
        return res;
   }
 }

