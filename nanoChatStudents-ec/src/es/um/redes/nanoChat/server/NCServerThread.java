package es.um.redes.nanoChat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.stream.Collectors;

import es.um.redes.nanoChat.messageFV.NCChatMessage;
import es.um.redes.nanoChat.messageFV.NCInternalListMessage;
import es.um.redes.nanoChat.messageFV.NCInternalMessage;
import es.um.redes.nanoChat.messageFV.NCMessage;
import es.um.redes.nanoChat.messageFV.NCRoomMessage;
import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;
import es.um.redes.nanoChat.server.roomManager.NCRoomManager;

/**
 * A new thread runs for each connected client
 */
public class NCServerThread extends Thread {

	private Socket socket = null;
	// Manager global compartido entre los Threads
	private NCServerManager serverManager = null;
	// Input and Output Streams
	private DataInputStream dis;
	private DataOutputStream dos;
	// Usuario actual al que atiende este Thread
	User user;
	// RoomManager actual (dependerá de la sala a la que entre el usuario)
	NCRoomManager roomManager;

	// Inicialización de la sala
	public NCServerThread(NCServerManager manager, Socket socket) throws IOException {
		super("NCServerThread");
		this.socket = socket;
		this.serverManager = manager;
	}

	// Main loop
	public void run() {
		try {
			// Se obtienen los streams a partir del Socket
			dis = new DataInputStream(socket.getInputStream());
			dos = new DataOutputStream(socket.getOutputStream());
			// En primer lugar hay que recibir y verificar el nick
			receiveAndVerifyNickname();
			// Mientras que la conexión esté activa entonces...
			while (true) {

				NCMessage message = NCMessage.readMessageFromSocket(dis);

				switch (message.getOpcode()) {
					case NCMessage.OP_GET_ROOMS:
						sendRoomList();
						break;
					case NCMessage.OP_CREATE_ROOM:
						makeRoom((NCRoomMessage) message);
						break;

					case NCMessage.OP_ENTER_ROOM:
						String room = ((NCRoomMessage)  message).getValue();
						if (enterRoom(room))
							processRoomMessages();
						break;				
				
				}
			}
		} catch (Exception e) {
			// If an error occurs with the communications the user is removed from all the
			// managers and the connection is closed
			if(user != null){
				System.out.println("* User " + user.getName() + " disconnected.");
				if(roomManager != null ) serverManager.leaveRoom(user, roomManager.getRoomName());
				serverManager.removeUser(user);
			}else 
				System.out.println("* Unknown user disconnected.");	//Usuario no registrado
		} finally {
			if (!socket.isClosed())
				try {
					socket.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
		}
	}


	//Procces of general commands
	private void receiveAndVerifyNickname() throws IOException {

		boolean validNick = false;

		while (!validNick) {

			NCRoomMessage message = (NCRoomMessage) NCMessage.readMessageFromSocket(dis);
			String nick = message.getValue();
			if (serverManager.addUser(nick, socket.getInetAddress())) {
				user = serverManager.getUser(nick);
				NCInternalMessage response = (NCInternalMessage) NCMessage.makeInternalMessage(NCMessage.OP_OK);
				String rawResponse = response.toEncodedString();
				dos.writeUTF(rawResponse);
				validNick = true;
			} else {
				NCInternalMessage response = (NCInternalMessage) NCMessage
						.makeInternalMessage(NCMessage.OP_NICK_DUPLICATED);
				String rawResponse = response.toEncodedString();
				dos.writeUTF(rawResponse);
			}
		}

		// if the executions end and nick is not valid we send a message to the client
		// indicating that
		// someting went wrong

		if (!validNick) {// someting went wrong
			NCInternalMessage response = (NCInternalMessage) NCMessage
					.makeInternalMessage(NCMessage.OP_NICK_INVALID);
			String rawResponse = response.toEncodedString();
			dos.writeUTF(rawResponse);
		}

	}

	private void makeRoom(NCRoomMessage message) {
		byte opcode;
		String roomName = message.getValue();
		boolean sucess = false;

		if (!serverManager.registerRoomManager(roomName)) {
			opcode = NCMessage.OP_ROOM_DUPLICATED;
			System.out.println("Room " + roomName + " already exists");
		} else {
			opcode = NCMessage.OP_OK;
			System.out.println("* Room " + roomName + " created." + "by " + user.getName());
			sucess = true;
		}

		NCInternalMessage response = (NCInternalMessage) NCMessage.makeInternalMessage(opcode);
		String rawMessage = response.toEncodedString();
		try {
			dos.writeUTF(rawMessage);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (sucess) {
			roomManager = serverManager.enterRoom(user, roomName, socket);
			roomManager.addAdmin(user);

			try {
				processRoomMessages();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}	
		
		
	}	

	private boolean enterRoom(String room) {
		

		NCRoomManager rm = serverManager.enterRoom(user, room, socket);

		if(rm == null) {
			System.out.println("User " + user.getName() + " not accepted in the room " + room);
			NCInternalMessage msg = new NCInternalMessage(NCMessage.OP_ACESS_DENIED);
			String rawResponse = msg.toEncodedString();
			try {
				dos.writeUTF(rawResponse);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return false;
		}
		
		System.out.println("User " + user.getName() + " joined room " + room);
		NCInternalMessage msg = new NCInternalMessage(NCMessage.OP_OK);
		String rawResponse = msg.toEncodedString();
		roomManager = rm;

		try {
			dos.writeUTF(rawResponse);
		} catch (IOException e) {
			e.printStackTrace();
		}
	

		return true;
	}
	

	
	private void sendRoomList() {
		List<String> rooms = serverManager.getRoomList().stream()
				.map(NCRoomDescription::toEncodedString).collect(Collectors.toList());
		NCInternalListMessage message = (NCInternalListMessage) NCMessage
				.makeInternalListMessage(NCMessage.OP_ROOM_LIST, rooms);
		String rawMessage = message.toEncodedString();
		try {
			dos.writeUTF(rawMessage);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void processRoomMessages() throws IOException {
		
		boolean exit = false;
		boolean sucess = false;
		System.out.println("Procesing room roomcommands");
		NCChatMessage chatMessage = null;
		NCRoomMessage roomMessage = null;

		String rawResponse = "";

		String entryWarning = user.getName();
		roomMessage = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_ENTERED_USER, entryWarning);
		roomManager.broadcastSystemMessage(roomMessage);

		while (!exit) {

			NCMessage message = NCMessage.readMessageFromSocket(dis);
			byte opcode = message.getOpcode();

			switch (opcode) {

				case NCMessage.OP_ROOM_INFO:
					String roomDescription = roomManager.getDescription().toEncodedString();
					roomMessage = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_ROOM_INFO_RESPONSE, roomDescription);
					rawResponse = roomMessage.toEncodedString();
					dos.writeUTF(rawResponse);
					break;

				case NCMessage.OP_SEND_MESSAGE:
					roomMessage = (NCRoomMessage) message;
					chatMessage = (NCChatMessage) NCMessage.makeChatMessage(NCMessage.OP_CHAT_MESSAGE, user.getName(), roomMessage.getValue());
					roomManager.broadcastMessage(user, 	chatMessage);
					break;
				
				case NCMessage.OP_MAKE_ROOM_ADMIN:
					sucess = false;
					if(roomManager.isAdmin(user)) {
						roomMessage = (NCRoomMessage) message;
						sucess = roomManager.addAdmin(serverManager.getUser(roomMessage.getValue()));
					}

					if(sucess) {
						NCInternalMessage response = (NCInternalMessage) NCMessage.makeInternalMessage(NCMessage.OP_OK);
						rawResponse = response.toEncodedString();
						dos.writeUTF(rawResponse);
					} else {
						NCInternalMessage response = (NCInternalMessage) NCMessage.makeInternalMessage(NCMessage.OP_PERMISION_INVALID);
						rawResponse = response.toEncodedString();
						dos.writeUTF(rawResponse);
					}
					break;
				
				case NCMessage.OP_DELETE_ROOM_ADMIN:
					sucess = false;
					if(roomManager.isAdmin(user)) {
						roomMessage = (NCRoomMessage) message;
						sucess = roomManager.removeAdmin(serverManager.getUser(roomMessage.getValue()));
					}

					if(sucess) {
						NCInternalMessage response = (NCInternalMessage) NCMessage.makeInternalMessage(NCMessage.OP_OK);
						rawResponse = response.toEncodedString();
						dos.writeUTF(rawResponse);
					} else {
						NCInternalMessage response = (NCInternalMessage) NCMessage.makeInternalMessage(NCMessage.OP_PERMISION_INVALID);
						rawResponse = response.toEncodedString();
						dos.writeUTF(rawResponse);
					}
					break;

				case NCMessage.OP_EXIT_ROOM: {
					serverManager.leaveRoom(user, roomManager.getRoomName());
					roomMessage = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_LEFT_USER, user.getName());
					roomManager.broadcastSystemMessage(roomMessage);	
					roomManager = null;
					exit = true;
					break;
				}

				case NCMessage.OP_HISTORY_REQUEST:
					List<String> historyRequest = roomManager.getHistory(user);
					NCInternalListMessage listMessage = (NCInternalListMessage) NCMessage.makeInternalListMessage(NCMessage.OP_HISTORY_RESPONSE, 
					historyRequest);
					rawResponse = listMessage.toEncodedString();
					dos.writeUTF(rawResponse);
					break;

				case NCMessage.OP_KICK_USER:
					NCInternalMessage response = null;
					roomMessage = (NCRoomMessage) message;
					User toBanUser = serverManager.getUser(roomMessage.getValue());
				

					if(!roomManager.isAdmin(user) || roomManager.isAdmin(toBanUser) ) {
						response = (NCInternalMessage) NCMessage.makeInternalMessage(NCMessage.OP_PERMISION_INVALID);
						rawResponse = response.toEncodedString();
						dos.writeUTF(rawResponse);
					} 
					else if (roomManager.isUser(toBanUser)) { 
						NCInternalMessage internalMessage = (NCInternalMessage) NCMessage.makeInternalMessage(NCMessage.OP_GOT_BANNED);
						rawResponse = internalMessage.toEncodedString();
						roomManager.sendPrivateMessage(toBanUser, rawResponse);

						roomManager.blockUser(toBanUser);
						response = (NCInternalMessage) NCMessage.makeInternalMessage(NCMessage.OP_OK);		
						rawResponse = response.toEncodedString();
						dos.writeUTF(rawResponse);

						String exitWarning = toBanUser.getName() + " ha sido baneado del chat por el admin " + user.getName();
						roomMessage = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_SYSTEM_MESSAGE, exitWarning);
						roomManager.broadcastSystemMessage(roomMessage);	

					} else{
						response = (NCInternalMessage) NCMessage.makeInternalMessage(NCMessage.OP_NICK_INVALID);
						rawResponse = response.toEncodedString();
						dos.writeUTF(rawResponse);
					}


					break;
				
				case NCMessage.OP_CHANGE_ROOM_NAME:
					roomMessage = (NCRoomMessage) message;
					String roomName = roomMessage.getValue();
					sucess = false;

					if (roomManager.isAdmin(user)) {
						sucess = serverManager.changeRoomName(roomManager.getRoomName(), roomName);
						if (sucess) {
							response = (NCInternalMessage) NCMessage.makeInternalMessage(NCMessage.OP_OK);
							rawResponse = response.toEncodedString();
							dos.writeUTF(rawResponse);
							NCRoomMessage advertisingMessage = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_SYSTEM_MESSAGE ,"RoomName is now " + roomName + " changed by " + user.getName());
							rawResponse = advertisingMessage.toEncodedString();
							roomManager.broadcastSystemMessage(advertisingMessage);
						} else {
							response = (NCInternalMessage) NCMessage.makeInternalMessage(NCMessage.OP_ROOM_DUPLICATED);
							rawResponse = response.toEncodedString();
							dos.writeUTF(rawResponse);
						}
					} else {
						response = (NCInternalMessage) NCMessage.makeInternalMessage(NCMessage.OP_PERMISION_INVALID);
						rawResponse = response.toEncodedString();
						dos.writeUTF(rawResponse);
					}
					break;
		
				case NCMessage.OP_SEND_PRIVATE:
					chatMessage = (NCChatMessage) message;
					String privateMessage = chatMessage.getMessage();
					User toUser = serverManager.getUser(chatMessage.getValue());
					if (toUser != null && roomManager.isUser(toUser)) {
						response = (NCInternalMessage) NCMessage.makeInternalMessage(NCMessage.OP_OK);
						rawResponse = response.toEncodedString();
						dos.writeUTF(rawResponse);
						chatMessage = (NCChatMessage) NCMessage.makeChatMessage(NCMessage.OP_PRIVATE_CHAT_MESSAGE, user.getName(), privateMessage);
						rawResponse = chatMessage.toEncodedString();
						roomManager.sendPrivateMessage(toUser, rawResponse); //The order matters: if we send the private message before the confirmation, in the case that a user is trying to autosend a private msg the client will crash						
					} else {
						response = (NCInternalMessage) NCMessage.makeInternalMessage(NCMessage.OP_NICK_INVALID);
						rawResponse = response.toEncodedString();
						dos.writeUTF(rawResponse);
					}
					break;

				default:
					throw new IllegalArgumentException("Unexpected value: " + opcode);
				}
		}
	}
}
