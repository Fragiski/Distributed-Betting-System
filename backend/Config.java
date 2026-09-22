import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.ArrayList;
import java.util.List;

public class Config {
    public static String MASTER_IP;
    public static int MASTER_PORT;
    public static String REDUCER_IP;
    public static int REDUCER_PORT;
    public static String SRG_IP;
    public static int SRG_PORT;
    public static List<WorkerInfo> WORKERS = new ArrayList<>();

    //The static block is executed once when the class is loaded.
    static {
        Properties prop = new Properties();
        try (FileInputStream input = new FileInputStream("config.properties")) {
            prop.load(input);

            MASTER_IP = prop.getProperty("master.ip");
            MASTER_PORT = Integer.parseInt(prop.getProperty("master.port"));

            REDUCER_IP = prop.getProperty("reducer.ip");
            REDUCER_PORT = Integer.parseInt(prop.getProperty("reducer.port"));

            SRG_IP = prop.getProperty("srg.ip");
            SRG_PORT = Integer.parseInt(prop.getProperty("srg.port"));

            //Separating workers (e.g. "127.0.0.1:6001,127.0.0.1:6002")
            String[] workerArray = prop.getProperty("workers").split(",");
            for (String w : workerArray) {
                String[] parts = w.split(":");
                WORKERS.add(new WorkerInfo(parts[0], Integer.parseInt(parts[1])));
            }
        } catch (IOException ex) {
            System.err.println("Error reading config.properties: " + ex.getMessage());
            // Setting some default values ​​in case of an error so it doesn't crash
            MASTER_IP = "127.0.0.1";
            MASTER_PORT = 5555;
        }
    }
}
