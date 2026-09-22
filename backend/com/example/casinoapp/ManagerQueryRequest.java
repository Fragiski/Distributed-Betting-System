package com.example.casinoapp;
//So that the Master sends a packet to his workers. This way they know his exact request
import java.io.Serializable;

public class ManagerQueryRequest implements Serializable{
    //Unique version identifier of the class for secure transfer of the object over the network (Serialization/Deserialization). 
    // It ensures that sender and receiver "talk" about the exact same form of the class.
    private static final long serialVersionUID = 1l; 
    
    private int queryId;
    private String queryType; // Provider or Player
    private String targetName;//e.g. user123 or NetEnt

    public ManagerQueryRequest(int queryId, String queryType, String targetName) {
        this.queryId = queryId;
        this.queryType = queryType;
        this.targetName = targetName;
    }

    public int getQueryId() { return queryId; }
    public String getQueryType() { return queryType; }
    public String getTargetName() { return targetName; }
}
