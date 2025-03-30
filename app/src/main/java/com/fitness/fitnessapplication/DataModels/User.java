package com.fitness.fitnessapplication.DataModels;

public class User {
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public float getCalories() {
        return calories;
    }

    public void setCalories(float calories) {
        this.calories = calories;
    }

    private String email;
    private String password;
    private String name;
    private String height;
    private String gender;
    private String weight;

    private int age;
    private float calories;

    public User(String email, String password, String name, String height, String gender, String weight, int age, float calories) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.height = height;
        this.gender = gender;
        this.weight = weight;
        this.age = age;
        this.calories = calories;

    }

    public User(String email, String password){
        this.email = email;
        this.password = password;
    }
}