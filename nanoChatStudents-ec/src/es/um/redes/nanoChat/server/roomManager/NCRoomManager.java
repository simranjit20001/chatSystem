package es.um.redes.nanoChat.server.roomManager;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import es.um.redes.nanoChat.messageFV.NCChatMessage;
import es.um.redes.nanoChat.messageFV.NCRoomMessage;
import es.um.redes.nanoChat.server.User;


public class NCRoomManager {

	String roomName;
    long lastMessageTime;
    HashMap<User, DataOutputStream> users;
    Set<User> admins;
    HashSet<User> blocked;
    List<String> history;

    public NCRoomManager (String roomName) {
        this.roomName = roomName;
        users = new HashMap<User, DataOutputStream>();
        lastMessageTime = Instant.now().toEpochMilli();
        admins = new HashSet<User>();
        history = new LinkedList<String>();
        blocked = new HashSet<User>();
    }

	public boolean registerUser(User u, Socket s) {

        if(blocked.contains(u)){
            return false;
        }

        boolean result = false;
        try {
            result = users.putIfAbsent(u, new DataOutputStream(s.getOutputStream())) == null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }


    public void broadcastMessage(User u, NCChatMessage message) throws IOException {

        if(!users.containsKey(u)){
            return ; 
        }

        for (DataOutputStream dos : users.values()) {
            dos.writeUTF(message.toEncodedString());
        }
        
        Instant now = Instant.now();
        history.add("[" + now.toString() + "] " + message.getValue() + ": " + message.getMessage());
        lastMessageTime = now.toEpochMilli();

    }

    public List<String> getHistory(User u) {

        if(!users.containsKey(u)){
            return new ArrayList<String>();
        }

        return Collections.unmodifiableList(history);
    }
   
    public boolean removeAdmin(User u) {
        return admins.remove(u);
    }

    public boolean addAdmin(User u) {        
        return users.keySet().contains(u) && admins.add(u);
        //if the user is not in the "users" returns false, else, returns whether admin was added or not (if the user is already an admin, it returns false)
    }
    public void removeUser(User u) {
        removeAdmin(u);
        users.remove(u);
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public NCRoomDescription getDescription() {
        List<String> usersName  = users.keySet().stream().map(User::getName).collect(Collectors.toList());
        List<String> adminsName  = admins.stream().map(User::getName).collect(Collectors.toList());
        return new NCRoomDescription(roomName, 
                usersName, adminsName, lastMessageTime);
    }

    public int usersInRoom() {
        return users.keySet().size();
    }

    public boolean isUser(User nick){
        return users.keySet().contains(nick);
    }

    public boolean isAdmin(User u) {
        return admins.contains(u);
    }

    public int adminsInRoom() {
        return admins.size();
    }

    public void blockUser(User u) {
        blocked.add(u);
        users.remove(u);
    }

    public void unblockUser(User u) {
        blocked.remove(u);
    }

    public void addRandomAdmin() {
        if (users.size() > 0) {
            List<User> valuesList = new ArrayList<User>(users.keySet());
            int randomIndex = new Random().nextInt(valuesList.size());
            User randomUser = valuesList.get(randomIndex);
            addAdmin(randomUser);
        }
    }


    public void broadcastSystemMessage(NCRoomMessage advertisingMessage)  throws IOException {
        for (DataOutputStream dos : users.values()) {
            dos.writeUTF(advertisingMessage.toEncodedString());
        }
    }

    public boolean isBlocked(User u) {
        return blocked.contains(u);
    }

    public void clearHistory() {
        history.clear();
    }

    public void sendPrivateMessage(User user, String message) throws IOException {
        
        DataOutputStream dos = users.get(user);
        dos.writeUTF(message);

    }

    public String getRoomName() {
        return roomName;
    }



}


