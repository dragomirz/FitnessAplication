package com.fitness.fitnessapplication.DataModels;

public class User {
    private String email;
    private String password;
    private String name;
    private String height;
    private String gender;
    private String weight;

    public User(String email, String password, String name, String height, String gender, String weight) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.height = height;
        this.gender = gender;
        this.weight = weight;
    }

    public User(String email, String password){
        this.email = email;
        this.password = password;
    }
}