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
package com.amazonaws.serverless.sample.spark;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.internal.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.internal.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spark.SparkLambdaContainerHandler;
import com.amazonaws.serverless.sample.spark.model.Pet;
import com.amazonaws.serverless.sample.spark.model.PetData;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.log4j.LambdaAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.util.UUID;

import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.post;

public class LambdaHandler implements RequestHandler<AwsProxyRequest, AwsProxyResponse> {
    private ObjectMapper objectMapper = new ObjectMapper();
    private boolean isInitialized = false;
    private SparkLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;
    private Logger log = LoggerFactory.getLogger(LambdaHandler.class);

    @Override
    public AwsProxyResponse handleRequest(AwsProxyRequest awsProxyRequest, Context context) {
        if (!isInitialized) {
            isInitialized = true;
            try {
                handler = SparkLambdaContainerHandler.getAwsProxyHandler();
                defineResources();
            } catch (ContainerInitializationException e) {
                log.error("Cannot initialize Spark application", e);
                return null;
            }
        }
        return handler.proxy(awsProxyRequest, context);
    }

    private void defineResources() {
        before((request, response) -> response.type("application/json"));

        post("/pets", (req, res) -> {
            Pet newPet = objectMapper.readValue(req.body(), Pet.class);
            if (newPet.getName() == null || newPet.getBreed() == null) {
                return Response.status(400).entity(new Error("Invalid name or breed")).build();
            }

            Pet dbPet = newPet;
            dbPet.setId(UUID.randomUUID().toString());

            res.status(200);
            return dbPet;
        }, new JsonTransformer());

        get("/pets", (req, res) -> {
            int limit = 10;
            if (req.queryParams("limit") != null) {
                limit = Integer.parseInt(req.queryParams("limit"));
            }

            Pet[] outputPets = new Pet[limit];

            for (int i = 0; i < limit; i++) {
                Pet newPet = new Pet();
                newPet.setId(UUID.randomUUID().toString());
                newPet.setName(PetData.getRandomName());
                newPet.setBreed(PetData.getRandomBreed());
                newPet.setDateOfBirth(PetData.getRandomDoB());
                outputPets[i] = newPet;
            }

            res.status(200);
            return outputPets;
        }, new JsonTransformer());

        get("/pets/:petId", (req, res) -> {
            Pet newPet = new Pet();
            newPet.setId(UUID.randomUUID().toString());
            newPet.setBreed(PetData.getRandomBreed());
            newPet.setDateOfBirth(PetData.getRandomDoB());
            newPet.setName(PetData.getRandomName());
            res.status(200);
            return newPet;
        }, new JsonTransformer());
    }
}