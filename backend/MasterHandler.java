import com.example.casinoapp.Game;
import com.example.casinoapp.ManagerQueryRequest;
import com.example.casinoapp.PlayRequest;
import com.example.casinoapp.Player;
import com.example.casinoapp.ReducerResult;
import com.example.casinoapp.SearchFilters;
import com.example.casinoapp.SearchRequest;
import java.io.*;
import java.net.*;

/**
 * MasterHandler runs in a separate thread for every new client connection.
 * It parses the request type and routes the task to the appropriate Worker(s).
 */

public class MasterHandler extends Thread {
    private Socket clientSocket;
    private Master master; 

    // Global counter to track how many games exist across the whole distributed system
    public static volatile int totalGamesCount = 0;

    public MasterHandler(Socket socket, Master master) {
        this.clientSocket = socket;
        this.master = master;
    }

    // ==========================================================
    // 1. MAIN EXECUTION (Switch Board)
    // ==========================================================
    @Override
    public void run() {
        try {
            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            out.flush(); // Flushing the header before opening the input (avoiding deadlock)
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());

            // reads sent label from console to decide which method to call
            String requestType = (String) in.readObject();

            switch (requestType) {
                // --- MANAGER ACTIONS ---
                case "MANAGER_ADD":
                    Game newGame = (Game) in.readObject();
                    handleAddGame(newGame);
                    totalGamesCount++;
                    break;

                case "MANAGER_MODIFY_RISK":
                    String gameRisk = (String) in.readObject();
                    String newRisk = (String) in.readObject();
                    handleModifyRisk(gameRisk, newRisk, out);
                    break;
                    
                case "MANAGER_MODIFY_LIMITS":
                    String gameLim = (String) in.readObject();
                    double newMin = in.readDouble();
                    double newMax = in.readDouble();
                    handleModifyLimits(gameLim, newMin, newMax, out);
                    break;

                case "MANAGER_DELETE":
                    String gameToDelete = (String) in.readObject();
                    handleDeleteGame(gameToDelete,out);
                    break;

                case "MANAGER_AGGREGATE":
                    ManagerQueryRequest aggReq = (ManagerQueryRequest) in.readObject();
                    int aggQid = handleManagerAggregate(aggReq, out);
                    waitForMapReduceCompletion(aggQid);
                    break;
                
                case "MANAGER_RESTORE":
                    String gameToRestore = (String) in.readObject();
                    handleRestoreGame(gameToRestore,out);
                    break;

                // --- PLAYER ACTIONS ---
                case "PLAYER_PLAY":
                    PlayRequest playReq = (PlayRequest) in.readObject();
                    handlePlay(playReq, out);
                    break;

                case "PLAYER_SEARCH":
                    SearchFilters filters = (SearchFilters) in.readObject();
                    int searchQid = handleSearch(filters, out);
                    waitForMapReduceCompletion(searchQid);
                    break;

                case "ADD_BALANCE":
                    String playerName = (String) in.readObject(); 
                    double amount = (double) in.readDouble(); 
                    
                    handleAddBalance(playerName, amount, out); 
                    break;

                case "GET_BALANCE":
                    String pName = (String) in.readObject();
                    handleGetBalance(pName, out);
                    break;

                case "PLAYER_RATE":
                    String rateGameName = (String) in.readObject();
                    int starsToGive = (int) in.readObject();
                    handlePlayerRate(rateGameName, starsToGive, out);
                    break;

                // --- UTILITY & REDUCER ACTIONS ---
                case "ADD_PLAYER":
                    String playerNametoReg = (String) in.readObject();
                    String passwordToReg = (String) in.readObject();
                    String fullNameToReg = (String) in.readObject();
                    String emailToReg = (String) in.readObject();
                    String bDate = (String) in.readObject();
                    handleRegisterPlayer(playerNametoReg, passwordToReg, fullNameToReg, emailToReg, bDate, out);
                    break;
                case "GET_PLAYER_INFO":
                    String nameToGetInfo = (String) in.readObject();
                    handleGetPlayerInfo(nameToGetInfo, out);
                    break;
                case "UPDATE_PROFILE":
                    String updatePayload = (String) in.readObject();
                    handleUpdateProfile(updatePayload, out);
                    break;
                case "DELETE_ACCOUNT":
                    String deletePayload = (String) in.readObject();
                    handleDeleteAccount(deletePayload, out);
                    break;
                case "CHECK_GAME_EXISTS":
                    String gameNameCheck = (String) in.readObject();
                    handleCheckGameExists(gameNameCheck, out);
                    break;
                case "CHECK_PLAYER_EXISTS":
                    handlePlayerExists(in,out);
                    break;
                case "CHECK_PROVIDER_EXISTS":
                    handleProviderExists(in,out);
                    break;
                case "REDUCER_RESULT":
                    ReducerResult res = (ReducerResult) in.readObject();
                    handleReducerResult(res);
                    out.writeObject("OK");
                    out.flush();
                    break;
                    
                case "CHECK_SYSTEM_EMPTY":
                    out.writeObject(totalGamesCount == 0);
                    out.flush();
                    break;
                default:
                    System.err.println("Unknown request type: " + requestType);
            }
        } catch (Exception e) {
            System.err.println("\nError handling client: " + e.getMessage());
        } finally {
            try {
                if (clientSocket != null) clientSocket.close();
            } catch(IOException e) { 
                e.printStackTrace(); 
            }
        }
    }


    // ==========================================================
    // 2. MANAGER HANDLERS
    // ==========================================================
    private void handleAddGame(Game game) {
        // Uses a hashing function (in Master) to determine which Worker stores this game
        WorkerInfo targetWorker = master.getTargetWorker(game.getGameName()); // assigns new game to worker
        
        // sends request to selected worker (opens socket)
        try (Socket workerSocket = new Socket(targetWorker.getIp(), targetWorker.getPort());

            ObjectOutputStream out = new ObjectOutputStream(workerSocket.getOutputStream())) {
            out.flush();
            ObjectInputStream in = new ObjectInputStream(workerSocket.getInputStream());
            
            System.out.println("\nSending game " + game.getGameName() + " to Worker " + targetWorker.getPort());
            // sends request's label
            out.writeObject("ADD_GAME");
            out.writeObject(game);
            out.flush();

            String ack = (String) in.readObject(); // waits for confirmation
            System.out.println("Worker replied: " + ack);
        } catch (Exception e) {
            System.err.println("\nFailed to connect to Worker on port " + targetWorker.getPort() + ": " + e.getMessage());
        }   
    }

    private void handleModifyRisk(String gameName, String newRisk, ObjectOutputStream clientOut) {
        // Routes to the specific worker responsible for this game
        WorkerInfo target = master.getTargetWorker(gameName);

        // sends request to selected worker (opens socket)
        try (Socket socket = new Socket(target.getIp(), target.getPort());
             ObjectOutputStream workerOut = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream workerIn = new ObjectInputStream(socket.getInputStream())) {
            
            // sends request's label
            workerOut.writeObject("MANAGER_MODIFY_RISK");
            workerOut.writeObject(gameName);
            workerOut.writeObject(newRisk);
            workerOut.flush();
            
            String workerReply = (String) workerIn.readObject(); // receives response from worker

            // sends results to console
            clientOut.writeObject(workerReply);
            clientOut.flush();
        } catch (Exception e) {
            System.err.println("\nError forwarding modify risk: " + e.getMessage());
        }
    }

    private void handleModifyLimits(String gameName, double newMin, double newMax, ObjectOutputStream clientOut) {
        // Routes to the specific worker responsible for this game
        WorkerInfo target = master.getTargetWorker(gameName);

        // sends request to selected worker (opens socket)
        try (Socket socket = new Socket(target.getIp(), target.getPort());
            ObjectOutputStream workerOut = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream workerIn = new ObjectInputStream(socket.getInputStream())) {

            //sends request's label
            workerOut.writeObject("MANAGER_MODIFY_LIMITS");
            workerOut.writeObject(gameName);
            workerOut.writeDouble(newMin);
            workerOut.writeDouble(newMax);
            workerOut.flush();
            
            String workerReply = (String) workerIn.readObject();//receives response from worker

            //sends results to console
            clientOut.writeObject(workerReply);
            clientOut.flush();
        } catch (Exception e) {
            System.err.println("\nError forwarding modify limits: " + e.getMessage());
        }
    }

    private void handleDeleteGame(String gameName, ObjectOutputStream clientOut) {
        // Routes to the specific worker responsible for this game
        WorkerInfo target = master.getTargetWorker(gameName);
        
        //sends request to selected worker (opens socket)
        try (Socket socket = new Socket(target.getIp(), target.getPort());
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream workerIn = new ObjectInputStream(socket.getInputStream())) {
            
            // sends request's label
            out.writeObject("MANAGER_DELETE");
            out.writeObject(gameName);
            out.flush();

            String workerReply = (String) workerIn.readObject(); //receives response from worker

            //sends results to console
            clientOut.writeObject(workerReply);
            clientOut.flush();
            
            System.out.println("\nMaster: Forwarded delete command for " + gameName + " to Worker " + target.getPort());
        } catch (Exception e) {
            System.err.println("\nError forwarding delete: " + e.getMessage());
            try { clientOut.writeObject("ERROR"); clientOut.flush(); } catch (Exception ignored) {}
        }
    }

    private int handleManagerAggregate(ManagerQueryRequest req, ObjectOutputStream outToClient) {
        int qId = Master.generateQueryId(); // Unique ID for this specific distributed request

        //using "synchronized" for thread safety
        synchronized (Master.pendingRequests) {
            Master.pendingRequests.put(qId, outToClient); //registering request in the Master's global pending Map.
        }

        ManagerQueryRequest requestPacket = new ManagerQueryRequest(qId, req.getQueryType(), req.getTargetName()); // creating request object with qId,query type, name of player/provider

        // MAP PHASE: Send the query to EVERY worker in the cluster 
        for (WorkerInfo worker : master.getWorkers()) {

            try (Socket workerSocket = new Socket(worker.getIp(), worker.getPort());
                ObjectOutputStream outToWorker = new ObjectOutputStream(workerSocket.getOutputStream());
                ObjectInputStream inFromWorker = new ObjectInputStream(workerSocket.getInputStream())) {

                // sends request's label to worker
                outToWorker.writeObject("MAP_AGGREGATE");
                outToWorker.writeObject(requestPacket);
                outToWorker.flush();

                System.out.println("\nMaster: Sent aggregate request " + qId + " to Worker " + worker.getPort());
            } catch (Exception e) {
                System.err.println("\nError: Worker on port " + worker.getPort() + " is offline. " + e.getMessage());
            }
        }
        return qId;
    }


    private void handleRestoreGame(String gameToRestore, ObjectOutputStream out) {
        // Routes to the specific worker responsible for this game
        WorkerInfo target = master.getTargetWorker(gameToRestore); 
    
        if (target != null) {
            try (Socket workerSocket = new Socket(target.getIp(), target.getPort());
                ObjectOutputStream workerOut = new ObjectOutputStream(workerSocket.getOutputStream());
                ObjectInputStream workerIn = new ObjectInputStream(workerSocket.getInputStream())) {
                
                //sends request's label to worker
                workerOut.writeObject("MANAGER_RESTORE");
                workerOut.writeObject(gameToRestore);
                workerOut.flush();
                
                //receiving response from worker
                String response = (String) workerIn.readObject();
            
                //sending response to console
                out.writeObject(response); 
            } catch (Exception e) {
                System.err.println("\nError restoring game: " + e.getMessage());
            }
        }
    }


    // ==========================================================
    // 3. PLAYER HANDLERS
    // ==========================================================
    private void handlePlay(PlayRequest playReq, ObjectOutputStream outToClient) {
        WorkerInfo PlayerWorker = master.getTargetWorker(playReq.getPlayerName());
        
        try {
            // STEP 1: Checking balance
            double currentBalance = 0;
            try (Socket pSocket = new Socket(PlayerWorker.getIp(), PlayerWorker.getPort());
                ObjectOutputStream pOut = new ObjectOutputStream(pSocket.getOutputStream());
                ObjectInputStream pIn = new ObjectInputStream(pSocket.getInputStream())) {
                pOut.writeObject("GET_BALANCE");
                pOut.writeObject(playReq.getPlayerName());
                pOut.flush();
                currentBalance = (Double) pIn.readObject();
                if (currentBalance < playReq.getBetAmount()) {
                    outToClient.writeObject("Error: Insufficient funds.");
                    outToClient.flush();
                    return;
                }
            }
            

            // STEP 2: Game execution in GameWorker
            WorkerInfo GameWorker = master.getTargetWorker(playReq.getGameName());
            try (Socket workerSocket = new Socket(GameWorker.getIp(), GameWorker.getPort());
                ObjectOutputStream outToWorker = new ObjectOutputStream(workerSocket.getOutputStream());
                ObjectInputStream inFromWorker = new ObjectInputStream(workerSocket.getInputStream())) {
                
                outToWorker.writeObject("PLAY_GAME");
                outToWorker.writeObject(playReq);
                outToWorker.flush();

                // Receiving winAmount from Worker
                double winAmount = (Double) inFromWorker.readObject(); 
                String resultMessage = (String) inFromWorker.readObject();

                if (resultMessage.toLowerCase().contains("error")) {
                    outToClient.writeObject(resultMessage);
                    outToClient.flush();
                    return; // Διακοπή της μεθόδου!
                }

                // STEP 3: Updating PlayerWorker 
                try (Socket pUpdateSocket = new Socket(PlayerWorker.getIp(), PlayerWorker.getPort());
                    ObjectOutputStream pUpdateOut = new ObjectOutputStream(pUpdateSocket.getOutputStream());
                    ObjectInputStream pIn = new ObjectInputStream(pUpdateSocket.getInputStream())) {
                    
                    pUpdateOut.writeObject("RECORD_BET"); // 
                    pUpdateOut.writeObject(playReq.getPlayerName());
                    pUpdateOut.writeDouble(playReq.getBetAmount()); 
                    pUpdateOut.writeDouble(winAmount);              
                    pUpdateOut.flush();

                    pIn.readObject(); // Wait for confirmation
                }

                outToClient.writeObject(resultMessage);
                outToClient.flush();
            }
        } catch (Exception e) {
            // Errors
        }
    }

    private int handleSearch(SearchFilters filters, ObjectOutputStream outToClient) {
        int qId = Master.generateQueryId();  // Unique ID for this specific distributed request

        //using "synchronized" for thread safety
        synchronized (Master.pendingRequests) { 
            Master.pendingRequests.put(qId, outToClient); //registering request in the Master's global pending Map.
        }

        // creates request object with id,filters
        SearchRequest requestPacket = new SearchRequest(qId, filters);

         // MAP PHASE: Send the query to EVERY worker in the cluster
        for (WorkerInfo worker : master.getWorkers()) {

            //sends request to selected worker (opens socket)
            try (Socket workerSocket = new Socket(worker.getIp(), worker.getPort());
                ObjectOutputStream outToWorker = new ObjectOutputStream(workerSocket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(workerSocket.getInputStream())) {
                 
                // sends request's label
                outToWorker.writeObject("MAP_SEARCH"); 
                outToWorker.writeObject(requestPacket);
                outToWorker.flush();
                
                String ack = (String) in.readObject();  //receives response from worker
                System.out.println("Master: I sent request " + qId + " to Worker " + worker.getPort() + ". Worker replied: " + ack);
            } catch (Exception e) {
                System.err.println("\nError: Worker on port " + worker.getPort() + " is out of connection.");
            }
        }
        return qId;
    }

    private void handleAddBalance(String playerName, double amount, ObjectOutputStream outToClient) {
        // Find worker who has the player's data
        WorkerInfo target = master.getTargetWorker(playerName); 
        
        //sends request to selected worker (opens socket)
        try (Socket socket = new Socket(target.getIp(), target.getPort());
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            // sends request's label
            out.writeObject("ADD_BALANCE");
            out.writeObject(playerName);
            out.writeDouble(amount);
            out.flush();

            String response = (String) in.readObject();//receives response from worker

            //sends results to console
            outToClient.writeObject(response);
            outToClient.flush();

        } catch (Exception e) {
            try { outToClient.writeObject("Error: Worker unreachable"); outToClient.flush(); }  //sending final result to console if possible
            catch (IOException ex) { ex.printStackTrace(); }
        }
    }

    private void handleGetBalance(String pName, ObjectOutputStream outToClient) {
        // Finding worker who has the player's data
        WorkerInfo target = master.getTargetWorker(pName); 

        //opening socket with suited worker
        try (Socket socket = new Socket(target.getIp(), target.getPort());
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            //sending label to worker
            out.writeObject("GET_BALANCE");
            out.writeObject(pName);
            out.flush();

            // Receive the balance from worker 
            Object response = in.readObject();
            Double balance = (Double) response;

            //sending final result to console
            if (balance < 0) { // (worker saves balance as -1 if player is not found)
                outToClient.writeObject("Error: Player '" + pName + "' not found.");
            } else {
                outToClient.writeObject("Current balance for " + pName + ": " + balance + " FUN.");
            }
            outToClient.flush();

        } catch (Exception e) {
            try { outToClient.writeObject("Error: Worker unreachable"); outToClient.flush(); }  
            catch (IOException ex) { ex.printStackTrace(); }
        }
    }

    private void handlePlayerRate(String rateGameName, int starsToGive, ObjectOutputStream outToClient) {
        // Find worker who has the game's data
        WorkerInfo targetWorker = master.getTargetWorker(rateGameName);

        //opening socket with suited worker
        try (Socket workerSocket = new Socket(targetWorker.getIp(), targetWorker.getPort());
            ObjectOutputStream workerOut = new ObjectOutputStream(workerSocket.getOutputStream());
            ObjectInputStream workerIn = new ObjectInputStream(workerSocket.getInputStream())) {
            
            //sending label to worker
            workerOut.writeObject("PLAYER_RATE");
            workerOut.writeObject(rateGameName);
            workerOut.writeObject(starsToGive);
            workerOut.flush();

            String workerReply = (String) workerIn.readObject(); //receiving results from worker

            //sending final result to console
            outToClient.writeObject(workerReply);
            outToClient.flush();
        } catch (Exception e) {
            System.err.println("\nError forwarding rating: " + e.getMessage());
        }
    }


    // ==========================================================
    // 4. UTILITY & REDUCER HANDLERS
    // ==========================================================
    private void handleRegisterPlayer(String pName, String passwordToReg, String fullName, String email,String bDate, ObjectOutputStream outToClient) {
        // finding corresponding worker for player
        WorkerInfo target = master.getTargetWorker(pName); 

        //Opening socket to worker
        try (Socket socket = new Socket(target.getIp(), target.getPort());
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())){ 
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            //Sending label to worker
            out.writeObject("ADD_PLAYER");
            out.writeObject(pName);
            out.writeObject(passwordToReg);
            out.writeObject(fullName);
            out.writeObject(email);
            out.writeObject(bDate);
            out.flush();

            //Receiving response from worker
            String response = (String) in.readObject();

            //Sending result to client
            outToClient.writeObject(response);
            outToClient.flush();
            
        } catch (Exception e) {
            try { outToClient.writeObject("Error: Worker offline"); outToClient.flush(); } 
                catch (IOException ex) { ex.printStackTrace(); }
        }
    }
   
    private void handleGetPlayerInfo(String pName, ObjectOutputStream outToClient) {
    WorkerInfo target = master.getTargetWorker(pName); 
        try (Socket socket = new Socket(target.getIp(), target.getPort());
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject("GET_PLAYER_INFO");
            out.writeObject(pName);
            out.flush();

            Player p = (Player) in.readObject();
            outToClient.writeObject(p);
            outToClient.flush();

        } catch (Exception e) {
            try { outToClient.writeObject("Error: Worker offline"); outToClient.flush(); } 
                catch (IOException ex) { ex.printStackTrace(); }
        }
    }

    private void handleUpdateProfile(String payload, ObjectOutputStream outToClient) {
        try {
            // payload -> "UPDATE_PROFILE:currentName,newName,newPassword"
            String data = payload.substring("UPDATE_PROFILE:".length());
            String[] parts = data.split(",");
            String currentName = parts[0];

            WorkerInfo target = master.getTargetWorker(currentName);

            if (target != null) {
                try (Socket socket = new Socket(target.getIp(), target.getPort());
                     ObjectOutputStream workerOut = new ObjectOutputStream(socket.getOutputStream());
                     ObjectInputStream workerIn = new ObjectInputStream(socket.getInputStream())) {

                    // Ο Master στέλνει στον Worker 2 αντικείμενα
                    workerOut.writeObject("UPDATE_PROFILE");
                    workerOut.writeObject(payload); 
                    workerOut.flush();

                    String workerReply = (String) workerIn.readObject();
                    outToClient.writeObject(workerReply);
                    outToClient.flush();
                }
            } else {
                outToClient.writeObject("FAILURE: Player route not found");
                outToClient.flush();
            }
        } catch (Exception e) {
            System.err.println("Error routing profile update: " + e.getMessage());
        }
    }

    private void handleDeleteAccount(String payload, ObjectOutputStream outToClient) {
        try {
            // payload -> "DELETE_ACCOUNT:playerName"
            String playerName = payload.substring("DELETE_ACCOUNT:".length());

            WorkerInfo target = master.getTargetWorker(playerName);

            if (target != null) {
                try (Socket socket = new Socket(target.getIp(), target.getPort());
                     ObjectOutputStream workerOut = new ObjectOutputStream(socket.getOutputStream());
                     ObjectInputStream workerIn = new ObjectInputStream(socket.getInputStream())) {

                    // Ο Master στέλνει στον Worker 2 αντικείμενα
                    workerOut.writeObject("DELETE_ACCOUNT");
                    workerOut.writeObject(playerName); 
                    workerOut.flush();

                    String workerReply = (String) workerIn.readObject();
                    outToClient.writeObject(workerReply);
                    outToClient.flush();
                }
            } else {
                outToClient.writeObject("FAILURE: Player route not found");
                outToClient.flush();
            }
        } catch (Exception e) {
            System.err.println("Error routing account deletion: " + e.getMessage());
        }
    }


    private void handleCheckGameExists(String gameNameCheck, ObjectOutputStream outToClient) {
        // finding corresponding worker for game
        WorkerInfo workerForCheck = master.getTargetWorker(gameNameCheck);
        boolean gameFound = false;
        
        //Opening socket to worker
        try (Socket workerSocket = new Socket(workerForCheck.getIp(), workerForCheck.getPort());
            ObjectOutputStream wOut = new ObjectOutputStream(workerSocket.getOutputStream());
            ObjectInputStream wIn = new ObjectInputStream(workerSocket.getInputStream())) {
             
            //Sending label to worker
            wOut.writeObject("CHECK_GAME_EXISTS");
            wOut.writeObject(gameNameCheck);
            wOut.flush();
            
            gameFound = (boolean) wIn.readObject(); //Receiving response from worker
        } catch (Exception e) {
            System.err.println("\nError checking if game exists: " + e.getMessage());
        }
        
        try {
            outToClient.writeObject(gameFound);
            outToClient.flush(); //sending final result to console if possible
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handlePlayerExists(ObjectInputStream in,ObjectOutputStream out) throws Exception {
        // Reads the name of player the client is asking about
        String playername = (String) in.readObject();
        String playerPassword = (String) in.readObject();
                    boolean playerFound = false;

                    // it is not known which worker has this player, so it is necessary to ask ALL of them.
                    for (WorkerInfo worker : master.getWorkers()) {
                        //Opening socket to worker
                        try (Socket ws = new Socket(worker.getIp(), worker.getPort());
                            ObjectOutputStream wOut = new ObjectOutputStream(ws.getOutputStream());
                            ObjectInputStream wIn = new ObjectInputStream(ws.getInputStream())) {

                            //Sending label to current worker in loop
                            wOut.writeObject("CHECK_PLAYER_EXISTS");
                            wOut.writeObject(playername);
                            wOut.writeObject(playerPassword);
                            wOut.flush();

                            // Waits for this specific worker to check its local hashMap
                            boolean existsOnWorker = (boolean) wIn.readObject(); 

                            if (existsOnWorker) {
                                playerFound = true;
                                break; //If found the player on one worker, no need to check the rest
                            }
                        } catch (Exception e) {
                            System.err.println("Worker on port " + worker.getPort() + " offline during player check.");
                        }
                    }

                    //sending final result to console (boolean variable, true-> player is found, otherwise false)
                    out.writeObject(playerFound);
                    out.flush();
    }

    private void handleProviderExists(ObjectInputStream in,ObjectOutputStream out) throws Exception {
        // Reads the name of provider the client is asking about
        String providerName = (String) in.readObject();
                    boolean providerFound = false;

                    // it is not known which worker has this player, so it is necessary to ask ALL of them.
                    for (WorkerInfo worker : master.getWorkers()) {
                        //Opening socket to worker
                        try (Socket ws = new Socket(worker.getIp(), worker.getPort());
                            ObjectOutputStream wOut = new ObjectOutputStream(ws.getOutputStream());
                            ObjectInputStream wIn = new ObjectInputStream(ws.getInputStream())) {
                            
                            //Sending label to current worker in loop
                            wOut.writeObject("CHECK_PROVIDER_EXISTS");
                            wOut.writeObject(providerName);
                            wOut.flush();

                            // Waits for this specific worker to check its local hashMap
                            boolean existsOnWorker = (boolean) wIn.readObject(); //Receiving response from worker

                            if (existsOnWorker) {
                                providerFound = true;
                                break;  //If found the player on one worker, no need to check the rest
                            }
                        } catch (Exception e) {
                            System.err.println("Worker on port " + worker.getPort() + " offline during provider check.");
                        }
                    }

                    //sending final result to console (boolean variable, true-> provider is found, otherwise false)
                    out.writeObject(providerFound); 
                    out.flush();
    }


    /**
     * REDUCER CALLBACK: This is called when the Reducer node has finished 
     * aggregating data from all workers and sends the final answer back to the Master.
     */
    private void handleReducerResult(ReducerResult res) {
        ObjectOutputStream playerOut = null;

        // Retrieves the original console's connection using the query ID
        synchronized (Master.pendingRequests) {
            playerOut = Master.pendingRequests.remove(res.getQueryId()); // remove() takes the stream out of the map because the request is now finished
        }

        if (playerOut != null) {
            try {
                // Determines what kind of result we got (List of Games or a Number)
                if ("GAMES".equals(res.getResultType())) {
                    playerOut.writeObject(res.getFinalGames());
                } else {
                    playerOut.writeObject(res.getTotalAmount());
                }
                playerOut.flush();
                System.out.println("\nMaster: The search " + res.getQueryId() + " was completed and sent to the client.");
            } catch (IOException e) {
                System.err.println("Error sending Reducer results to client: " + e.getMessage());
            }

            // Wakes up the original masterHandler thread that was waiting for this result
            synchronized (Master.pendingRequests) {
                Master.pendingRequests.notifyAll();
            }
        }
    }

    // master' function pauses until he gets an answer from Reducer 
    //It prevents the Master from closing the client socket before the Reducer provides the data.
    private void waitForMapReduceCompletion(int queryId) throws InterruptedException {
        synchronized (Master.pendingRequests) {
            // As long as the query ID is still in the map, the result hasn't arrived yet
            while (Master.pendingRequests.containsKey(queryId)) {
                // thread goes to sleep until handleReducerResult calls notifyAll()
                Master.pendingRequests.wait();
            }
        }
    }
}