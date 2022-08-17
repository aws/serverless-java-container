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
package com.amazonaws.serverless.sample.struts.actions;

import com.amazonaws.serverless.sample.struts.model.Pet;
import com.amazonaws.serverless.sample.struts.model.PetData;
import com.opensymphony.xwork2.ModelDriven;
import org.apache.struts2.rest.DefaultHttpHeaders;
import org.apache.struts2.rest.HttpHeaders;
import org.apache.struts2.rest.RestActionSupport;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;


public class PetsController extends RestActionSupport implements ModelDriven<Object> {

    private Pet model = new Pet();
    private String id;
    private Collection<Pet> list = null;

    // GET /pets/1
    public HttpHeaders show() {
        return new DefaultHttpHeaders("show");
    }

    // GET /pets
    public HttpHeaders index() {
        list = PetData.getNames()
                .stream()
                .map(petName -> new Pet(
                        UUID.randomUUID()
                                .toString(), PetData.getRandomBreed(), petName, PetData.getRandomDoB()))
                .collect(Collectors.toList());
        return new DefaultHttpHeaders("index")
                .disableCaching();
    }

    // POST /pets
    public HttpHeaders create() {
        if (model.getName() == null || model.getBreed() == null) {
            return null;
        }

        Pet dbPet = model;
        dbPet.setId(UUID.randomUUID().toString());
        return new DefaultHttpHeaders("success")
                .setLocationId(model.getId());

    }

    // PUT /pets/1
    public String update() {
        //TODO: UPDATE LOGIC
        return SUCCESS;
    }

    // DELETE /petsr/1
    public String destroy() {
        //TODO: DELETE LOGIC
        return SUCCESS;
    }

    public void setId(String id) {
        if (id != null) {
            this.model = new Pet(id, PetData.getRandomBreed(), PetData.getRandomName(), PetData.getRandomDoB());
        }
        this.id = id;
    }

    public Object getModel() {
        if (list != null) {
            return list;
        } else {
            if (model == null) {
                model = new Pet();
            }
            return model;
        }
    }
}
