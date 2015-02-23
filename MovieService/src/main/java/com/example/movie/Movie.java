package com.example.movie;

public class Movie {
    int id;
    String title;
    String friend;
    int rating;

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getFriend() {
        return friend;
    }

    public int getRating() {
        return rating;
    }

    public void setFriend(String friend) {
        this.friend = friend;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }
}