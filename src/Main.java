import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        try {
            System.out.println("Starting web server on port 8080...");
            SimpleWebServer server = new SimpleWebServer(8080);
            server.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
