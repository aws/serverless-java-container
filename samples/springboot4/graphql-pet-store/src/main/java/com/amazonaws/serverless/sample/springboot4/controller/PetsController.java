package com.amazonaws.serverless.sample.springboot4.controller;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import com.amazonaws.serverless.sample.springboot4.model.Owner;
import com.amazonaws.serverless.sample.springboot4.model.Pet;

@Controller
public class PetsController {
    @QueryMapping
    public Pet petById(@Argument String id) {
        return Pet.getById(id);
    }

    @SchemaMapping
    public Owner owner(Pet pet) {
        return Owner.getById(pet.ownerId());
    }
}
