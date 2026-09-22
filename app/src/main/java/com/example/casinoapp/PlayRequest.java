package com.example.casinoapp;

import java.io.Serializable;

public class PlayRequest implements Serializable {

    // serialVersionUID: A number like an ID so that Java knows
    // that the class is the same on both sides of the Socket.
    private static final long serialVersionUID = 1L;

    private String gameName;  //The name of the game chosen by the player
    private double betAmount; //The amount he bets (e.g. 10.5 FUN)
    private String playerName;

    public PlayRequest(String gameName, double betAmount, String playerName) {
        this.gameName = gameName;
        this.betAmount = betAmount;
        this.playerName = playerName;
    }


    public String getGameName() {
        return gameName;
    }

    public double getBetAmount() {
        return betAmount;
    }

    public String getPlayerName() {
        return playerName;
    }

}
