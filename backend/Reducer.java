import java.io.*;
import java.net.*;
import java.util.*;
import com.example.casinoapp.Game;
import com.example.casinoapp.ReducerResult;

/**
 * The Reducer class implements the "Reduce" part of MapReduce.
 * It stays idle until Workers send their partial results, then aggregates them.
 */

public class Reducer {

private int port;
private int numberOfWorkers;

// for temporary storing the results coming from the workers
private List<Game> finalResults = new ArrayList<>(); 
  private double totalAggregateSum = 0.0;

  public Reducer(int port,int numberOfWorkers) {
    this.port = port;
    this.numberOfWorkers = numberOfWorkers;
  }

  public void startServer() {
      try(ServerSocket serverSocket = new ServerSocket(port)){
        System.out.println("\nReducer is waiting for Workers on port " + port);

      // Outer loop: Keeps the Reducer alive to handle multiple queries over time
      while (true) { 
        int receivedCount = 0;
        int currentQueryId = -1;

        // This flag tracks if we are dealing with a List of Games (Search) 
        // or a single Number (Aggregate/Profit calculation).
        boolean isSearchQuery = false; 
            
        while(receivedCount < numberOfWorkers) { //waits to collect responses from all Workers in the system
            System.out.println("Reducer: Waiting for Worker " + (receivedCount + 1) + "/" + numberOfWorkers + "...");
            
            Socket socket = serverSocket.accept(); // accepts for every connection
            System.out.println("Reducer: Worker connected!");

              //Each worker sends a list of games he found
              try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())){

                // Reads the instruction from the Worker
                String requestType = (String) in.readObject();

                if ("REDUCE_SEARCH".equals(requestType)){
                  // Logic for "Find games matching X"
                  currentQueryId = (int) in.readObject();//Storing the search ID sent to by the worker
                  List<Game> workerResults = (List<Game>) in.readObject();
                  isSearchQuery = true;

                  synchronized(finalResults) {
                    finalResults.addAll(workerResults); // Merging this worker's list into global list
                  }

                  System.out.println("Reducer: Received " + workerResults.size() + " games for Query ID: " + currentQueryId);

                } else if ("REDUCE_AGGREGATE".equals(requestType)) {
                  // Logic for calculation total profit for a provider 
                    currentQueryId = (int) in.readObject();
                    double workerSum = in.readDouble();
                    isSearchQuery = false;

                    synchronized (this) {
                        totalAggregateSum += workerSum; // adds this worker's partial sum to our global total
                    }
                    System.out.println("Reducer: Received aggregate sum " + workerSum + " for Query ID: " + currentQueryId);
                }

                // Acknowledging worker so it can close its connection
                out.writeObject("OK");
                out.flush();

              } catch (Exception e) {
                System.err.println("\nError reading from Worker: " + e.getMessage());
                
              } finally {
                socket.close();
                receivedCount++; // incrementing count even on error to avoid infinite waiting
              } 
            }
            
            System.out.println("Reducer: All workers responded. Sending results to Master...");

            if (isSearchQuery) { // Checking the TYPE of the query
                sendToMaster(Config.MASTER_IP, Config.MASTER_PORT, new ReducerResult(currentQueryId, new ArrayList<>(finalResults))); // 'search' query
            } else {
                sendToMaster(Config.MASTER_IP, Config.MASTER_PORT, new ReducerResult(currentQueryId, totalAggregateSum)); // 'profits' query
            }

            // Cleanup to be ready for the next query from the Master
            finalResults.clear();
            totalAggregateSum = 0.0;
        } 
      } catch (Exception e) {
      }  }


    /**
    * Connects back to the Master to deliver the final answer.
    */
    private void sendToMaster(String masterIP, int masterPort, ReducerResult resultPacket) {
      //Opens a new TCP connection to the Master
      try (Socket socket = new Socket(masterIP, masterPort);
          ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
          ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
        //Sending the following text so that the master understands that it is the reducer
        out.writeObject("REDUCER_RESULT");

        //Sending the finalized packet (id,data)
        out.writeObject(resultPacket);
        out.flush();

        // Waits for Master to confirm receipt
        in.readObject();

        System.out.println("\nReducer:Final results sent to Master.");

        
        }catch (Exception e) {
            System.err.println("\nError sending from Master to Reducer: " + e.getMessage());
            e.printStackTrace();
        }

    }
    public static void main(String[] args) {
        // Reducer on port 8000, waiting for a response from 1 Worker
        Reducer reducerServer = new Reducer(Config.REDUCER_PORT, Config.WORKERS.size()); 
        reducerServer.startServer();
    }

  }



