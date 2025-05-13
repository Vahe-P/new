package com.example.anew;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Place_2 {
    private String name;
    private String imageUrl;
    private double lat;
    private double lng;
    private String distance;
    private String id;
    String category;

    public Place_2( String name, String imageUrl, double lat, double lng, String distance,String category) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.lat = lat;
        this.lng = lng;
        this.distance = distance;
        this.id = generateId(name, lat, lng);
        this.category=category;
    }
    private String generateId(String name, double lat, double lng) {
        String input = name + lat + lng; // Combine attributes to create a unique string
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error generating unique ID", e);
        }
    }


    public String getName() {
        return name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public String getDistance() {
        return distance;
    }
    public String getId() {
        return id;
    }
    public String getCategory() {
        return category;
    }
}