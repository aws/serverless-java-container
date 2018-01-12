/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.amazonaws.serverless.proxy.jersey;

import com.amazonaws.serverless.proxy.RequestReader;
import com.amazonaws.serverless.proxy.model.ApiGatewayRequestContext;
import com.amazonaws.serverless.proxy.jersey.model.MapResponseModel;
import com.amazonaws.serverless.proxy.jersey.model.SingleValueModel;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Enumeration;
import java.util.Random;

/**
 * Jersey application class for aws-serverless-java-container unit proxy
 */
@Path("/echo")
public class EchoJerseyResource {

    public static final String EXCEPTION_MESSAGE = "Fake exception";

    @Path("/headers") @GET
    @Produces(MediaType.APPLICATION_JSON)
    public MapResponseModel echoHeaders(@Context ContainerRequestContext context) {
        MapResponseModel headers = new MapResponseModel();
        for (String key : context.getHeaders().keySet()) {
            headers.addValue(key, context.getHeaderString(key));
        }

        return headers;
    }

    @Path("/servlet-headers") @GET
    @Produces(MediaType.APPLICATION_JSON)
    public MapResponseModel echoServletHeaders(@Context HttpServletRequest context) {
        MapResponseModel headers = new MapResponseModel();
        Enumeration<String> headerNames = context.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.addValue(headerName, context.getHeader(headerName));
        }
        return headers;
    }

    @Path("/servlet-context") @GET
    @Produces(MediaType.APPLICATION_JSON)
    public SingleValueModel echoContextInformation(@Context ServletContext context) {
        SingleValueModel singleValueModel = new SingleValueModel();
        singleValueModel.setValue(context.getServerInfo());

        return singleValueModel;
    }

    @Path("/query-string") @GET
    @Produces(MediaType.APPLICATION_JSON)
    public MapResponseModel echoQueryString(@Context UriInfo context) {
        MapResponseModel queryStrings = new MapResponseModel();
        for (String key : context.getQueryParameters().keySet()) {
            queryStrings.addValue(key, context.getQueryParameters().getFirst(key));
        }

        return queryStrings;
    }

    @Path("/authorizer-principal") @GET
    @Produces(MediaType.APPLICATION_JSON)
    public SingleValueModel echoAuthorizerPrincipal(@Context ContainerRequestContext context) {
        SingleValueModel valueModel = new SingleValueModel();
        ApiGatewayRequestContext apiGatewayRequestContext =
                (ApiGatewayRequestContext) context.getProperty(RequestReader.API_GATEWAY_CONTEXT_PROPERTY);
        valueModel.setValue(apiGatewayRequestContext.getAuthorizer().getPrincipalId());

        return valueModel;
    }

    @Path("/authorizer-context") @GET
    @Produces(MediaType.APPLICATION_JSON)
    public SingleValueModel echoAuthorizerContext(@Context ContainerRequestContext context, @QueryParam("key") String key) {
        SingleValueModel valueModel = new SingleValueModel();
        ApiGatewayRequestContext apiGatewayRequestContext =
                (ApiGatewayRequestContext) context.getProperty(RequestReader.API_GATEWAY_CONTEXT_PROPERTY);
        valueModel.setValue(apiGatewayRequestContext.getAuthorizer().getContextValue(key));

        return valueModel;
    }

    @Path("/json-body") @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public SingleValueModel echoJsonValue(final SingleValueModel requestValue) {
        SingleValueModel output = new SingleValueModel();
        output.setValue(requestValue.getValue());

        return output;
    }

    @Path("/status-code") @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response echoCustomStatusCode(@QueryParam("status") int statusCode ) {
        SingleValueModel output = new SingleValueModel();
        output.setValue("" + statusCode);

        return Response.status(statusCode).entity(output).build();
    }

    @Path("/binary") @GET
    @Produces("application/octet-stream")
    public Response echoBinaryData() {
        byte[] b = new byte[128];
        new Random().nextBytes(b);

        return Response.ok(b).build();
    }

    @Path("/exception") @GET
    public Response throwException() {
        throw new UnsupportedOperationException(EXCEPTION_MESSAGE);
    }
}
