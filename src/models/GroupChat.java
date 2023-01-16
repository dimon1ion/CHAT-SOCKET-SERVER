package models;

import services.ClientSocketService;
import services.ServerService;

import java.util.LinkedHashSet;
import java.util.LinkedList;

public class GroupChat {
    public String name;

    public LinkedHashSet<ClientSocketService> clients = new LinkedHashSet<>();
    private LinkedHashSet<ClientSocketService> admins = new LinkedHashSet<>();

    public GroupChat(String name, ClientSocketService admin) {
        this.name = name;
        this.clients.add(admin);
        this.admins.add(admin);
    }

    public boolean addClient(ClientSocketService client){
        return addClient(client, false);
    }

    public boolean addClient(ClientSocketService client, boolean admin){
        if (clients.add(client)){
            if (admin){
                admins.add(client);
            }
            return true;
        }
        return false;
    }

    public boolean addAdmin(ClientSocketService client){
        if (clients.contains(client)){
            admins.add(client);
            return true;
        }
        return false;
    }

    public boolean removeAdmin(ClientSocketService client){
        return admins.remove(client);
    }

    public boolean isAdmin(ClientSocketService client){
        return admins.contains(client);
    }

    public boolean isClient(ClientSocketService client){
        return clients.contains(client);
    }

    public boolean removeClientFromGroup(ClientSocketService client){
        if (clients.remove(client)){
            admins.remove(client);
            if (clients.size() == 0){
                ServerService.groupList.remove(this);
                return true;
            }
        }
        return false;
    }
}
