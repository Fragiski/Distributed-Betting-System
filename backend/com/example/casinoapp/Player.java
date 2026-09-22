package com.example.casinoapp;
import java.io.Serializable;

public class Player implements Serializable{
    private String playerId;
    private String password;

    private String fullName;
    private String email;

    private String bDate;

    private double balance; //Available tokens
    private double totalProfitLoss; //Total profits/losses

    private static final long serialVersionUID = 2L; // unique identifier for the deserialization process

    public Player(String playerId, String password, String fullName, String email,String bDate){
        this.fullName = fullName;
        this.email = email;
        this.bDate = bDate;
        this.playerId = playerId;
        this.password = password;
        this.balance = 0.0;
        this.totalProfitLoss = 0.0;
    }

    public String getPassword() {
        return password;
    }
    
    // Adding tokens
    public synchronized void addBalance(double amount) {  // "sychronized" so that totalprofitloss is not modified at the same time
        if (amount>0) {
            this.balance += amount;
        }        
    }

    /* Updating the player's status after a bet
        betAmount: the bet amount
        winAmount: the amount the player won */
    public synchronized void recordBet (double betAmount, double winAmount) {
        double netResult = winAmount - betAmount; // Calculation of net profit
        this.balance += netResult;
        this.totalProfitLoss += netResult;
    }

    public String getPlayerId() {
        return playerId;
    }

    public double getBalance() {
        return balance;
    }

    public double getTotalProfitLoss() {
        return totalProfitLoss;
    }

    public void setFullName (String Name) {

        this.fullName = Name;
    }

    public void setEmail (String Email) {

        this.email = Email;
    }

    public void setbDate (String Date) {

        this.bDate = Date;
    }

    public String getFullName() { 

        return fullName;
    }

    public String getEmail() { 
        return email; 
    }

    public String getDate() { 
        return bDate; 
    }

    public void setPassword(String password) {
        this.password = password; 
    }

    public void setPlayerName(String playerId) {
        this.playerId = playerId;
    }


    
}
