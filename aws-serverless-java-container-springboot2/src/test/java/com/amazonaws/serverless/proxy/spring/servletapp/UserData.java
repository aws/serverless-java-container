package com.amazonaws.serverless.proxy.spring.servletapp;


import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class UserData {
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    @NotNull @Email
    private String email;
    private String error;

    public UserData() {

    }

    public UserData(String err) {
        error = err;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getError() { return error; }
}
