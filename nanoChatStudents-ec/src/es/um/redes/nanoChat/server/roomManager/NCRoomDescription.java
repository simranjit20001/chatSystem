package es.um.redes.nanoChat.server.roomManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class NCRoomDescription {
	//Campos de los que, al menos, se compone una descripción de una sala
	public String roomName;
	public List<String> members;
	public List<String> admins;
	public long timeLastMessage;

	private final static String MAIN_DELIMITER = "@";
	private final static String MEMBER_DELIMITER = ":";


	//Constructor a partir de los valores para los campos
	public NCRoomDescription(String roomName, Collection<String> members, Collection<String> admins,
			long timeLastMessage) {
		
		this.roomName = roomName;
		this.members = new ArrayList<String>(members);
		this.admins = new ArrayList<String>(admins);
		this.timeLastMessage = timeLastMessage;
	}

	public List<String> getMembers() {
		return Collections.unmodifiableList(members);
	}

	public List<String> getAdmins() {
		return Collections.unmodifiableList(admins);
	}

	public long getTimeLastMessage() {
		return timeLastMessage;
	}

	public String getRoomName() {
		return roomName;
	}

	//Constructor a partir de una cadena
	public static NCRoomDescription readFromString(String roomDescription) {
		String[] parts = roomDescription.split(MAIN_DELIMITER);
		String roomName = parts[0];
		List<String> members =  Arrays.asList(parts[1].split(MEMBER_DELIMITER));
		List<String> admins =Arrays.asList(parts[2].split(MEMBER_DELIMITER));
		long timeLastMessage = Long.parseLong(parts[3]);
		return new NCRoomDescription(roomName, members, admins, timeLastMessage);
	}
	
	public String toEncodedString() {
		StringBuilder sb = new StringBuilder();
		sb.append(roomName);
		sb.append(MAIN_DELIMITER);
		for (String member : members) {
			sb.append(member);
			sb.append(MEMBER_DELIMITER);
		}
		sb.append(MAIN_DELIMITER);
		for (String admin : admins) {
			sb.append(admin);
			sb.append(MEMBER_DELIMITER);
		}
		sb.append(MAIN_DELIMITER);
		sb.append(timeLastMessage);
		return sb.toString();
	}

	//Método que devuelve una representación de la Descripción lista para ser impresa por pantalla
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Room name: " + roomName);
		sb.append("\nMembers: ");
		for (String member : members) {
			sb.append(member);
			sb.append(" ");
		}

		sb.append("\nAdmins: ");

		for (String admin : admins) {
			
			sb.append(admin);
			sb.append(" ");
			
		}
		sb.append("\nTime last message: " + new Date(timeLastMessage).toString());

		return sb.toString();
	}
}