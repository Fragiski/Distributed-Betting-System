package com.example.casinoapp;
import java.io.Serializable;

public class SearchRequest implements Serializable {
    private static final long serialVersionUID = 1L; // For Serialization
    private int queryId;
    private SearchFilters filters;

    public SearchRequest(int queryId, SearchFilters filters) {
        this.queryId = queryId;
        this.filters = filters;
    }

    public int getQueryId() { return queryId; }
    public SearchFilters getFilters() { return filters; }
}