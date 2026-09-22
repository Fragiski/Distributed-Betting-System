package com.example.casinoapp;
import java.io.Serializable;

public class Game implements Serializable {
    //for JSON files 
    private String gameName;
    private String providerName;
    private int stars;
    private int noOfVotes;
    private String gameLogo;
    private double minBet;
    private double maxBet;
    private String riskLevel;
    private String hashKey; //secret code S (encryption)

    //NOT in JSON files, automatically calculated
    private String betCategory;
    private int jackpot;

    private Boolean isActive; //For soft-deleting when Manager temporalily removes a game
    private double totalProfitLoss; // Profits/Losses


    public Game(String gameName, String providerName, int stars, int noOfVotes, String gameLogo, double minBet, double maxBet, String riskLevel, String hashKey) {
        this.gameName = gameName;
        this.providerName = providerName;
        this.stars = stars;
        this.noOfVotes = noOfVotes;
        this.gameLogo = gameLogo;
        this.minBet = minBet;
        this.maxBet = maxBet;
        this.riskLevel = riskLevel;
        this.hashKey = hashKey;

        this.isActive = true;
        this.totalProfitLoss = 0.0;

        calculateBetCategory();
        calculateJackpot();
    }

    //Calculating BetCategory based on minBet
    private void calculateBetCategory() {
        if (this.minBet >= 5.0) {
            this.betCategory = "$$$"; //Minimal 5 FUN -> $$$
        } else if (this.minBet >= 1.0) {
            this.betCategory = "$$";
        } else {
            this.betCategory = "$";
        }
    }

    //Calculating category based on risk-level
    public void calculateJackpot() {
        switch (this.riskLevel) {
            case "Low":
                this.jackpot = 10;
                break;
            case "Medium":
                this.jackpot = 20;
                break;
            case "High":
                this.jackpot = 40;
                break;
            default:
                this.jackpot = 0;
        }
    }

    public String getProviderName() {
        return providerName;
    }

    public double getTotalProfitLoss() {
        return totalProfitLoss;
    }

    public void markAsDeleted(){
        this.isActive = false; //Removing the game
    }

    public void updateProfitLoss(double amount) {
        this.totalProfitLoss += amount;
    }

    public String getGameName() {
        return gameName;
    }

    public int getJackpot() {
        return jackpot;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;

        this.calculateJackpot();
    }

    public String getHashKey() {
       return hashKey;
    }

    public boolean isActive() {
        return this.isActive;
    }

    public int getStars() {
       return stars;
    }

    public String getBetCategory() {
        return betCategory;
    }

    public String getLogo() {
        return gameLogo;
    }

    public void setActive(boolean active) { // for soft-delete (manager)
        this.isActive = active;
    }

    public void addRating (int newRating) {
        if (newRating >= 1 && newRating <= 5) {

            //Calculate (stars * numOfVotes)
            double totalScore = (this.stars * this.noOfVotes);
            
            totalScore += newRating;
            this.noOfVotes++;

            this.stars = (int) Math.round(totalScore / this.noOfVotes);
        }
    }

    public void setMinBet(double minBet) {
        this.minBet = minBet;
    }

    public void setMaxBet(double maxBet) {
        this.maxBet = maxBet;
    }

    public double getMinBet() {
        return minBet;
    }

    public double getMaxBet() {
        return maxBet;
    }
}

