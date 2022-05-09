package es.um.redes.nanoChat.messageFV;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;


public abstract class NCMessage {
	protected byte opcode;

	//TDO Implementar el resto de los opcodes para los distintos mensajes
	public static final byte OP_ACESS_DENIED = 1;
	public static final byte OP_BAN_FROM_ROOM = 2;
	public static final byte OP_CHANGE_ROOM_NAME = 3;
	public static final byte OP_CHAT_MESSAGE = 4;
	public static final byte OP_CREATE_ROOM = 5;
	public static final byte OP_DELETE_ROOM_ADMIN = 6;
	public static final byte OP_ENTER_ROOM = 8;
	public static final byte OP_EXIT_ROOM = 9;
	public static final byte OP_GET_ROOMS = 10;
	public static final byte OP_HISTORY_REQUEST = 11;
	public static final byte OP_HISTORY_RESPONSE = 12;
	public static final byte OP_INVALID_ROOM = 13;
	public static final byte OP_KICK_USER = 14;
	public static final byte OP_MAKE_ROOM_ADMIN = 15;
	public static final byte OP_NICK = 16;
	public static final byte OP_NICK_DUPLICATED = 17;
	public static final byte OP_NICK_INVALID = 18;
	public static final byte OP_PERMISION_INVALID = 19;
	public static final byte OP_ROOM_DUPLICATED = 20;
	public static final byte OP_ROOM_INFO = 21;
	public static final byte OP_ROOM_LIST = 22;
	public static final byte OP_SEND_MESSAGE = 23;
	public static final byte OP_SYSTEM_MESSAGE = 24;
	public static final byte OP_GOT_BANNED = 25;
	public static final byte OP_ENTERED_USER = 26;
	public static final byte OP_LEFT_USER = 27;
	public static final byte OP_SEND_PRIVATE = 28;
	public static final byte OP_PRIVATE_CHAT_MESSAGE = 29;
	public static final byte OP_ROOM_INFO_RESPONSE = 30;
	public static final byte OP_OK = 100;
	public static final byte OP_INVALID_CODE = 101;

	//Constantes con los delimitadores de los mensajes de field:value
	public static final char DELIMITER = ':';    //Define el delimitador
	public static final char END_LINE = '\n';    //Define el carácter de fin de línea

	public static final String OPCODE_FIELD = "operation";

	/**
	 * Códigos de los opcodes válidos  El orden
	 * es importante para relacionarlos con la cadena
	 * que aparece en los mensajes
	 */

	 /**
	  * 
	  	OP_NICK_INVALID, OP_ENTER_ROOM, OP_ROOM_ENTER_OK, 
		OP_INVALID_ROOM, OP_ACESS_DENIED, OP_MAKE_ROOM, OP_MAKE_ROOM_OK,  OP_MAKE_ROOM_ADMIN, 
		OP_DELETE_ROOM_ADMIN, OP_BAN_FROM_ROOM, OP_DELETE_ROOM_BAN, OP_CHANGE_ROOM_NAME,OP_ROOM_ENTER_OK, 
		OP_CHANGE_ROOM_NAME_OK, OP_ROOM_LIST, OP_ROOM_DUPLICATED, 
		OP_ROOM_INFO, OP_LEAVE_ROOM, OP_ROOM_LIST, OP_PERMISION_INVALID, OP_CONFIG_ROOM_OK
	  */
	  
	private static final Byte[] _valid_opcodes = {
		OP_ACESS_DENIED,
		OP_BAN_FROM_ROOM,
		OP_CHANGE_ROOM_NAME,
		OP_CHAT_MESSAGE,
		OP_CREATE_ROOM,
		OP_DELETE_ROOM_ADMIN,
		OP_ENTER_ROOM,
		OP_EXIT_ROOM,
		OP_GET_ROOMS,
		OP_HISTORY_REQUEST,
		OP_HISTORY_RESPONSE,
		OP_INVALID_ROOM,
		OP_KICK_USER,
		OP_MAKE_ROOM_ADMIN,
		OP_NICK,
		OP_NICK_DUPLICATED,
		OP_NICK_INVALID,
		OP_PERMISION_INVALID,
		OP_ROOM_DUPLICATED,
		OP_ROOM_INFO,
		OP_ROOM_LIST,
		OP_SEND_MESSAGE,
		OP_SYSTEM_MESSAGE,
		OP_GOT_BANNED,
		OP_ENTERED_USER,
		OP_LEFT_USER, 
		OP_SEND_PRIVATE,
		OP_PRIVATE_CHAT_MESSAGE,
		OP_ROOM_INFO_RESPONSE,
		OP_OK,
		OP_INVALID_CODE
	
	};

