package com.amazonaws.serverless.sample.quarkus;

import com.amazonaws.serverless.sample.quarkus.model.Pet;
import com.amazonaws.serverless.sample.quarkus.model.PetData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
public class PetsController {

    private PetData petData;

    @Autowired
    public PetsController(PetData data) {
        petData = data;
    }

    @RequestMapping(path = "/pets", method = RequestMethod.POST)
    public Pet createPet(@RequestBody Pet newPet) {
        if (newPet.getName() == null || newPet.getBreed() == null) {
            return null;
        }

        Pet dbPet = newPet;
        dbPet.setId(UUID.randomUUID().toString());
        return dbPet;
    }

    @RequestMapping(path = "/pets", method = RequestMethod.GET)
    public Pet[] listPets(@RequestParam("limit") Optional<Integer> limit) {
        int queryLimit = 10;
        if (limit.isPresent()) {
            queryLimit = limit.get();
        }

        Pet[] outputPets = new Pet[queryLimit];

        for (int i = 0; i < queryLimit; i++) {
            Pet newPet = new Pet();
            newPet.setId(UUID.randomUUID().toString());
            newPet.setName(petData.getRandomName());
            newPet.setBreed(petData.getRandomBreed());
            newPet.setDateOfBirth(petData.getRandomDoB());
            outputPets[i] = newPet;
        }

        return outputPets;
    }

    @RequestMapping(path = "/pets/{petId}", method = RequestMethod.GET)
    public Pet getPet(@RequestParam("petId") String petId) {
        Pet newPet = new Pet();
        newPet.setId(UUID.randomUUID().toString());
        newPet.setBreed(petData.getRandomBreed());
        newPet.setDateOfBirth(petData.getRandomDoB());
        newPet.setName(petData.getRandomName());
        return newPet;
    }

}
