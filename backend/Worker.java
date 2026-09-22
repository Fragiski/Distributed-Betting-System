import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import com.example.casinoapp.Game;
import com.example.casinoapp.Player;

public class Worker {
    private int port;

    private Map<String, Game> localGames;
    private Map<String, Player> localPlayers;

    public Worker(int port) {
        this.port = port;
        this.localGames = new HashMap<>();
        this.localPlayers = new HashMap<>();
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Worker started and is listening on port: " + port);

            //Endless loop to continuously accept connections from the Master
            while (true) {
                Socket masterSocket = serverSocket.accept();
                System.out.println("New connection from Master to Worker " + port);

                //multi-threading
                new WorkerHandler(masterSocket, localGames, localPlayers).start();
            }
        } catch (IOException e) {
            System.err.println("Error starting Worker on port " + port + ": " + e.getMessage());
        }
    }


    /**
    * Main method to start a worker instance.
    * The port is provided as a command-line argument to allow 
    * starting multiple workers on the same machine dynamically.
    */
    public static void main(String[] args) {
        if (args.length < 1) {
        System.out.println("Usage: java Worker <port>");
        return;
        }

        // Initializing and starting the worker server on the specified port
        int port = Integer.parseInt(args[0]);
        Worker worker = new Worker(port);

        worker.startServer();
    }
}


