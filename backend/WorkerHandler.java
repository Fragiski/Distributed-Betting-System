import com.example.casinoapp.Game;
import com.example.casinoapp.ManagerQueryRequest;
import com.example.casinoapp.PlayRequest;
import com.example.casinoapp.Player;
import com.example.casinoapp.SearchFilters;
import com.example.casinoapp.SearchRequest;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * WorkerHandler runs on the Worker node. 
 * It handles logic for a specific subset of games and players assigned to this worker.
 */

public class WorkerHandler extends Thread {
    private Socket socket;
    private Map<String, Game> localGames; // Local "database" of games
    private Map<String, Player> localPlayers; // Local "database" of players

    public WorkerHandler(Socket socket, Map<String, Game> localGames, Map<String, Player> localPlayers) {
        this.socket = socket;
        this.localGames = localGames;
        this.localPlayers = localPlayers;
    }

    // ==========================================================
    // 1. MAIN EXECUTION (Switch Board)
    // ==========================================================
    @Override
    public void run() {
        try (
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ) {
            String requestType = (String) in.readObject();

            switch (requestType) {
                // --- MANAGER ACTIONS ---
                case "ADD_GAME":
                    handleAddGame(in, out);
                    break;
                case "MANAGER_DELETE":
                    handleManagerDelete(in,out);
                    break;
                case "MANAGER_MODIFY_RISK":
                    handleManagerModifyRisk(in, out);
                    break;
                case "MANAGER_MODIFY_LIMITS":
                    handleManagerModifyLimits(in, out);
                    break;
                case "MANAGER_RESTORE":
                    handleManagerRestore(in,out);
                break;
                case "RECORD_BET":
                    handleRecordBet(in, out);
                break;

                // --- PLAYER ACTIONS ---
                case "PLAY_GAME":
                    handlePlayGame(in, out);
                    break;
                case "ADD_BALANCE":
                    handleAddBalance(in, out);
                    break;
                case "GET_BALANCE":
                    handleGetBalance(in, out);
                    break;
                
                case "PLAYER_RATE":
                    handlePlayerRate(in, out);
                    break;

                // --- MAPREDUCE ACTIONS ---
                case "MAP_AGGREGATE":
                    handleMapAggregate(in);
                    break;
                case "MAP_SEARCH":
                    handleMapSearch(in, out);
                    break;

                // --- UTILITY ACTIONS ---
                case "ADD_PLAYER":
                    handleRegisterPlayer(in, out);
                    break;
                case "GET_PLAYER_INFO":
                    handlePlayerInfo(in,out);
                    break;
                case "UPDATE_PROFILE":
                    handleWorkerUpdateProfile(in, out);
                    break;
                case "DELETE_ACCOUNT":
                    handleWorkerDeleteAccount(in, out);
                    break;
                case "CHECK_GAME_EXISTS":
                    handleCheckGameExists(in, out);
                    break;
                case "CHECK_PLAYER_EXISTS":
                    handlePlayerExists(in,out);
                    break;
                case "CHECK_PROVIDER_EXISTS":
                    handleProviderExists(in,out);
                    break;
                default:
                    System.err.println("\nWorker: Unknown request from Master: " + requestType);
            }
        } catch (EOFException | java.net.SocketException e) { // Standard network cleanup - no action needed
        } catch (Exception e) {
            System.err.println("\nError in WorkerHandler: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close(); //closing socket
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    // ==========================================================
    // 2. MANAGER HANDLERS
    // ==========================================================
    private void handleAddGame(ObjectInputStream in, ObjectOutputStream out) throws Exception {
        Game newGame = (Game) in.readObject(); // creates new Game object

        // Synchronize on the collection to prevent concurrent write issues
        synchronized (localGames) {
            localGames.put(newGame.getGameName(), newGame); 
        }
        System.out.println("\nWorker: Successfully saved the game to memory: " + newGame.getGameName());

        // Sending acknowledgment back to Master
        out.writeObject("OK");
        out.flush();
    }

    private void handleManagerDelete(ObjectInputStream in, ObjectOutputStream out) throws Exception {
        String nameToDelete = (String) in.readObject();

        // Synchronize on the collection for thread safety
        synchronized (localGames) {
            Game g = localGames.get(nameToDelete);

            if (g != null) {
                g.setActive(false); // changing the game's status to inactive
                System.out.println("\nWorker: Game '" + nameToDelete + "' is now INACTIVE!");
                out.writeObject("SUCCESS");
            } else {
                out.writeObject("NOT_FOUND");
            }
            out.flush();
        }
    }

    private void handleRecordBet(ObjectInputStream in, ObjectOutputStream out) throws Exception {
        String playerName = (String) in.readObject();
        double bet = in.readDouble();
        double win = in.readDouble();

        synchronized (localPlayers) {
            Player p = localPlayers.get(playerName);
            if (p != null) {
                p.recordBet(bet, win); // Updating balance and totalProfitLoss
                out.writeObject("SUCCESS: Bet recorded for " + playerName);
            } else {
                out.writeObject("ERROR: Player not found.");
            }
        }
        out.flush();
    }

    private void handleManagerModifyRisk(ObjectInputStream in, ObjectOutputStream out) throws Exception {
        String gameToMod = (String) in.readObject();
        String newRiskLvl = (String) in.readObject();
        
        //Fetching from localGames Map
        synchronized (localGames) {
            Game game = localGames.get(gameToMod);

            if (game != null) {
                // updating game
                game.setRiskLevel(newRiskLvl); 
                System.out.println("\nWorker: Game '" + gameToMod + "' modified! New Risk: " + newRiskLvl);
                out.writeObject("SUCCESS"); // to master
            } else {
                out.writeObject("NOT_FOUND");
            }
        }
        out.flush();
    }

    private void handleManagerModifyLimits(ObjectInputStream in, ObjectOutputStream out) throws Exception {
        String gameToMod = (String) in.readObject();
        double newMinBet = in.readDouble();
        double newMaxBet = in.readDouble();

        //Fetching from localGames Map
        synchronized (localGames) {
            Game game = localGames.get(gameToMod);

            if (game != null) {
                // updating game
                game.setMinBet(newMinBet);
                game.setMaxBet(newMaxBet);
                System.out.println("\nWorker: Game '" + gameToMod + "' modified! Min: " + newMinBet + " | Max: " + newMaxBet);
                out.writeObject("SUCCESS"); // to master
            } else {
                out.writeObject("NOT_FOUND");
            }
        }
        out.flush();
    }


    private void handleManagerRestore(ObjectInputStream in, ObjectOutputStream out) throws Exception {
        String gameToRestore = (String) in.readObject();

        //Fetching from localGames Map
        synchronized (localGames) { 

            if (localGames.containsKey(gameToRestore)) {
                Game g = localGames.get(gameToRestore);

                if (!g.isActive()) {
                    //setting game to "active"
                    g.setActive(true); 
                    System.out.println("\nWorker: Game '" + gameToRestore + "restored.");
                    out.writeObject("SUCCESS!"); // to master
                }
                else {
                    //game is already active
                    System.out.println("\nWorker: Game '" + gameToRestore + "is already active.");
                    out.writeObject("Game is already active."); // to master
                }

            } else {
                //game not found
                out.writeObject("FAILED: Game not found on this node.");
            }
        }
        out.flush();
    }


    // ==========================================================
    // 3. PLAYER HANDLERS
    // ==========================================================
    private void handlePlayGame(ObjectInputStream in, ObjectOutputStream out) throws Exception {
        PlayRequest playReq = (PlayRequest) in.readObject();
        double betAmount = playReq.getBetAmount();
        System.out.println("\nWorker: Bet request received for game: " + playReq.getGameName());

        Game targetGame;
        synchronized (localGames) {
            targetGame = localGames.get(playReq.getGameName());
        }

        // in case game does not exist 
        if (targetGame == null) {
            out.writeObject(0.0); // ProfitLoss = 0
            out.writeObject("Error: Game not found on Worker.");
            out.flush(); 
            return;
        }

        // in case game is deleted by manager
        if (!targetGame.isActive()) { 
            out.writeObject(0.0); // ProfitLoss = 0
            out.writeObject("Error: This game is currently unavailable.");
            out.flush();
            return;
        }


        // checks for min bet limit
        if (betAmount < targetGame.getMinBet()) {
            out.writeObject(0.0);
            out.writeObject("Error: Bet is too low! The minimum bet for '" + targetGame.getGameName() + "' is " + targetGame.getMinBet() + " FUN.");
            out.flush();
            return;
        }

        // checks for max bet limit
        if (betAmount > targetGame.getMaxBet()) {
            out.writeObject(0.0);
            out.writeObject("Error: Bet is too high! The maximum bet for '" + targetGame.getGameName() + "' is " + targetGame.getMaxBet() + " FUN.");
            out.flush();
            return;
        }
        
        // 1. Communicates with SRG
        int randomNumber = -1;
        String receivedHash = "";
        
        //opens socket with SRG
        try (Socket srgSocket = new Socket(Config.SRG_IP, Config.SRG_PORT); 
            ObjectOutputStream srgOut = new ObjectOutputStream(srgSocket.getOutputStream());
            ObjectInputStream srgIn = new ObjectInputStream(srgSocket.getInputStream())) {
        
            //Requesting a new number
            srgOut.writeObject("GET_NUMBER");

            // Sending the game's unique 'HashKey' as a seed. 
            // This links the random result specifically to THIS game session.
            srgOut.writeObject(targetGame.getHashKey());
            srgOut.flush();

            // Receiving the result
            // The SRG sends two things: the raw number and a cryptographic signature (hash).
            randomNumber = srgIn.readInt();
            receivedHash = (String) srgIn.readObject();
        } catch (Exception e) {
            out.writeObject(0.0);
            out.writeObject("Error: Unable to communicate with the Random Number Generator.");
            out.flush();
            return;
        }

        // 2. SHA-256 Verification (proving the result hasn't been tampered with in transit.)
        String rawData = randomNumber + targetGame.getHashKey();  // Combining the number and the secret key exactly how the SRG did.
        String expectedHash = generateSHA256(rawData);

        // If the Worker recalculates the hash and it matches the one sent by the SRG, 
        // it guarantees that 'randomNumber' was not changed or corrupted
        if (!expectedHash.equals(receivedHash)) {
            out.writeObject(0.0);
            out.writeObject("Security Error: Hash does not match! Result rejected.");
            out.flush();
            return;
        }

        // 3. Profit/Loss Calculation
        double winAmount = 0.0;

        if (randomNumber % 100 == 0) {
            // JACKPOT check
            System.out.println("\nWorker: JACKPOT at " + targetGame.getGameName() + "!");
            winAmount = betAmount * targetGame.getJackpot();
        } else {
            // no jackpot
            int index = randomNumber % 10;
            double multiplier = getMultiplier(targetGame.getRiskLevel(), index);
            winAmount = betAmount * multiplier;
        }

        double playerProfit = winAmount - betAmount;  // player profits

        double systemProfit = betAmount - winAmount;  // system profits
        synchronized (targetGame) {
            targetGame.updateProfitLoss(systemProfit); 
        }

        String resultMsg = "The random number was " + randomNumber + ". You won " + winAmount + " FUN!";

        //sends results to console
        out.writeObject(winAmount);
        out.writeObject(resultMsg); 
        out.flush();
    }

    private void handleAddBalance(ObjectInputStream in, ObjectOutputStream out) throws Exception {
        String playerName = (String) in.readObject();
                    double amount = in.readDouble();
                    String result = "ERROR: Player not found.";

                    //Searching for requested player
                    synchronized (localPlayers) {
                        Player p = localPlayers.get(playerName);
                        if (p != null) {
                            p.addBalance(amount);
                            result = "SUCCESS: New balance for " + playerName + " is " + p.getBalance();
                        }
                    }

                    //Sending result to Master
                    out.writeObject(result);
                    out.flush();
    }

    private void handleGetBalance(ObjectInputStream in, ObjectOutputStream out) throws Exception {
        String nameToCheck = (String) in.readObject();
        double currentBal = 0.0;

        //Looking for player and getting his balance
        synchronized (localPlayers) {
            Player p = localPlayers.get(nameToCheck);
            if (p != null) {
                currentBal = p.getBalance();
            } else {
                currentBal = -1.0; // if player is not found
            }
        }
        
        //sending balance to master
        out.writeObject(currentBal); 
        out.flush();
    }

    private void handlePlayerRate(ObjectInputStream in, ObjectOutputStream out) throws Exception {
        String gameToRate = (String) in.readObject();
        int newStars = (int) in.readObject();

        synchronized (localGames) {
            Game g = localGames.get(gameToRate); // finding the requested game for rating 

            if (g != null) {
                g.addRating(newStars); // adds rating
                System.out.println("\nWorker: Game '" + gameToRate + "' received a new rating of " + newStars + " stars! Average updated.");
                out.writeObject("Game rated successfully and average updated!");
            } else {
                System.out.println("\nWorker: Rating failed. Game '" + gameToRate + "' not found.");
                out.writeObject("Rating failed. Game not found.");
            }
        }
        out.flush(); // sending results to master
    }


    // ==========================================================
    // 4. MAPREDUCE HANDLERS
    // ==========================================================

    /**
     * Searches local memory for games that match the user's filters.
     * This is a "Map" operation because it runs on every worker simultaneously.
     */
    private void handleMapSearch(ObjectInputStream in, ObjectOutputStream outToMaster) throws Exception {
        // 1. Unpacking the search request from the Master
        SearchRequest request = (SearchRequest) in.readObject();
        SearchFilters filters = request.getFilters();
        int queryId = request.getQueryId(); 

        List<Game> matchedGames = new ArrayList<>();

        // Iterating through only the games stored ON THIS worker.
        synchronized(localGames) {
            for (Game game : localGames.values()) {
                // Check: game matches or not ->Risk, Bet Category, and Star Rating
                boolean riskMatches = filters.getRiskLevel().equalsIgnoreCase("ALL") || 
                                      game.getRiskLevel().equalsIgnoreCase(filters.getRiskLevel());
                                      
                boolean betMatches = filters.getBetCategory().equalsIgnoreCase("ALL") || 
                                     game.getBetCategory().equalsIgnoreCase(filters.getBetCategory());
                                     
                boolean starsMatch = game.getStars() >= filters.getStars();

                // Only adds the game if it's active and meets all criteria
                if (game.isActive() && riskMatches && betMatches && starsMatch) {
                    matchedGames.add(game);
                }
            }
        }

        System.out.println("\nWorker: Finished filtering. Found " + matchedGames.size() + " games. Connecting to Reducer...");

        //Forwarding results to reducer to merge them all
        try (Socket reducerSocket = new Socket(Config.REDUCER_IP, Config.REDUCER_PORT);
            ObjectOutputStream outToReducer = new ObjectOutputStream(reducerSocket.getOutputStream());
            ObjectInputStream inFromReducer = new ObjectInputStream(reducerSocket.getInputStream())) {
            
            //label to reducer
            outToReducer.writeObject("REDUCE_SEARCH"); 
            outToReducer.writeObject(queryId);
            outToReducer.writeObject(matchedGames);
            outToReducer.flush();

            // Blocking until Reducer acknowledges receipt
            inFromReducer.readObject();
    
            System.out.println("Worker: Sent " + matchedGames.size() + " games to Reducer for qId: " + queryId);
        } catch (IOException e) {
            System.err.println("\nError: Reducer is offline or connection failed!");
        }

        // notifying master that worker's job is done
        outToMaster.writeObject("OK"); 
        outToMaster.flush();
    }

    /**
     * Calculates a local sum of profit/loss for a specific Provider or Player.
     */
    private void handleMapAggregate(ObjectInputStream in) throws Exception {
    // Unpacking request sent from master
    ManagerQueryRequest request = (ManagerQueryRequest) in.readObject();
    int queryId = request.getQueryId();
    String queryType = request.getQueryType();
    String targetName = request.getTargetName();

    double localSum = 0.0;

    if (queryType.equalsIgnoreCase("PROVIDER")) {
        // Sum profit/loss for all games belonging to a specific provider
        synchronized (localGames) {
            for (Game game: localGames.values()) {
                if (game.getProviderName().equalsIgnoreCase(targetName)) {
                    localSum += game.getTotalProfitLoss();
                }
            }
        }
    } else if (queryType.equalsIgnoreCase("PLAYER")) {
        synchronized (localPlayers) {
            // Sum profit/loss for all games belonging to a specific player
            Player player = localPlayers.get(targetName);
            if (player != null) {
                localSum = player.getTotalProfitLoss();
            }
        }
    }

    // Forwarding partial sum to reducer
    try (Socket reducerSocket = new Socket(Config.REDUCER_IP, Config.REDUCER_PORT);
        ObjectOutputStream outToReducer = new ObjectOutputStream(reducerSocket.getOutputStream());
        ObjectInputStream inFromReducer = new ObjectInputStream(reducerSocket.getInputStream())) {

        // Label to reducer
        outToReducer.writeObject("REDUCE_AGGREGATE");
        outToReducer.writeObject(queryId);
        outToReducer.writeDouble(localSum);
        outToReducer.flush();
        
        // Blocking until Reducer acknowledges receipt
        inFromReducer.readObject();
        
        System.out.println("\nWorker: Sent partial sum " + localSum + " to Reducer for qId: " + queryId);
    } catch (IOException e) {
        System.err.println("\nError: Reducer is offline.");
    }
}


    // ==========================================================
    // 5. UTILITY HANDLERS
    // ==========================================================

    /**
     * Registers a new player in this worker's local storage.
     */
    private void handleRegisterPlayer(ObjectInputStream in, ObjectOutputStream out) throws Exception {

        String newPlayerName = (String) in.readObject();
        String newPassword = (String) in.readObject();
        String fullName = (String) in.readObject();
        String email = (String) in.readObject();
        String bDate = (String) in.readObject();

                    //registering player
                    synchronized (localPlayers) {
                        if (localPlayers.containsKey(newPlayerName)) {
                            out.writeObject("ERROR: Player already exists."); //player already exists
                        } else {
                            //adding player
                            localPlayers.put(newPlayerName, new Player(newPlayerName, newPassword,fullName,email,bDate));
                            System.out.println("SUCCESS: Player '" + newPlayerName + "' registered." + bDate);
                            out.writeObject("SUCCESS: Player '" + newPlayerName + "' registered.");
                            
                        }
                    }

                    out.flush();
    }

    private void handlePlayerInfo (ObjectInputStream in, ObjectOutputStream out) throws Exception {

        String pName = (String) in.readObject();
            synchronized (localPlayers) {
                Player p = localPlayers.get(pName);
                out.writeObject(p); 
            }
        out.flush();
    }

    private void handleWorkerUpdateProfile(ObjectInputStream in, ObjectOutputStream out) throws Exception {
        String payload = (String) in.readObject();
        String data = payload.substring("UPDATE_PROFILE:".length());
        String[] parts = data.split(",");
        
        String currentName = parts[0];
        String newName = parts.length > 1 ? parts[1] : "";
        String newPassword = parts.length > 2 ? parts[2] : "";
        
        synchronized (localPlayers) {
            Player p = localPlayers.get(currentName);
            if (p != null) {
                if (!newPassword.isEmpty()) {
                    p.setPassword(newPassword);
                }
                
                if (!newName.isEmpty() && !currentName.equalsIgnoreCase(newName)) {
                    if (localPlayers.containsKey(newName)) {
                        out.writeObject("FAILURE: Username already exists.");
                        out.flush();
                        return;
                    }
                    p.setPlayerName(newName);
                    localPlayers.remove(currentName);
                    localPlayers.put(newName, p);
                }
                
                out.writeObject("SUCCESS");
            } else {
                out.writeObject("FAILURE: Player not found.");
            }
            out.flush();
        }
    }

    private void handleWorkerDeleteAccount(ObjectInputStream in, ObjectOutputStream out) throws Exception {
        String playerName = (String) in.readObject();
        
        synchronized (localPlayers) {
            if (localPlayers.containsKey(playerName)) {
                localPlayers.remove(playerName);
                System.out.println("Worker: Account permanently deleted -> " + playerName);
                out.writeObject("SUCCESS");
            } else {
                out.writeObject("FAILURE: Player not found on this node.");
            }
            out.flush();
        }
    }
    
    private void handleCheckGameExists(ObjectInputStream in, ObjectOutputStream out) throws Exception {
        String gameToCheck = (String) in.readObject();
        boolean exists;
        synchronized (localGames) {
            // looks for game's name as a key inside the hashmap 'localGames'
            exists = localGames.containsKey(gameToCheck);
        }
        out.writeObject(exists);
        out.flush();
    }

    private void handlePlayerExists (ObjectInputStream in, ObjectOutputStream out) throws Exception {
        String playerToCheck = (String) in.readObject();
        String passwordToCheck = (String) in.readObject();
        boolean loginSuccess = false;
                    
        synchronized (localPlayers) {
            // Checks if player exists AND if password is valid
            if(localPlayers.containsKey(playerToCheck)){
                Player p = localPlayers.get(playerToCheck);
                if(p.getPassword().equals(passwordToCheck)){
                    loginSuccess = true;
                }
            }
        }
                    
            out.writeObject(loginSuccess);
            out.flush();
    }

    /*Returns a multiplier based on the game's risk and a random index.
    * The 'index' is the last digit of the number received from the SRG (0-9).
    */
    private double getMultiplier(String riskLevel, int index) {
        double[] lowRisk = {0.0, 0.0, 0.0, 0.1, 0.5, 1.0, 1.1, 1.3, 2.0, 2.5};
        double[] mediumRisk = {0.0, 0.0, 0.0, 0.0, 0.0, 0.5, 1.0, 1.5, 2.5, 3.5};
        double[] highRisk = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 2.0, 6.5};

        // Determining which array to use based on the game's current risk setting
        switch(riskLevel.toLowerCase()) {
            case "low": return lowRisk[index];
            case "medium": return mediumRisk[index];
            case "high": return highRisk[index];
            default: return 0.0;
        }
    }

    /**
     * Checks if any game in this Worker's local memory belongs to a specific provider.
     */
    private void handleProviderExists(ObjectInputStream in, ObjectOutputStream out) throws Exception {

        String providerToCheck = (String) in.readObject();
                    boolean providerExists = false;
                    
                    synchronized (localGames) {
                        for (Game game : localGames.values()) { // iterates through the values of the hashmap(games)
                            if (game.getProviderName().equalsIgnoreCase(providerToCheck)) {
                                providerExists = true;
                                break; // Stops searching once we find at least one match
                            }
                        }
                    }
                    
                    //Sending boolean result back to Master
                    out.writeObject(providerExists);
                    out.flush();
    }

    /**
     * Cryptographic Tool: Converts a string input into a SHA-256 hex string.
     * This is used to verify that the Random Number hasn't been tampered with.
     */
    private String generateSHA256(String input) {
        try {
            // Gets the SHA-256 algorithm instance from Java Security
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");

            // Converts the input string (Number + Secret Key) into a byte array
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            
            // Converts the byte array into a human-readable Hexadecimal string (64 characters)
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                // Converts each byte to its hex representation
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}



