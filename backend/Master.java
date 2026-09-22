import java.io.*;
import java.net.*;
import java.util.*;


/**
 * The Master class acts as the central coordinator 
 * It manages the cluster of workers and routes client requests based on game names.
 */

public class Master {
  private int port;

  //List with the workers' info  (ip and port)
  private List<WorkerInfo> workers;

  // counter used to tag every Search or Aggregate query with a unique ID
  private static int queryCounter = 0;

  //HashMap with pending requests
  //Key: the qID, Value: the output stream (ObjectOutputStream) to the player's or manager's connection
  public static Map<Integer, ObjectOutputStream> pendingRequests = new HashMap<>();
  
  public Master(int port, List<WorkerInfo> workers) {
    this.port = port;
    this.workers = workers;
  }

  // Synchronized method to generate a unique if for distributed queries
  public static synchronized int generateQueryId() {
    queryCounter++;
    return queryCounter;}
  
  //Worker selection with hash functionDetermines which Worker is responsible for a specific Game or Player.
  public WorkerInfo getTargetWorker(String gameName) {
    if (workers == null || workers.isEmpty()) return null;

    int hash = Math.abs(gameName.hashCode());
    int workerIndex = hash % workers.size();
    return workers.get(workerIndex);
  }

  public List<WorkerInfo> getWorkers() {
    return workers;
  }

  /**
   * Starts the Master Server and enters an infinite loop to accept new connections.
   */
  public void startServer() {
    try (ServerSocket serverSocket = new ServerSocket(port)) {
      System.out.println("Master started on port " + port);

      //Continuously accepts new connections from players and managers
      while (true) {
            // Blocking call: waits here until someone connects
            Socket clientSocket = serverSocket.accept(); 

            // MULTI-THREADING: Each connection gets its own MasterHandler thread.
            // This prevents one slow client from blocking the whole system.
            new MasterHandler(clientSocket, this).start();
}
    } catch (IOException e) {
        System.err.println("Could not start server: " + e.getMessage());
    }
    
  }
  public static void main(String[] args) {

      // Port and Worker list are loaded from a centralized Configuration class
      Master masterServer = new Master (Config.MASTER_PORT , Config.WORKERS);
      masterServer.startServer();
  }
}
