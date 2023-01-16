package services;

import models.GroupChat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

public class ServerService {
    public static final int PORT = 28822;
    public static LinkedList<ClientSocketService> serverList = new LinkedList<>();
    public static LinkedList<GroupChat> groupList = new LinkedList<>();

    public void start() {
        try {
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(PORT);
                while(true) {
                    Socket socket = serverSocket.accept();
                    try {
                        serverList.add(new ClientSocketService(socket));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        serverList.removeIf(item -> item.socket == socket);
                        socket.close();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

