package es.um.redes.nanoChat.server;

import java.net.InetAddress;
import java.net.Socket;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;
import es.um.redes.nanoChat.server.roomManager.NCRoomManager;

/**
 * Esta clase contiene el estado general del servidor (sin la lógica relacionada con cada sala particular)
 */
class NCServerManager {



	//Usuarios registrados en el servidor
	private HashMap<String, User> users = new  HashMap<String, User>();	
	//Habitaciones creadas por los usuarios
	private Map<String,NCRoomManager> rooms = new HashMap<String,NCRoomManager>();
	//Habitaciones creadas por defecto
	private final Set<String> defaultRooms;

	NCServerManager(Collection<String> defaultRooms, Collection<String> reservedNames) {
		
		for(String name : reservedNames) {
			users.put(name, null); 
		}
		
		this.defaultRooms = new HashSet<String>(defaultRooms);
		for(String room : defaultRooms){
			registerRoomManager(room);
		}

	}

	public User getUser(String name) {
		return users.get(name);
	}

	//Método para registrar un RoomManager
	public boolean registerRoomManager(String id) {
		if (rooms.containsKey(id)) {
			return false;
		}
		NCRoomManager roomManager = new NCRoomManager(id);
		rooms.put(id, roomManager);
		
		return true;
	}

	//Devuelve la descripción de las salas existentes
	public synchronized List<NCRoomDescription> getRoomList() {

		List<NCRoomDescription> roomList = 
				rooms.values().stream()
				.map(NCRoomManager::getDescription)
				.collect(java.util.stream.Collectors.toList());

		return roomList;
	}

	public synchronized boolean isRoomNameAvailable(String id) {
		return !rooms.containsKey(id);
	}


	//Intenta registrar al usuario en el servidor.
	public synchronized boolean addUser(String user, InetAddress address) {
		return users.putIfAbsent(user, new User(user, address)) == null;
	}

	//Elimina al usuario del servidor
	public synchronized void removeUser(User user) {
		//TOD Elimina al usuario del servidor
		users.remove(user.getName());
	}

	//Un usuario solicita acceso para entrar a una sala y registrar su conexión en ella
	public synchronized NCRoomManager enterRoom(User u, String room, Socket s) {

		NCRoomManager rm = rooms.get(room);
		if(rm != null && rm.registerUser(u, s)) { //Si la sala existe y se ha podido registrar al usuario
			return rm;
		}
		
		return null;

	}


	//Un usuario deja la sala en la que estaba
	public synchronized void leaveRoom(User u, String room) {
		NCRoomManager rm = rooms.get(room);
		if (rm == null) {
			return;
		}
		rm.removeUser(u);
		if (!defaultRooms.contains(room)) {
			{
				if (rm.usersInRoom() == 0) {
					rooms.remove(room);
				} // If there are no users in the room, remove it
				else if (rm.adminsInRoom() == 0) {
					rm.addRandomAdmin();
				} // Select random admin
			}
		}
	}

	public boolean changeRoomName(String currentRoom, String roomName) {
		NCRoomManager rm = rooms.get(currentRoom);
		if(rm != null && isRoomNameAvailable(roomName)) {
			rm.setRoomName(roomName);
			rooms.put(roomName, rm);
			rooms.remove(currentRoom);
			return true;
		} 
		return false;
	}

}
