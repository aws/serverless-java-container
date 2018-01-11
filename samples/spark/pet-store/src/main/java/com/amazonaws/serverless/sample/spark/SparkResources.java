package com.amazonaws.serverless.sample.spark;


import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.sample.spark.model.Pet;
import com.amazonaws.serverless.sample.spark.model.PetData;

import javax.ws.rs.core.Response;

import java.util.UUID;

import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.post;


public class SparkResources {

    public static void defineResources() {
        before((request, response) -> response.type("application/json"));

        post("/pets", (req, res) -> {
            Pet newPet = LambdaContainerHandler.getObjectMapper().readValue(req.body(), Pet.class);
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
