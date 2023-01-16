package services;

import models.Client;
import models.GroupChat;

import java.io.*;
import java.net.Socket;
import java.util.stream.Stream;

public class ClientSocketService extends Thread {
    public Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private Client state;

    public ClientSocketService(Socket socket) throws IOException {
        this.socket = socket;
        in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));
        state = new Client();
        send("Welcome to global chat, send you nickname!!!\n");
        start();
    }

    @Override
    public void run() {
        String text;
        try {
            while(true) {
                text = in.readLine();
                if(state.getNickName().isEmpty()) {
                    state.setNickName(text);
                    send("Okey, now your nickname is " + state.getNickName() + "\n" +
                            "/commands -> show Commands\n\n");
                    sendAll(state.getNickName() + " joined the chat!\n");
                } else if(text.contains("/")) {
                    String command = text.substring(text.indexOf("/") + 1);
                    switch (command) {
                        case "users":
                            StringBuilder builder = new StringBuilder();
                            for(ClientSocketService clientSocket : ServerService.serverList) {
                                builder.append(clientSocket.state.getNickName() + ", ");
                            }
                            this.send(builder.toString() + "\n");
                            break;
                        case "commands":
                            this.send("-------Commands-------\n" +
                                    "/users -> get all users\n" +
                                    "/m -> sms\n" +
                                    "--GROUP--\n" +
                                    "/createGroup [Name] [User1] [User2]..\n" +
                                    "/removeGroup [Name]\n" +
                                    "/g [Group] [Message] -> chat\n" +
                                    "/gAdd [Group] [User1] [User2].. -> add User in Group\n" +
                                    "gRemove [Group] [User1] [User2].. -> remove User from Group\n" +
                                    "/gAddAdmin [Group] [User1] [User2].. -> change Role to Admin\n" +
                                    "/gRemoveAdmin [Group] [User1] [User2].. -> change Role to Normal\n" +
                                    "------------------------\n");
                        default:
                            String[] arr = command.split(" ");
                            if (command.startsWith("m ") && arr.length >= 2){
                                String name = arr[1];
                                String message = command.substring(command.indexOf(name) + name.length());
                                findClientsByName(name)
                                        .forEach(client -> {
                                            System.out.println(client.state.getNickName() + message);
                                            try {
                                                client.send(state.getNickName() + "-->you:" + message + "\n");
                                            } catch (IOException e) {
                                                System.out.println("err");
                                            }
                                        });
                            }
                            else if(command.startsWith("createGroup ") && arr.length >= 3){
                                String groupName = arr[1];
                                GroupChat searchGroup = findGroup(groupName);
                                if (searchGroup != null){
                                    this.send("Error! A group with the same name already exists (" + groupName + ")\n");
                                    break;
                                }
                                GroupChat group = new GroupChat(groupName, this);
                                ServerService.groupList.add(group);
                                String userName;
                                for (int i = 2; i < arr.length; i++) {
                                    userName = arr[i];
                                    ClientSocketService client = findClientsByName(userName).findFirst().orElse(null);
                                    if (client != null){
                                        if (group.addClient(client)){
                                            client.send("You have joined the group " + groupName + " !\n" +
                                                    "To send message: /g " + groupName + " [message]\n");
                                        }
                                    }
                                }
                                this.send("Group " + groupName + " has created!\n" +
                                        "To send message: /g " + groupName + " [message]\n");
                            }
                            else if(command.startsWith("removeGroup ") && arr.length == 2){
                                String groupName = arr[1];
                                GroupChat group = findGroup(groupName);
                                if (group == null){
                                    this.send("Error! There is no group with this name (" + groupName + ")\n");
                                    break;
                                }
                                if (group.isAdmin(this)){
                                    sendGroup(group, "[" + groupName + "] The admin has deleted this group\n");
                                    ServerService.groupList.remove(group);
                                    this.send("Group " + groupName + " has removed!\n");
                                }
                                else{
                                    this.send("You have not permission \n");
                                }
                            }
                            else if (command.startsWith("g ") && arr.length >= 3){
                                String groupName = arr[1];
                                String message = command.substring(command.indexOf(groupName) + groupName.length());
                                GroupChat group = findGroup(groupName);
                                if (group == null){
                                    this.send("Error! Group not found\n");
                                }
                                else if (group.isClient(this)){
                                    boolean admin = group.isAdmin(this);
                                    sendGroup(group, "[" + (admin ? "(A)" : "") + groupName + "]"
                                            + state.getNickName() + ":" + message + "\n");
                                    break;
                                }
                                else{
                                    this.send("Error! You are not group member\n");
                                }
                            }
                            else if (command.startsWith("gAdd ") || command.startsWith("gRemove ") && arr.length >= 3){
                                boolean add = command.startsWith("gAdd ");
                                String groupName = arr[1];
                                GroupChat group = findGroup(groupName);
                                if (group == null){
                                    this.send("Error! Group not found\n");
                                }
                                else if (group.isAdmin(this)){
                                    String userName;
                                    for (int i = 2; i < arr.length; i++){
                                        userName = arr[i];
                                        ClientSocketService client = findClientsByName(userName).findFirst().orElse(null);
                                        if (client != null){
                                            if (add){
                                                if (group.addClient(client)){
                                                    client.send("You have joined the group " + groupName + " !\n" +
                                                            "To send message: /g " + groupName + " [message]\n");
                                                    client.sendGroup(group, "[" + groupName + "] " + client.state.getNickName() + " joined the group\n");
                                                }
                                            }
                                            else{
                                                group.removeClientFromGroup(client);
                                                client.send("[" + groupName + "] " + "You have been kicked out of the group \n");
                                                client.sendGroup(group, "[" + groupName + "] " + client.state.getNickName() + " has been kicked out of the group\n");
                                            }

                                        }
                                        else{
                                            this.send(userName + " not found!\n");
                                        }
                                    }
                                    break;
                                }
                                else{
                                    this.send("You have not permission \n");
                                }
                            }
                            else if (command.startsWith("gAddAdmin ") && arr.length >= 3) {
                                String groupName = arr[1];
                                GroupChat group = findGroup(groupName);
                                if (group == null){
                                    this.send("Error! Group not found\n");
                                }
                                else if (group.isAdmin(this)){
                                    String userName;
                                    for (int i = 2; i < arr.length; i++){
                                        userName = arr[i];
                                        ClientSocketService client = findClientsByName(userName).findFirst().orElse(null);
                                        if (client != null){
                                            if (group.addAdmin(client)){
                                                this.send(client.state.getNickName() + " is Admin \n");
                                                client.send("You are admin in " + groupName + " !\n");
                                            }
                                        }
                                        else{
                                            this.send(userName + " not found!\n");
                                        }
                                    }
                                    break;
                                }
                                else{
                                    this.send("You have not permission \n");
                                }
                            }
                            else if (command.startsWith("gRemoveAdmin ") && arr.length >= 3) {
                                String groupName = arr[1];
                                GroupChat group = findGroup(groupName);
                                if (group == null){
                                    this.send("Error! Group not found\n");
                                }
                                else if (group.isAdmin(this)){
                                    String userName;
                                    for (int i = 2; i < arr.length; i++){
                                        userName = arr[i];
                                        ClientSocketService client = findClientsByName(userName).findFirst().orElse(null);
                                        if (client != null){
                                            if (group.removeAdmin(client)){
                                                this.send(client.state.getNickName() + " no longer an admin \n");
                                                client.send("You are not admin in " + groupName + " !\n");
                                            }
                                            else {
                                                this.send(client.state.getNickName() + " is not an admin \n");
                                            }
                                        }
                                        else{
                                            this.send(userName + " not found!\n");
                                        }
                                    }
                                    break;
                                }
                                else{
                                    this.send("You have not permission \n");
                                }
                            }
                            else{
                                this.send(text + " unknown command!\n");
                            }
                    }
                }
                else {
                    boolean isStop = text.equals("stop");
                    if(!isStop) {
                        text += "\n";
                    }
                    else
                        break;
                    System.out.println("server catch message: " + text);
                    System.out.println("count socket: " + ServerService.serverList.size());
                    this.sendAll("[Global]" + state.getNickName() + ": " + text);
                }
            }
        } catch (Exception e) {
            System.out.println("Error");
        }
        finally {
            disconnect();
        }
    }

    private void sendAll(String message) throws IOException {
        for(ClientSocketService clientSocket : ServerService.serverList) {
            if(clientSocket.socket != this.socket) {
                clientSocket.send(message);
            }
        }
    }

    private GroupChat findGroup(String groupName){
        for (GroupChat group : ServerService.groupList){
            if (group.name.equals(groupName)){
                return group;
            }
        }
        return null;
    }

    private void sendGroup(GroupChat group, String message) throws IOException {
        for(ClientSocketService clientSocket : group.clients) {
            if(clientSocket.socket != this.socket) {
                clientSocket.send(message);
            }
        }
    }

    private void send(String message) throws IOException {
        out.write(message);
        out.flush();
    }

    private Stream<ClientSocketService> findClientsByName(String name){
        return ServerService.serverList.stream()
                .filter(client -> client.state.getNickName().equals(name));
    }

    private void disconnect(){
        try {
//                e.printStackTrace();
            ServerService.serverList.removeIf(item -> item.socket == socket);
            ServerService.groupList.removeIf(item -> item.removeClientFromGroup(this));
            this.socket.close();
            this.sendAll(state.getNickName() + " has left the chat!\n");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }
}

