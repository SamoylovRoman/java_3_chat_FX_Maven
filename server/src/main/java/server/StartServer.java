package server;

import sun.rmi.runtime.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class StartServer {
    private static final Logger logger = Logger.getLogger("");

    public static void main(String[] args) {

        LogManager logManager = LogManager.getLogManager();
        try {
            logManager.readConfiguration(new FileInputStream("logging.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        new Server();
    }
}
