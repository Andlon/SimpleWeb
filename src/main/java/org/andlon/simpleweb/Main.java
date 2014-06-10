package org.andlon.simpleweb;

import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        try {
            System.out.println("Starting web server on port 9000...");
            SimpleWebServer server = new SimpleWebServer(9000);
            server.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
