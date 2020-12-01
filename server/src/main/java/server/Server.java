package server;

import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

public class Server {
    private static final Logger logger = Logger.getLogger(Server.class.getName());

    private List<ClientHandler> clients;
    private AuthService authService;

    public Server() {
        clients = new CopyOnWriteArrayList<>();
//        authService = new SimpleAuthService();
        authService = new DBAuthService();
        ServerSocket server = null;
        Socket socket = null;
        final int PORT = 8189;


        try {
            server = new ServerSocket(PORT);
            logger.info("Server started");
            System.out.println("Server started");


            while (true) {
                socket = server.accept();
                new ClientHandler(this, socket);
            }

        } catch (IOException e) {
            e.printStackTrace();
            logger.info("Server starting error...");
            System.out.println("Server starting error...");
        } finally {
            DBAuthService.disconnect();
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                logger.info("Server stopped");
                System.out.println("Server stopped");
                server.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void broadcastMsg(ClientHandler sender, String msg) {
        String message = String.format("[ %s ]: %s", sender.getNickname(), msg);
        for (ClientHandler c : clients) {
            c.sendMsg(message);
        }
    }

    public void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        broadcastClientList();
    }

    public void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastClientList();
    }

    public AuthService getAuthService() {
        return authService;
    }

    public void sendMsgToReceiver(ClientHandler sender, String nameOfReceiver, String msg) {
        String message;
        if (sender.getNickname().equals(nameOfReceiver)) {
            message = String.format("[ To yourself]: %s", msg);
            sender.sendMsg(message);
            return;
        }
        message = String.format("[ From %s ]: %s", sender.getNickname(), msg);
        for (ClientHandler c : clients) {
            if (c.getNickname().equals(nameOfReceiver) &&
                    !sender.getNickname().equals(nameOfReceiver)) {
                c.sendMsg(message);
                message = String.format("[ To %s ]: %s", c.getNickname(), msg);
                sender.sendMsg(message);
                return;
            }
        }
        message = String.format("[ To %s ]: this user is not log in....", nameOfReceiver);
        sender.sendMsg(message);
    }

    public boolean isLoginAuthenticated(String login) {
        for (ClientHandler c : clients) {
            if (c.getLogin().equals(login)) {
                return true;
            }
        }
        return false;
    }

    public void broadcastClientList() {
        StringBuilder sb = new StringBuilder("/clientlist ");
        for (ClientHandler c : clients) {
            sb.append(c.getNickname()).append(" ");
        }
        sb.setLength(sb.length() - 1);
        String message = sb.toString();
        for (ClientHandler c : clients) {
            c.sendMsg(message);
        }
        System.out.println("Отправляеем Список клиентов: " + sb.toString());
    }

    public void sendHistory(ClientHandler receiver) {

    }
}
