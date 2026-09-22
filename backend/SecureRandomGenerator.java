import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.LinkedList;
import java.util.Random;

/**
 * The Secure Random Generator (SRG) is a standalone server.
 * It uses the Producer-Consumer pattern to manage a pool of random numbers.
 */

public class SecureRandomGenerator {
    private int port;
    private final CustomBuffer buffer; //shared storage space for the numbers

    public SecureRandomGenerator(int port, int bufferCapacity) {
        this.port = port;
        //Initializing our own thread-safe buffer with a specific capacity
        // The buffer is the synchronization point between the Producer and the SRGHandlers
        this.buffer = new CustomBuffer(bufferCapacity);
    }

    public void startServer() {
        //Starting the producer's thread
        // This background thread works independently to keep the buffer full.
        Thread producerThread = new Thread(new Producer(buffer));
        producerThread.start();

        //Starting the TCP that will be receiving connections from the Workers
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Secure Random Generator started and listening on port: " + port);

            while (true) {
                //Waiting (blocking) until a Worker connects.
                Socket workerSocket = serverSocket.accept();
                System.out.println("New random number request from Worker!");

                //Creating a new thread (Consumer) to serve the Worker
                //This way the central Server can immediately accept the next Worker (MULTI-THREADING)
                new SRGHandler(workerSocket, buffer).start();
            }
        } catch (IOException e) {
            System.out.println("SRG Server startup error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        //Starting the generator on a port (e.g. 7000) with a buffer of 100 positions
        SecureRandomGenerator srg = new SecureRandomGenerator(Config.SRG_PORT, 100);
        srg.startServer();
    }
}




/**
 * THREAD-SAFE BUFFER (The Monitor)
 * Uses wait/notify mechanism to manage the flow of numbers.
 */
class CustomBuffer {
    private LinkedList<Integer> list = new LinkedList<>();
    private int capacity;

    public CustomBuffer(int capacity) {
        this.capacity = capacity;
    }

    //The method that the Producer calls to put a number into the list
    //synchronized -> only one thread can run this method at a time
    public synchronized void produce(int number) throws InterruptedException {
        
        //While the list is full, the Producer must wait
        //Using 'while' and not 'if' to protect against spurious wakeups
        //(cases where the thread wakes up for no reason)

        // Each time the control goes to the beginning of the loop and re-checks the condition
        while (list.size() == capacity) {
            wait(); //The thread waits and releases the object's "key"
        }

        list.add(number); //add the number to the end of the list

        //Notifying all waiting threads (e.g. Consumers waiting because the list was empty)
        notifyAll();
    }
    
    //The moment the worker asks for a random number to see if the player won
    //The method the Consumer (SRGHandler) calls to get a number from the list
    public synchronized int consume() throws InterruptedException {
        // While the list is empty, the Consumer must wait (has nothing to pull)
        while (list.isEmpty()) {
            wait();
        }

        //Getting and removing the first available number from the list
        int number = list.removeFirst();

        notifyAll(); //Notifying Producer
        return number;
    }
}

//Generates random numbers continuously and tries to put them in the buffer
//if the buffer is full, calling buffer.produce() will put it to sleep
class Producer implements Runnable {
    private CustomBuffer buffer;
    private Random random;

    public Producer(CustomBuffer buffer) {
        this.buffer = buffer;
        this.random = new Random();
    }

    @Override
    public void run() {
        while (true) { //Endless Production Loop
            try {
               // Generating a positive random integer number
                int randomNumber = Math.abs(random.nextInt(1000000));

                //Simulation of the "slight delay" when generating the number.
                Thread.sleep(50);
                buffer.produce(randomNumber); //placing in the buffer 
            } catch (InterruptedException e) {
                System.err.println("\nProducer interrupted.");
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}

/**
 * SRG HANDLER: The "Clerk"
 * Serves the Worker and signs the number with a SHA-256 hash.
 */
class SRGHandler extends Thread {
    private Socket socket;
    private CustomBuffer buffer;

    public SRGHandler(Socket socket, CustomBuffer buffer) {
        this.socket = socket;
        this.buffer = buffer;
    }
     
    @Override
    public void run() {
        try (
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ) {
            String request = (String) in.readObject();

            if ("GET_NUMBER".equals(request)) {
                //reads the secret key associated with the game
                String secretKey = (String) in.readObject();

                //Taking the first available number from the buffer
                int randomNumber = buffer.consume();

                // CRYPTOGRAPHIC SIGNING
                // combining the random number + the game's secret key
                String rawData = randomNumber + secretKey;
                String hash = generateSHA256(rawData);

                // Send both the number and the signature to the Worker
                out.writeInt(randomNumber);
                out.writeObject(hash);
                out.flush(); //Emptying the stream buffer so the data leaves immediately
            }
        } catch (Exception e) {
            System.err.println("\nError serving Worker: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * SHA-256 Generator
     * This creates the "Proof" that the number was generated by the SRG
     * and not manipulated by the Worker.
     * Takes a string and returns the SHA-256 in hexadecimal String format
     */
    private String generateSHA256(String input) {
        try {
            // 1. Initializing the SHA-256 hashing algorithm from the Java Security library
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // 2. Converting the input string to bytes using UTF-8 encoding and "digest" it
            byte[] hashBytes = digest.digest(input.getBytes("UTF-8"));

            // 3. Preparing to convert the raw bytes into a human-readable Hexadecimal string
            StringBuilder hexString = new StringBuilder();

            // 4. Iterating through each byte in the 32-byte hash array
            for (byte b: hashBytes) {
                String hex = Integer.toHexString(0xff & b); // converting the byte to a Hex string.
                if (hex.length() == 1) {
                    hexString.append('0'); // zero-padding
                }
                hexString.append(hex);
            }
            return hexString.toString(); //returns the final 64-character hexadecimal string

        } catch (Exception e) {
            throw new RuntimeException("\nFailed to calculate SHA-256", e);
        }
    }
}



