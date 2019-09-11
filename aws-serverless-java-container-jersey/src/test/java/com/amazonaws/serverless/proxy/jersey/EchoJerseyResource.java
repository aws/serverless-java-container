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
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletRequest;
import com.amazonaws.serverless.proxy.jersey.providers.ServletRequestFilter;
import com.amazonaws.serverless.proxy.model.AwsProxyRequestContext;
import com.amazonaws.serverless.proxy.jersey.model.MapResponseModel;
import com.amazonaws.serverless.proxy.jersey.model.SingleValueModel;

import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static com.amazonaws.serverless.proxy.jersey.JerseyHandlerFilter.JERSEY_SERVLET_REQUEST_PROPERTY;


/**
 * Jersey application class for aws-serverless-java-container unit proxy
 */
@Path("/echo")
public class EchoJerseyResource {
    public static final String SERVLET_RESP_HEADER_KEY = "X-HttpServletResponse";
    public static final String EXCEPTION_MESSAGE = "Fake exception";

    @Context
    SecurityContext securityCtx;

    @Inject
    JerseyDependency jerseyDependency;

    @Path("/decoded-param") @GET
    @Produces(MediaType.APPLICATION_JSON)
    public SingleValueModel echoDecodedParam(@QueryParam("param") String param) {
        SingleValueModel model = new SingleValueModel();
        model.setValue(param);
        return model;
    }

    @Path("/filter-attribute") @GET
    @Produces(MediaType.APPLICATION_JSON)
    public SingleValueModel returnFilterAttribute(@Context HttpServletRequest req) {
        SingleValueModel model = new SingleValueModel();
        if (req.getAttribute(ServletRequestFilter.FILTER_ATTRIBUTE_NAME) == null) {
            model.setValue("");
        } else {
            model.setValue(req.getAttribute(ServletRequestFilter.FILTER_ATTRIBUTE_NAME).toString());
        }
        return model;
    }

    @Path("/list-query-string") @GET
    @Produces(MediaType.APPLICATION_JSON)
    public SingleValueModel echoQueryStringLength(@QueryParam("list") List<String> param) {
        SingleValueModel model = new SingleValueModel();
        model.setValue(param.size() + "");
        return model;
    }

    @Path("/encoded-param") @GET
    @Produces(MediaType.APPLICATION_JSON)
    public SingleValueModel echoEncodedParam(@QueryParam("param") @Encoded String param) {
        SingleValueModel model = new SingleValueModel();
        model.setValue(param);
        return model;
    }

    @Path("/headers") @GET
    @Produces(MediaType.APPLICATION_JSON)
    public MapResponseModel echoHeaders(@Context ContainerRequestContext context) {
        MapResponseModel headers = new MapResponseModel();
        for (String key : context.getHeaders().keySet()) {
            headers.addValue(key, context.getHeaderString(key));
        }

        return headers;
    }

    @Path("/security-context") @GET
    @Produces(MediaType.APPLICATION_JSON)
    public SingleValueModel getPrincipal() {
        SingleValueModel output = new SingleValueModel();
        if (securityCtx != null) {
            output.setValue(securityCtx.getUserPrincipal().getName());
        }
        return output;
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

    @Path("/scheme") @GET
    @Produces(MediaType.APPLICATION_JSON)
    public SingleValueModel echoRequestScheme(@Context UriInfo context) {
        SingleValueModel model = new SingleValueModel();
        model.setValue(context.getRequestUri().getScheme());
        return model;
    }

    @Path("/authorizer-principal") @GET
    @Produces(MediaType.APPLICATION_JSON)
    public SingleValueModel echoAuthorizerPrincipal(@Context ContainerRequestContext context) {
        SingleValueModel valueModel = new SingleValueModel();
        AwsProxyRequestContext awsProxyRequestContext =
                (AwsProxyRequestContext) context.getProperty(RequestReader.API_GATEWAY_CONTEXT_PROPERTY);
        valueModel.setValue(awsProxyRequestContext.getAuthorizer().getPrincipalId());

        return valueModel;
    }

    @Path("/authorizer-context") @GET
    @Produces(MediaType.APPLICATION_JSON)
    public SingleValueModel echoAuthorizerContext(@Context ContainerRequestContext context, @QueryParam("key") String key) {
        SingleValueModel valueModel = new SingleValueModel();
        AwsProxyRequestContext awsProxyRequestContext =
                (AwsProxyRequestContext) context.getProperty(RequestReader.API_GATEWAY_CONTEXT_PROPERTY);
        valueModel.setValue(awsProxyRequestContext.getAuthorizer().getContextValue(key));

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

    @Path("/servlet-response") @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response echoCustomStatusCode(@Context HttpServletResponse resp) {
        SingleValueModel output = new SingleValueModel();
        output.setValue("Custom header in resp");
        resp.setHeader(SERVLET_RESP_HEADER_KEY, "1");
        return Response.ok().entity(output).build();
    }

    @Path("/binary") @GET
    @Produces("application/octet-stream")
    public Response echoBinaryData() {
        byte[] b = new byte[128];
        new Random().nextBytes(b);

        return Response.ok(b).build();
    }

    @Path("/empty-stream/{paramId}/test/{param2}") @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response emptyStream(@PathParam("paramId") String paramId, @PathParam("param2") String param2) {
        SingleValueModel sv = new SingleValueModel();
        sv.setValue(paramId);
        return Response.ok(sv).build();
    }

    @Path("/exception") @GET
    public Response throwException() {
        throw new UnsupportedOperationException(EXCEPTION_MESSAGE);
    }

    @Path("/encoded-path/{resource}") @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response encodedPathParam(@Encoded @PathParam("resource") String resource) {
        SingleValueModel sv = new SingleValueModel();
        sv.setValue(resource);
        return Response.ok(sv).build();
    }

    @Path("/referer-header") @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response referer(@HeaderParam("Referer") String referer) {
        SingleValueModel sv = new SingleValueModel();
        sv.setValue(referer);
        return Response.ok(sv).build();
    }

    @Path("/file-size") @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response fileSize(@FormDataParam("file") final File uploadedFile,
                             @FormDataParam("file") FormDataContentDisposition fileDetail,
                             @Context ContainerRequestContext req) {
        SingleValueModel sv = new SingleValueModel();

        try {
            InputStream fileIs = new FileInputStream(uploadedFile);
            System.out.println("File: " + fileDetail.getName() + " " + fileDetail.getFileName() + " " + fileDetail.getSize());
            System.out.println("Size: " + fileIs.available());
            sv.setValue("" + fileIs.available());
            return Response.ok(sv).build();
        } catch (IOException e) {
            e.printStackTrace();
            return Response.status(500).build();
        }
    }

    @Path("/plain") @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response plain() {
        return Response.status(200).entity("Hello!").build();
    }
}
