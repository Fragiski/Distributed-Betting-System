package com.example.casinoapp;

import java.io.Serializable;

public class ReviewRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String playerName;
    private String gameName;
    private int stars;
    private String comment;

    public ReviewRequest(String playerName, String gameName, int stars, String comment) {
        this.playerName = playerName;
        this.gameName = gameName;
        this.stars = stars;
        this.comment = comment;
    }

    // Getters
    public String getPlayerName() { return playerName; }
    public String getGameName() { return gameName; }
    public int getStars() { return stars; }
    public String getComment() { return comment; }
}