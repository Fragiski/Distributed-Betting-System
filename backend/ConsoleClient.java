import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.casinoapp.Game;
import com.example.casinoapp.ManagerQueryRequest;
import com.example.casinoapp.PlayRequest;
import com.example.casinoapp.SearchFilters;

public class ConsoleClient {
    private static final String MASTER_IP = Config.MASTER_IP;
    private static final int MASTER_PORT = Config.MASTER_PORT;

    // ==========================================================
    // 1. MAIN ENTRY POINT
    // ==========================================================
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("            Welcome at the Casino             ");
        System.out.println("----------------------------------------------");

        //STARTING MENU
        while (true) {
            System.out.println("\nChoose your role: ");
            System.out.println("1. Manager Login");
            System.out.println("2. Player Login");
            System.out.println("3. Register New Player");
            System.out.println("0. Exit Application");
            
            int role = readInteger(scanner, "Selection: ");

            //exit
            if (role == 0) {
                System.out.println("\nExiting Application...");
                break;
            } else if (role == 1) {
                handleManagerMenu(scanner); // continues to manager's menu
            } else if (role == 2) {
                // Checks if there are games for the player to play
                if (isSystemEmpty()) {
                    System.out.println("\n[!] The Casino is currently closed!");
                    System.out.println("No games have been added yet. Please ask a Manager to add some games first.");
                    continue; 
                }
                    // Player enters
                    System.out.print("\nEnter your Player Name to login: ");
                    String playerName = scanner.nextLine().trim();
                    while (playerName.isEmpty()) {
                        System.out.print("Name cannot be empty! Enter Player Name: ");
                        playerName = scanner.nextLine().trim();
                    
                }

                // Player not registered
                if (!checkPlayerExists(playerName)) {
                    System.out.println("\n[!] Error: Player '" + playerName + "' not found! Please register first (Option 3).");
                    continue;
                }

                System.out.println("\nWelcome to the Casino, " + playerName + "!");

                handlePlayerMenu(scanner, playerName); // continues to player's menu
             } else if (role == 3) { // player's registration
                    try {
                        registerPlayer(scanner);
                    }catch(CancelException e) {
                        System.out.println("\nRegistration cancelled by user."); // exit
                    }  
            } else {
                System.err.println("\nInvalid role selected. Please try again.");
            }
        }
        scanner.close();
    }


    // ==========================================================
    // 2. MENU HANDLERS
    // ==========================================================
    private static void handleManagerMenu(Scanner scanner) {
        while (true) {
            System.out.println("\n--- MANAGER MENU ---");
            System.out.println("1. Add Game from JSON");
            System.out.println("2. View Profits/Losses (MapReduce)");
            System.out.println("3. Soft Delete Game");
            System.out.println("4. Modify Game Settings");
            System.out.println("5. Restore/Activate Game");   
            System.out.println("0. EXIT (Back to Role Selection)");  
                                         
            
            int choice = readInteger(scanner, "Selection: ");
            try{
                switch (choice) {
                    case 1: addGameFromJson(scanner); break;
                    case 2: queryProfits(scanner); break;
                    case 3: softDeleteGame(scanner); break;
                    case 4: modifyGameSettings(scanner); break;
                    case 5: restoreGame(scanner); break;
                    case 0: 
                        System.out.println("\nLogging out to role selection..."); // exit
                        return;
                    default: System.out.println("\nInvalid choice. Please try again!");
            }
            } catch (CancelException e) {
                System.out.println("\n[!] Action cancelled. Returning to menu..."); 
            }
            
        }
    }

    private static void handlePlayerMenu(Scanner scanner, String playerName) {
        while (true) {
            System.out.println("\n--- PLAYER MENU ---");
            System.out.println("1. View All Available Games");
            System.out.println("2. Bet (Play)");
            System.out.println("3. Search Games (Filters)");
            System.out.println("4. Add Tokens (Balance)");
            System.out.println("5. View Balance");
            System.out.println("6. Rate a Game (1 to 5 *)");
            System.out.println("0.EXIT (Back to Role Selection)");
        
            int choice = readInteger(scanner, "Selection: ");

            try{
                switch (choice) {
                    case 1: viewAllGames(); break;
                    case 2: playGame(scanner, playerName); break;
                    case 3: searchGames(scanner); break;
                    case 4: addBalance(scanner, playerName); break;
                    case 5: viewBalance(scanner, playerName);break;
                    case 6: rateGame(scanner); break;
                    case 0: 
                        System.out.println("\nLogging out to role selection..."); // exit
                        return;
                    default: System.out.println("\nInvalid choice. Please try again!");
                }
            } catch (CancelException e) {
                System.out.println("\n[!] Action cancelled. Returning to menu...");
            }
        }
    }


    // ==========================================================
    // 3. MANAGER ACTIONS
    // ==========================================================
    private static void addGameFromJson(Scanner scanner) {
        String prompt = "\nEnter the path to the JSON file (e.g., Starburst.json)";
        String filePath = readString(scanner, prompt); // in case user wants to cancel option

        //extracts all file's context
        try {
            String jsonContent = new String(Files.readAllBytes(Paths.get(filePath)));
            Game newGame = new Game(
                extractString(jsonContent, "GameName"),
                extractString(jsonContent, "ProviderName"),
                extractInt(jsonContent, "Stars"),
                extractInt(jsonContent, "NoOfVotes"),
                extractString(jsonContent, "GameLogo"),
                extractDouble(jsonContent, "MinBet"),
                extractDouble(jsonContent, "MaxBet"),
                extractString(jsonContent, "RiskLevel"),
                extractString(jsonContent, "HashKey")
            );

            // file is sent to master for saving (creating socket)
            try (Socket socket = new Socket(MASTER_IP, MASTER_PORT);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                 
                out.writeObject("MANAGER_ADD");
                out.writeObject(newGame);
                out.flush();
                System.out.println("The game " + newGame.getGameName() + " was sent to the Master for saving.");
            }
        } catch (IOException e) {
            System.err.println("File reading or connection error: " + e.getMessage());
        }
    }

    private static void queryProfits(Scanner scanner) {
        System.out.println("\nSelect query type [or type 0 to cancel]:");
        System.out.println("1. By Provider");
        System.out.println("2. By Player");
        int typeChoice = readInteger(scanner, "Selection: ");
        
        if (typeChoice==0) {
            throw new CancelException(); // in case user wants to cancel option
        }

        String queryType = (typeChoice == 1) ? "PROVIDER" : "PLAYER";
        String prompt = "\nEnter the name of the " + queryType;
        String targetName = readString(scanner, prompt);

        // player's profits
        if (queryType.equals("PLAYER")) {
            if (!checkPlayerExists(targetName)) {
                System.out.println("\n[!] Error: Player '" + targetName + "' does not exist in the system!");
                return; 
            }
        // provider's profits
        } else if (queryType.equals("PROVIDER")) {
            if (!checkProviderExists(targetName)) {
                System.out.println("\n[!] Error: Provider '" + targetName + "' does not exist in the system!");
                return; 
            }
        }

        ManagerQueryRequest request = new ManagerQueryRequest(0, queryType, targetName);

        // sends request to master (creating socket)
        try (Socket socket = new Socket(MASTER_IP, MASTER_PORT);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            
            //sends label to master
            out.writeObject("MANAGER_AGGREGATE");
            out.writeObject(request);
            out.flush();

            System.out.println("\nWaiting for MapReduce Aggregation from Master...");

            //receives response from master
            Object response = in.readObject();

            if (response instanceof Double) {
                System.out.println("\n--- MAPREDUCE RESULT ---");
                System.out.println("Total Profit/Loss for " + targetName + ": " + response + " FUN");
            } else {
                System.out.println("\nUnexpected response."); // result not a double
            }
        } catch (Exception e) {
            System.err.println("\nError calculating profits: " + e.getMessage());
        }
    }

    private static void softDeleteGame(Scanner scanner) {
        //searches for requested game
        String gameName = getValidGameName(scanner);
        if (gameName == null) return;

        //sends request to master (creating socket)
        try (Socket socket = new Socket(MASTER_IP, MASTER_PORT);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
           
            //sends label to master
            out.writeObject("MANAGER_DELETE");
            out.writeObject(gameName);
            out.flush();

            //receives response from master
            String response = (String) in.readObject();
            
            if (response.equals("SUCCESS")) {
                System.out.println("\nServer says: Game '" + gameName + "' was successfully soft-deleted (set to inactive)!");
            } else {
                System.out.println("\nServer says: Failed to delete. Game not found."); // game not found
            }
        } catch (Exception e) {
            System.err.println("\nError: " + e.getMessage());
        }
    }

    private static void modifyGameSettings(Scanner scanner) {
        //searches for requested game
        String gameToMod = getValidGameName(scanner);
        if (gameToMod == null) return;

        //modify settings
        System.out.println("---Modify Settings for ' " + gameToMod + " ' ---");
        System.out.println("1. Change Risk Level");
        System.out.println("2. Change Betting Limits (Min/Max)");
        System.out.println("0. Cancel");
        int choice = readInteger(scanner, "Selection: ");

        if (choice == 0) {
            System.out.println ("Modification cancelled."); // user wants to cancel option
            return;
        }

        // sends request to master (creating socket)
        try (Socket socket = new Socket(MASTER_IP, MASTER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            
            //change of risk level
            if (choice == 1) {
                String newRisk = readRiskLevel(scanner, false); 
                
                out.writeObject("MANAGER_MODIFY_RISK");
                out.writeObject(gameToMod);
                out.writeObject(newRisk);
                out.flush();
                
            // change of bet limits
            } else if (choice == 2) {
                double newMinBet, newMaxBet;
                while (true) {
                    newMinBet = readDouble(scanner, "Enter new Minimum Bet:");
                    newMaxBet = readDouble(scanner, "Enter new Maximum Bet:");

                    if (newMinBet > 0 && newMinBet < newMaxBet) break;
                    System.out.println("ERROR: Minimum bet must be positive, and Maximum bet must be greater than Minimum bet!\n");
                }
                
                //sends label to master
                out.writeObject("MANAGER_MODIFY_LIMITS");
                out.writeObject(gameToMod);
                out.writeDouble(newMinBet);
                out.writeDouble(newMaxBet);
                out.flush();
                
            } else {
                System.out.println("Invalid choice. Cancelling.");
                return;
            }

            // receives master's response
            String response = (String) in.readObject();
            System.out.println("Master says: " + response);  
        } catch (CancelException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace(); 
        }
    }


    private static void restoreGame(Scanner scanner) {
        //searches for requested game
        String gameName = getValidGameName(scanner);
        if (gameName == null) return;

        try (Socket socket = new Socket(MASTER_IP, MASTER_PORT);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            
            //sends label to master
            out.writeObject("MANAGER_RESTORE"); 
            out.writeObject(gameName);
            out.flush();

            //receives response from master
            String response = (String) in.readObject();
            System.out.println("\nMaster says: " + response);
            
        } catch (Exception e) {
            System.err.println("\nError connecting to Master: " + e.getMessage());
        }
    }


    // ==========================================================
    // 4. PLAYER & COMMON ACTIONS
    // ==========================================================
    private static void viewAllGames() {
        // Setting the filters to allow all types of characteristiques
        SearchFilters allFilters = new SearchFilters("ALL", -1, "ALL"); // (stars=-1) to allow all

        //sends request to master (creating socket)
        try (Socket socket = new Socket(MASTER_IP, MASTER_PORT);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            //sends label to master
            out.writeObject("PLAYER_SEARCH");
            out.writeObject(allFilters);
            out.flush();

            // reads results coming from master
            System.out.println("\nRetrieving all available games with details...");
            Object response = in.readObject();

            //Prints the game list 
            if (response instanceof List<?>) {
                List<Game> results = (List<Game>) response;
                System.out.println("\n----------------- AVAILABLE GAMES LIST -----------------");
                for (Game g : results) {
                    System.out.println("Game: " + g.getGameName());
                    System.out.println("  > Provider: "  + g.getProviderName());
                    System.out.println("  > Limits:   "  + g.getMinBet() + " - " + g.getMaxBet() + " FUN"); // Bet limits
                    System.out.println("  > Risk:     "  + g.getRiskLevel()); // Level risk
                    System.out.println("  > Jackpot:  x" + g.getJackpot());   // Jackpot
                    System.out.println("  > Rating:   "  + g.getStars() + "/5 stars");
                    System.out.println("--------------------------------------------------------");
                }
            }
        } catch (Exception e) {
            System.err.println("Error retrieving games: " + e.getMessage());
        }
    }

     private static void playGame(Scanner scanner, String playerName) {
        //searches for requested game
        String gameName = getValidGameName(scanner);
        if (gameName == null) return;
        
        // reads bet
        double betAmount = readDouble(scanner, "Enter the bet you would like to place (e.g. 10.5): ");

        PlayRequest request = new PlayRequest(gameName, betAmount, playerName);
        
        //sends request to master (creating socket)
        try (Socket socket = new Socket(MASTER_IP, MASTER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                
            //sends label to master
            out.writeObject("PLAYER_PLAY");
            out.writeObject(request); 
            out.flush();

            System.out.println("\nThe bet has been sent. The draw is taking place (through SRG)...");
            String result = (String) in.readObject(); // receives results from master
            System.out.println("\nRESULT: " + result);
        } catch (Exception e) {
            System.err.println("\nError while placing a bet: " + e.getMessage());
        }
    }

    private static void searchGames(Scanner scanner) {
        System.out.println("\n--- Search Filters ---");
        String betCategory = readBetCategory(scanner, true); // bet choice

        int stars = readStars(scanner, "Stars [1-5] / -1(ALL): ",true); // star choice (-1 stars, and allowAll:true allows all options)

        String riskLevel = readRiskLevel(scanner, true); // risk choice

        SearchFilters filters = new SearchFilters(betCategory, stars, riskLevel);

        //sends request to master (creating socket)
        try (Socket socket = new Socket(MASTER_IP, MASTER_PORT);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
             
            // sends request's label to master
            out.writeObject("PLAYER_SEARCH"); 
            out.writeObject(filters);
            out.flush();

            System.out.println("\nWaiting for results from Master (MapReduce)...");
            Object response = in.readObject(); // receives response from master

            if (response instanceof List<?>) {
                List<Game> results = (List<Game>) response;
                System.out.println("\n" + results.size() + " games were found:");

                // prints games' list
                for (Game g : results) {
                    System.out.println("- " + g.getGameName() + " (Jackpot: " + g.getJackpot() + ")");
                }
            } else {
                System.out.println("\nUnexpected response from the Master."); // received result not a list
            }
        } catch (Exception e) {
            System.err.println("\nError while searching: " + e.getMessage());
        }
    
    }

    private static void addBalance(Scanner scanner, String playerName) {

    String prompt = "\nEnter amount of tokens to add";
    double amount = readDouble(scanner, prompt);

    //creating connection(socket) with master
    try (Socket socket = new Socket(MASTER_IP, MASTER_PORT);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            //sending request to master 
            out.writeObject("ADD_BALANCE");
            out.writeObject(playerName);
            out.writeDouble(amount);
            out.flush();

            //result sent from master
            String response = (String) in.readObject();
            System.out.println(response);

        } catch (Exception e) {
            System.err.println("Connection error: " + e.getMessage());
        }
    }

   private static void viewBalance(Scanner scanner, String playerName) {

        //Opening socket with master 
        try (Socket socket = new Socket(MASTER_IP, MASTER_PORT);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            //sending label to master
            out.writeObject("GET_BALANCE");
            out.writeObject(playerName);
            out.flush();

            //receiving label from master
            String response = (String) in.readObject();
            System.out.println("\n[BALANCE INFO]: " + response);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void rateGame(Scanner scanner) {
        //searches for requested game
        String validGameToRate = getValidGameName(scanner);
        if (validGameToRate == null) return;
        
       int stars = readStars(scanner, "Enter your rating (1 to 5 stars): ",false); // allowAll: false , in order to receive valid response

       // sends request to master (creating socket)
        try (Socket socket = new Socket(MASTER_IP, MASTER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            // sends request's label to master
            out.writeObject("PLAYER_RATE");
            out.writeObject(validGameToRate); 
            out.writeObject(stars);
            out.flush();

            String reply = (String) in.readObject(); // receives response from master
            System.out.println("\nServer says: " + reply + "\n");
        } catch (Exception e) {
            System.err.println("Communication error: " + e.getMessage());
        }
    }


    // ==========================================================
    // 5. HELPER UTILITY METHODS
    // ==========================================================
    private static void registerPlayer(Scanner scanner) {
        String prompt = "\nEnter player's name";
        String username = readString(scanner, prompt);

        //opening socket with master
        try (Socket socket = new Socket(MASTER_IP, MASTER_PORT);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            //sending request to master
            out.writeObject("ADD_PLAYER");
            out.writeObject(username);
            out.flush();

            // receiving response from master
            String response = (String) in.readObject();
            System.out.println("\n" + response);

        } catch (Exception e) {
            System.err.println("Registration Error: " + e.getMessage());
        }
    }  
    private static String getValidGameName(Scanner scanner) {
        while (true) {
            String prompt = "\nEnter the name of the game";
            String gameName = readString(scanner, prompt);

            boolean exists = false;
            // sends request to master (creating socket)
            try (Socket checkSock = new Socket(MASTER_IP, MASTER_PORT); 
                ObjectOutputStream checkOut = new ObjectOutputStream(checkSock.getOutputStream());
                ObjectInputStream checkIn = new ObjectInputStream(checkSock.getInputStream())) {
                 
                //sends request's label 
                checkOut.writeObject("CHECK_GAME_EXISTS");
                checkOut.writeObject(gameName);
                checkOut.flush();
                
                exists = (boolean) checkIn.readObject(); // receives response from master
            } catch (Exception e) { 
                System.err.println("Communication error checking game: " + e.getMessage());
                throw new CancelException(); 
            }
            
            if (exists) { //valid game found
                return gameName; 
            } else {
                System.out.println("Error: Game '" + gameName + "' not found! Please try again.");
            }
        }
    }
    
    private static boolean checkPlayerExists(String playerName) {
        // sends request to master (creating socket)
        try (Socket socket = new Socket(MASTER_IP, MASTER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
             
            // sends request's label
            out.writeObject("CHECK_PLAYER_EXISTS");
            out.writeObject(playerName);
            out.flush();
            
            return (boolean) in.readObject(); // directly sends received results (true-> player exists, false->player doesn't exist)
        } catch (Exception e) {
            System.err.println("Communication error checking player: " + e.getMessage());
            return false;
        }
    }


    private static boolean checkProviderExists(String providerName) {
        // sends request to master (creating socket)
        try (Socket socket = new Socket(MASTER_IP, MASTER_PORT);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
             
            // sends request's label
            out.writeObject("CHECK_PROVIDER_EXISTS");
            out.writeObject(providerName);
            out.flush();
            
            return (boolean) in.readObject(); // directly sends received results (true-> provider exists, false->provider doesn't exist)
        } catch (Exception e) {
            System.err.println("Communication error checking provider: " + e.getMessage());
            return false;
        }
    }

    private static int readInteger(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt); // prints sent prompt
            try {
                return Integer.parseInt(scanner.nextLine().trim()); // reads user's input (number option)
            } catch (NumberFormatException e) {
                System.out.println("ERROR: Please enter a valid whole number."); //wrong input
            }
        }
    }

    private static double readDouble(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt + " [or type 0 to cancel]: "); // prints sent prompt
            String input = scanner.nextLine().trim(); // reads user's input 
            if (input.equals("0")) {
                throw new CancelException(); // user wants to cancel option
            }
            try {
                return Double.parseDouble(input.replace(",", "."));
            } catch (NumberFormatException e) {
                System.out.println("ERROR: Please enter a valid number.\n");
            }
        }
    }

    private static String readString(Scanner scanner, String prompt) {
        System.out.print(prompt + " [or type 0 to cancel]: "); // prints sent prompt
        String input = scanner.nextLine().trim(); // reads user's input
        if (input.equals("0")) {
            throw new CancelException(); // user wants to cancel option
        }
        return input;
    }

    private static String extractString(String json, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"(.*?)\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : "";
    }

    private static int extractInt(String json, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*([0-9]+)");
        Matcher m = p.matcher(json);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    private static double extractDouble(String json, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*([0-9.]+)");
        Matcher m = p.matcher(json);
        return m.find() ? Double.parseDouble(m.group(1)) : 0.0;
    }

    private static boolean isSystemEmpty() {
        // sends request to master (creates socket)
        try (Socket socket = new Socket(MASTER_IP,MASTER_PORT);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            // sends request's label
            out.writeObject("CHECK_SYSTEM_EMPTY");
            out.flush();
            return (boolean) in.readObject(); //Returns true if system is empty(no added games from manager)
        
        } catch (Exception e) {
            System.err.println("Error checking system status: " + e.getMessage());
            return true; // If Master crashes, we don't let the player enter for safety reasons
        }
    } 

    private static String readRiskLevel (Scanner scanner, boolean allowAll) {
        while (true) { 
            String prompt;
            if (allowAll) {
                prompt ="Enter Risk Level (Low, Medium, High, ALL)";
            } else {
                prompt ="Enter Risk Level (Low, Medium, High)";
            }

            String input = readString(scanner, prompt); // reads new risk level

            if (input.equalsIgnoreCase("Low")) return "Low";
            if (input.equalsIgnoreCase("Medium")) return "Medium";
            if (input.equalsIgnoreCase("High")) return "High";
            if (allowAll && input.equalsIgnoreCase("ALL")) return "ALL";

            System.out.println("ERROR: Invalid input! Please type exaclty what is in the parentheses.\n"); // invalid input
        }
    }

    private static String readBetCategory (Scanner scanner, boolean allowAll) {
        while (true) { 
            String prompt;
            if (allowAll) {
                prompt = "Enter Bet Category ($, $$, $$$, ALL)";
            } else {
                prompt = "Enter Bet Category ($, $$, $$$)";
            }

            String input = readString(scanner, prompt); // reads new bet category

            if (input.equals("$")) return "$";
            if (input.equals("$$")) return "$$";
            if (input.equals("$$$")) return "$$$";
            if (allowAll && input.equals("ALL")) return "ALL";

            System.out.println("ERROR: Invalid input! Please type exactly $, $$, $$$, or ALL)\n"); // invalid input
        }
    }

    private static int readStars(Scanner scanner, String prompt,boolean allowAll) {
        while (true) {
            int stars = readInteger(scanner, prompt + " [or type 0 to cancel]: ");
            if (stars == 0) {
                throw new CancelException(); // user wants to cancel option
            }
            if (allowAll && stars == -1) {
            return -1; // for "view all games" -> allows all stars (1-5)
            }
            if (stars >= 1 && stars <= 5) {
                return stars;
            }
            System.out.println("ERROR: Please enter a number between 1 and 5.\n");  // invalid input
        }
    }

    // Custom Exception for exiting/canceling selected option (pressing 0)
    static class CancelException extends RuntimeException {}
}