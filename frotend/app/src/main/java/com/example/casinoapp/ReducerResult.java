package com.example.casinoapp;

import java.io.Serializable;
import java.util.List;

/**
 * ReducerResult is a Data Transfer Object (DTO) that carries the finalized
 * MapReduce results from the Reducer back to the Master.
 */

public class ReducerResult implements Serializable {

    // The serialVersionUID ensures that Master and Reducer "speak" the same version of the class
    private static final long serialVersionUID = 1L;

    private int queryId;
    private List<Game> finalGames; // for search queries
    private double totalAmount;    // for profit/losses queries
    private String resultType;     // query type ("GAMES" vs "AGGREGATE")


    /**
     * Constructor for SEARCH results.
     * Used when the Reducer has merged lists of games from all Workers.
     */
    public ReducerResult(int queryId, List<Game> finalGames) {
        this.queryId = queryId;
        this.finalGames = finalGames;
        this.resultType = "GAMES";
        this.totalAmount = 0.0;
    }

    /**
     * Constructor for AGGREGATE results.
     * Used when the Reducer has summed up profit/loss numbers from all Workers.
     */
    public ReducerResult(int queryId, double totalAmount) {
        this.totalAmount = totalAmount;
        this.queryId = queryId;
        this.resultType = "AGGREGATE";
        this.finalGames = null;
    }

    // Getters for the master to read the context
    public int getQueryId() {
        return queryId;
    }

    public List<Game> getFinalGames() {
        return finalGames;
    }

    public double getTotalAmount(){
        return totalAmount;
    }

    public String getResultType() {
        return resultType;
    }
}