package com.amazonaws.serverless.sample.springboot3.model;

import java.util.Arrays;
import java.util.List;

public record Pet (String id, String name, String breed, String ownerId) {

    private static List<Pet> pets = Arrays.asList(
            new Pet("pet-1", "Alpha", "Bulldog", "owner-1"),
            new Pet("pet-2", "Max", "German Shepherd", "owner-2"),
            new Pet("pet-3", "Rockie", "Golden Retriever", "owner-3")
    );

    public static Pet getById(String id) {
        return pets.stream()
                .filter(pet -> pet.id().equals(id))
                .findFirst()
                .orElse(null);
    }
}
