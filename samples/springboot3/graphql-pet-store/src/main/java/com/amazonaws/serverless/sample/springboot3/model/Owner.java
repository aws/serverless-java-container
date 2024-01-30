package com.amazonaws.serverless.sample.springboot3.model;

import java.util.Arrays;
import java.util.List;

public record Owner (String id, String firstName, String lastName) {

    private static List<Owner> owners = Arrays.asList(
            new Owner("owner-1", "Joshua", "Bloch"),
            new Owner("owner-2", "Douglas", "Adams"),
            new Owner("owner-3", "Bill", "Bryson")
    );

    public static Owner getById(String id) {
        return owners.stream()
                .filter(owner -> owner.id().equals(id))
                .findFirst()
                .orElse(null);
    }
}
