package com.amazonaws.micronaut.demo;

import com.amazonaws.micronaut.demo.model.Pet;
import com.amazonaws.micronaut.demo.model.PetData;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.text.SimpleDateFormat;

@Configuration
public class PetStoreConfig {
    SimpleDateFormat dateFormatter = new SimpleDateFormat("YYYY-mm-dd");
    @Bean
    public PetData getPetData() {
        return new PetData();
    }

    @Bean
    public JsonSerializer<Pet> serializer() {
        return new JsonSerializer<Pet>() {
            @Override
            public void serialize(Pet pet, JsonGenerator generator, SerializerProvider provider) throws IOException {
                generator.writeStartObject();
                generator.writeFieldName("petId");
                generator.writeString(pet.getId());
                generator.writeFieldName("name");
                generator.writeString(pet.getName());
                generator.writeFieldName("breed");
                generator.writeString(pet.getBreed());
                generator.writeFieldName("dateOfBirth");
                generator.writeString(dateFormatter.format(pet.getDateOfBirth()));
                generator.writeEndObject();
            }
        };
    }
}