	/**
	 * cadena exacta de cada orden
	 */
	private static final String[] _valid_operations_str = {
		"OP_ACESS_DENIED",
		"OP_BAN_FROM_ROOM",
		"OP_CHANGE_ROOM_NAME",
		"OP_CHAT_MESSAGE",
		"OP_CREATE_ROOM",
		"OP_DELETE_ROOM_ADMIN",
		"OP_ENTER_ROOM",
		"OP_EXIT_ROOM",
		"OP_GET_ROOMS",
		"OP_HISTORY_REQUEST",
		"OP_HISTORY_RESPONSE",
		"OP_INVALID_ROOM",
		"OP_KICK_USER",
		"OP_MAKE_ROOM_ADMIN",
		"OP_NICK",
		"OP_NICK_DUPLICATED",
		"OP_NICK_INVALID",
		"OP_PERMISION_INVALID",
		"OP_ROOM_DUPLICATED",
		"OP_ROOM_INFO",
		"OP_ROOM_LIST",
		"OP_SEND_MESSAGE",
		"OP_SYSTEM_MESSAGE",
		"OP_GOT_BANNED",
		"OP_ENTERED_USER",
		"OP_EXITED_USER",
		"OP_SEND_PRIVATE",
		"OP_PRIVATE_CHAT_MESSAGE",
		"OP_ROOM_INFO_RESPONSE",
		"OP_OK",
		"OP_INVALID_CODE"
	};







	private static Map<String, Byte> _operation_to_opcode;
	private static Map<Byte, String> _opcode_to_operation;
	
	static {
		_operation_to_opcode = new TreeMap<>();
		_opcode_to_operation = new TreeMap<>();
		for (int i = 0 ; i < _valid_operations_str.length; ++i)
		{
			_operation_to_opcode.put(_valid_operations_str[i].toLowerCase(), _valid_opcodes[i]);
			_opcode_to_operation.put(_valid_opcodes[i], _valid_operations_str[i]);
		}
	}
	
	/**
	 * Transforma una cadena en el opcode correspondiente
	 */
	protected static byte operationToOpcode(String opStr) {
		return _operation_to_opcode.getOrDefault(opStr.toLowerCase(), OP_INVALID_CODE);
	}

	/**
	 * Transforma un opcode en la cadena correspondiente
	 */
	protected static String opcodeToOperation(byte opcode) {
		return _opcode_to_operation.getOrDefault(opcode, null);
	}

	//Devuelve el opcode del mensaje
	public byte getOpcode() {
		return opcode;
	}

	//Método que debe ser implementado específicamente por cada subclase de NCMessage
	protected abstract StringBuffer toBufferedString();

	public final String toEncodedString() {
		StringBuffer sb = new StringBuffer();		
		sb.append(OPCODE_FIELD+DELIMITER+opcodeToOperation(opcode)+END_LINE); 
		sb.append(toBufferedString());		
		sb.append(END_LINE);  //Marcamos el final del mensaje
		return sb.toString(); //Se obtiene el mensaje
	}

	//Extrae la operación del mensaje entrante y usa la subclase para parsear el resto del mensaje
	public static NCMessage readMessageFromSocket(DataInputStream dis) throws IOException {
		String message = dis.readUTF();
		String[] lines = message.split(String.valueOf(END_LINE));
		if (!lines[0].isEmpty()) { // Si la línea no está vacía
			int idx = lines[0].indexOf(DELIMITER); // Posición del delimitador
			String field = lines[0].substring(0, idx).toLowerCase(); 																		// minúsculas
			String value = lines[0].substring(idx + 1).trim();
			if (!field.equalsIgnoreCase(OPCODE_FIELD))
				return null;
			byte code = operationToOpcode(value);
			if (code == OP_INVALID_CODE)
				return null;
			switch (code) {

			case OP_NICK:
			case OP_ENTER_ROOM:
			case OP_DELETE_ROOM_ADMIN:
			case OP_CHANGE_ROOM_NAME:
			case OP_SEND_MESSAGE:
			case OP_CREATE_ROOM:
			case OP_KICK_USER:
			case OP_SYSTEM_MESSAGE:
			case OP_ENTERED_USER:
			case OP_LEFT_USER:
			case OP_MAKE_ROOM_ADMIN:
			case OP_ROOM_INFO_RESPONSE:
			
			{
				return NCRoomMessage.readFromString(code, message);
			}

			case OP_NICK_DUPLICATED:
			case OP_GET_ROOMS:
			case OP_PERMISION_INVALID:
			case OP_ROOM_DUPLICATED:	
			case OP_EXIT_ROOM:		
			case OP_HISTORY_REQUEST:
			case OP_GOT_BANNED:
			case OP_OK:
			case OP_ACESS_DENIED:	
			case OP_NICK_INVALID:	
			case OP_ROOM_INFO:
			{
				return NCInternalMessage.readFromString(code);
			}
			case OP_ROOM_LIST:
			case OP_HISTORY_RESPONSE:
			{
				return NCInternalListMessage.readFromString(code, message);
			}
		
			case OP_CHAT_MESSAGE:
			case OP_SEND_PRIVATE:
			case OP_PRIVATE_CHAT_MESSAGE:
			{
				return NCChatMessage.readFromString(code, message);
			}
			

			default:
				System.err.println("Unknown message type received:" + code);
				return null;
			}
		} else
			return null;
	}

	//Método para construir un mensaje de tipo Room a partir del opcode y del nombre
	public static NCMessage makeRoomMessage(byte code, String name) {
		return new NCRoomMessage(code, name);
	}

	public static NCMessage makeInternalListMessage(byte code, Collection<String> list) {
		return new NCInternalListMessage(code, list);
	}

    public static NCMessage makeInternalMessage(byte code) {
        return new NCInternalMessage(code);
    }

	public static NCMessage makeChatMessage(byte code, String nick, String message) {
		return new NCChatMessage(code, nick, message);
	}

}
