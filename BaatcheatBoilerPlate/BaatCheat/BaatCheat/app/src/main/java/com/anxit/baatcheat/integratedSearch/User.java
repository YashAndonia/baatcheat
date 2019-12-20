package com.anxit.baatcheat.integratedSearch;

import java.util.ArrayList;
import java.util.Map;

class User {

    private ArrayList<String> interests;
    private String name;
    private double longitude;
    private double latitude;
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


    public User(Map<String, Object> data) {

        this.interests = (ArrayList<String>) data.get("interests");
        this.name = (String) data.get("name");
        this.longitude = (double) data.get("Longitude");
        this.latitude = (double) data.get("Longitude");
        this.email=(String)data.get("email");

    }



    public ArrayList<String> getInterests() {
        return interests;
    }

    public void setInterests(ArrayList<String> interests) {
        this.interests = interests;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

}
