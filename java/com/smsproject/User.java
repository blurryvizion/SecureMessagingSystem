package com.smsproject;

public class User 
{
    private String username;
    private String password;

    //user constructor
    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    //getters for username and password
    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
    
}
