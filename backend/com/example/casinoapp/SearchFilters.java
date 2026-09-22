package com.example.casinoapp;
// For searching games

import java.io.Serializable;

public class SearchFilters implements Serializable {
        private String betCategory; //e.g. "$", "$$", "$$$"
        private int stars;          //e.g. 1 to 5
        private String riskLevel;   //e.g. "low", "medium", "high"

        public SearchFilters(String betCategory, int stars, String riskLevel) {
            this.betCategory = betCategory;
            this.stars = stars;
            this.riskLevel = riskLevel;
        }
        //Getters
        public String getBetCategory() { return betCategory; }
        public int getStars() { return stars; }
        public String getRiskLevel() { return riskLevel; }
    }