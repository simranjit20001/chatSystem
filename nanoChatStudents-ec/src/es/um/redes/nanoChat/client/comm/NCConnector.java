package es.um.redes.nanoChat.client.comm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

import es.um.redes.nanoChat.messageFV.NCChatMessage;
import es.um.redes.nanoChat.messageFV.NCInternalListMessage;
import es.um.redes.nanoChat.messageFV.NCInternalMessage;
import es.um.redes.nanoChat.messageFV.NCMessage;
import es.um.redes.nanoChat.messageFV.NCRoomMessage;
import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;

//Esta clase proporciona la funcionalidad necesaria para intercambiar mensajes entre el cliente y el servidor de NanoChat
public class NCConnector {
	private Socket socket;
	protected DataOutputStream dos;
	protected DataInputStream dis;
	
	public NCConnector(InetSocketAddress serverAddress) throws UnknownHostException, IOException {
		this.socket = new Socket(serverAddress.getAddress(), serverAddress.getPort());
		dos = new DataOutputStream(socket.getOutputStream());
		dis = new DataInputStream(socket.getInputStream());
	}

	public boolean registerNickname(String nick) throws IOException {
		NCRoomMessage message = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_NICK, nick);
		String rawMessage = message.toEncodedString();
		dos.writeUTF(rawMessage);
		NCInternalMessage response = (NCInternalMessage) NCMessage.readMessageFromSocket(dis);
		switch (response.getOpcode()) {
			case NCMessage.OP_OK:
				return true;
			case NCMessage.OP_NICK_DUPLICATED:
				return false;
			default:
				throw new IllegalStateException("Unexpected opcode: " + response.getOpcode());
		}	
	}
	
	//Método para obtener la lista de salas del servidor
	public List<NCRoomDescription> getRooms() throws IOException {
		//Funcionamiento resumido: SND(GET_ROOMS) and RCV(ROOM_LIST)
		NCInternalMessage message = (NCInternalMessage) NCMessage.makeInternalMessage(NCMessage.OP_GET_ROOMS);
		dos.writeUTF(message.toEncodedString());
		NCInternalListMessage response = (NCInternalListMessage) NCMessage.readMessageFromSocket(dis);

		if(response.getOpcode() != NCMessage.OP_ROOM_LIST) {
			throw new IllegalStateException("Unexpected opcode: " + response.getOpcode());
		}
		
		List<NCRoomDescription> rooms = new LinkedList<NCRoomDescription>();
		for(String encodedNcRoom : response.getList()){
			NCRoomDescription room = NCRoomDescription.readFromString(encodedNcRoom);
			rooms.add(room);
		}
		
		return rooms;
	}
	
	//Método para solicitar la entrada en una sala
	public boolean enterRoom(String room) throws IOException {
		//Funcionamiento resumido: SND(ENTER_ROOM<room>) and RCV(IN_ROOM) or RCV(REJECT)
		NCRoomMessage message = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_ENTER_ROOM, room);
		dos.writeUTF(message.toEncodedString());
		NCInternalMessage response = (NCInternalMessage) NCMessage.readMessageFromSocket(dis);
		
		switch(response.getOpcode()) {
			case NCMessage.OP_OK:
				return true;
			case NCMessage.OP_INVALID_ROOM: 
				System.out.println("The room " + room + " is not valid");
				return false;
			case NCMessage.OP_ACESS_DENIED:
				System.out.println("The room " + room + " is not open");
				return false;
			default:
				throw new IllegalStateException("Unexpected opcode: " + response.getOpcode());
		}

	}
	
	public void leaveRoom(String room) throws IOException {
		NCInternalMessage message = (NCInternalMessage) NCMessage.makeInternalMessage(NCMessage.OP_EXIT_ROOM);
		dos.writeUTF(message.toEncodedString());
	}
	
	//Método que utiliza el Shell para ver si hay datos en el flujo de entrada
	public boolean isDataAvailable() throws IOException {
		return (dis.available() != 0);
	}
	
	
	//Método para pedir la descripción de una sala
	public NCRoomDescription getCurrentRoomInfo() throws IOException {
		NCInternalMessage message = (NCInternalMessage) NCMessage.makeInternalMessage(NCMessage.OP_ROOM_INFO);
		String rawMessage = message.toEncodedString();
		dos.writeUTF(rawMessage);
		NCRoomMessage response = (NCRoomMessage) NCMessage.readMessageFromSocket(dis);
		switch (response.getOpcode()) {
			case NCMessage.OP_ROOM_INFO_RESPONSE:
				String roomInfo = response.getValue();
				NCRoomDescription roomDescription = NCRoomDescription.readFromString(roomInfo);
				return roomDescription;
			default:
				throw new IllegalStateException("Unexpected opcode: " + response.getOpcode());
		}

	}


	public void sendMessage(String chatMessage)  throws IOException {
		//Funcionamiento resumido: SND(CHAT_MESSAGE<chatMessage>)
		//Construimos el mensaje de chat
		//Enviamos el mensaje por el flujo de salida
		NCRoomMessage message = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_SEND_MESSAGE, chatMessage);
		dos.writeUTF(message.toEncodedString());
	}

	
	//Método para cerrar la comunicación con la sala
	//(Opcional) Enviar un mensaje de salida del servidor de Chat
	public void disconnect() {
		try {
			if (socket != null) {
				socket.close();
			}
		} catch (IOException e) {
		} finally {
			socket = null;
		}
	}



	public String getIncomingMessage() throws IOException {
		String text = null;; 
		NCMessage message = NCMessage.readMessageFromSocket(dis);
		NCRoomMessage roomMessage = null;
		NCChatMessage chatMessage = null;

		switch(message.getOpcode()) {
			case NCMessage.OP_CHAT_MESSAGE:
				chatMessage = (NCChatMessage) message;
				text = chatMessage.toPrintableString();
				break;
			case NCMessage.OP_SYSTEM_MESSAGE:
				roomMessage = (NCRoomMessage) message;
				text = "System: " + roomMessage.getValue();
				break;
			case NCMessage.OP_GOT_BANNED:
				text = "You got banned";
				break;
			case NCMessage.OP_ENTERED_USER:
				roomMessage = (NCRoomMessage) message;
				text = "User " + roomMessage.getValue() + " entered the room"; 
				break;
			case NCMessage.OP_LEFT_USER:
				roomMessage = (NCRoomMessage) message;
				text = "User " + roomMessage.getValue() + " left the room";
				break;
			case NCMessage.OP_PRIVATE_CHAT_MESSAGE:
				chatMessage = (NCChatMessage) message;
				text = "Private message: " + chatMessage.toPrintableString();
				break;
			default:
				throw new IllegalStateException("Unexpected opcode: " + message.getOpcode());
		}
			
		return text;
		
	}


    public boolean removeAdmin(String nickname) throws IOException {
		NCRoomMessage message = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_DELETE_ROOM_ADMIN, nickname);
		String rawMessage = message.toEncodedString();
		dos.writeUTF(rawMessage);
		NCInternalMessage response = (NCInternalMessage) NCMessage.readMessageFromSocket(dis);
		switch (response.getOpcode()) {
			case NCMessage.OP_OK:
				return true;
			case NCMessage.OP_NICK_INVALID:
				System.out.println("The user " + nickname + " is not in the room");
				return false;
			case NCMessage.OP_PERMISION_INVALID:
				System.out.println("Either you are not an admin or the user " + nickname + " is not admin");
				return false;
			default:
				throw new IllegalStateException("Unexpected opcode: " + response.getOpcode());
		}
    }


    public boolean makeAdmin(String nickname) throws IOException {
		NCRoomMessage message = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_MAKE_ROOM_ADMIN, nickname);
		String rawMessage = message.toEncodedString();
		dos.writeUTF(rawMessage);
		NCInternalMessage response = (NCInternalMessage) NCMessage.readMessageFromSocket(dis);
		switch (response.getOpcode()) {
			case NCMessage.OP_OK:
				return true;
			case NCMessage.OP_NICK_INVALID:
				System.out.println("The user " + nickname + " is not in the room");
				return false;
			case NCMessage.OP_PERMISION_INVALID:
				System.out.println("Either you are not an admin or the user " + nickname + " is already an admin");
				return false;
			default:
				throw new IllegalStateException("Unexpected opcode: " + response.getOpcode());
		}
	}


	public boolean changeRoomName(String room) throws IOException {
		NCRoomMessage message = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_CHANGE_ROOM_NAME, room);
		String rawMessage = message.toEncodedString();
		dos.writeUTF(rawMessage);
		NCInternalMessage response = (NCInternalMessage) NCMessage.readMessageFromSocket(dis);
		
		switch (response.getOpcode()) {
			case NCMessage.OP_OK:
				System.out.println("The room name has been changed");
				return true;
			case NCMessage.OP_ROOM_DUPLICATED:
				System.out.println("The room " + room + " already exists");
				return false;
			case NCMessage.OP_PERMISION_INVALID:
				System.out.println("You are not an admin");
				return false;
			default: 
				throw new IllegalStateException("Unexpected opcode: " + response.getOpcode());
		}	

	}


	public boolean makeRoom(String room) throws IOException {
		NCRoomMessage message = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_CREATE_ROOM, room);
		String rawMessage = message.toEncodedString();
		dos.writeUTF(rawMessage);
		NCInternalMessage response = (NCInternalMessage) NCMessage.readMessageFromSocket(dis);
		switch (response.getOpcode()) {
			case NCMessage.OP_OK:
				return true;
			case NCMessage.OP_ROOM_DUPLICATED:
				System.out.println("The room " + room + " already exists");
				return false;
			default: 
				throw new IllegalStateException("Unexpected opcode: " + response.getOpcode());
		}	
	}


	public String getHistory() throws IOException {
		NCMessage message = (NCInternalMessage) NCMessage.makeInternalMessage(NCMessage.OP_HISTORY_REQUEST);
		String rawMessage = message.toEncodedString();
		dos.writeUTF(rawMessage);
		NCInternalListMessage response = (NCInternalListMessage) NCMessage.readMessageFromSocket(dis);

		if(response.getOpcode() != NCMessage.OP_HISTORY_RESPONSE) 
			throw new IllegalStateException("Expected OP_HISTORY_RESPONSE, but got " + response.getOpcode());
		
		List<String> history = (List<String>) response.getList();
		StringBuilder sb = new StringBuilder();
		
		for(String s : history) {
			sb.append(s);
			sb.append("\n");
		}

		return sb.toString();
		

	}


	public boolean kickUser(String nickname) throws IOException {
		NCRoomMessage message = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_KICK_USER, nickname);
		String rawMessage = message.toEncodedString();
		dos.writeUTF(rawMessage);
		NCInternalMessage response = (NCInternalMessage) NCMessage.readMessageFromSocket(dis);
		switch (response.getOpcode()) {
			case NCMessage.OP_OK:
				System.out.println("The user " + nickname + " has been kicked");
				return true;
			case NCMessage.OP_NICK_INVALID:
				System.out.println("The user " + nickname + " is not in the room");
				return false;
			case NCMessage.OP_PERMISION_INVALID:
				System.out.println("Either you are not an admin or the user " + nickname + " is not admin");
				return false;
			default:
				throw new IllegalStateException("Unexpected opcode: " + response.getOpcode());
		}
	}

	public boolean sendPrivateMessage(String nickname, String chatMessage) throws IOException {
		NCChatMessage message = (NCChatMessage) NCMessage.makeChatMessage(NCMessage.OP_SEND_PRIVATE, nickname, chatMessage);
		String rawMessage = message.toEncodedString();
		dos.writeUTF(rawMessage);
		NCInternalMessage response = (NCInternalMessage) NCMessage.readMessageFromSocket(dis);
		switch (response.getOpcode()) {
			case NCMessage.OP_OK:
				System.out.println("The message has been sent");
				return true;
			case NCMessage.OP_NICK_INVALID:
				System.out.println("The user " + nickname + " is not in the room");
				return false;			
			default:
				throw new IllegalStateException("Unexpected opcode: " + response.getOpcode());
		}		
	}


}
