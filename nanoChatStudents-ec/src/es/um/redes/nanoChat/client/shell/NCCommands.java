package es.um.redes.nanoChat.client.shell;

import java.util.Map;
import java.util.TreeMap;

public class NCCommands {

	/**
	 * Códigos para todos los comandos soportados por el shell
	 */

	public static final byte COM_INVALID = 0;
	public static final byte COM_ROOMLIST = 1;
	public static final byte COM_ENTER = 2;
	public static final byte COM_NICK = 3;
	public static final byte COM_SEND = 4;
	public static final byte COM_EXIT = 5;
	public static final byte COM_ROOMINFO = 7;
	public static final byte COM_QUIT = 8;
	public static final byte COM_HELP = 9;
	public static final byte COM_CHANGE_ROOM_NAME = 10;
	public static final byte COM_MAKE_ADMIN = 11;
	public static final byte COM_REMOVE_ADMIN = 12;
	public static final byte COM_CREATE_ROOM = 13;
	public static final byte COM_HISTORY = 14;
	public static final byte COM_KICK = 15;
	public static final byte COM_SEND_PRIVATE = 16;
	public static final byte COM_SOCKET_IN = 101;
	
	
	/**
	 * Códigos de los comandos válidos que puede
	 * introducir el usuario del shell. El orden
	 * es importante para relacionarlos con la cadena
	 * que debe introducir el usuario y con la ayuda
	 */
	protected static final Byte[] _valid_user_commands = { 
		COM_ROOMLIST, 
		COM_ENTER,
		COM_NICK,
		COM_SEND,
		COM_EXIT, 
		COM_ROOMINFO,
		COM_QUIT,
		COM_HELP,
		COM_CHANGE_ROOM_NAME,
		COM_MAKE_ADMIN,
		COM_REMOVE_ADMIN,
		COM_CREATE_ROOM,
		COM_HISTORY,
		COM_KICK,
		COM_SEND_PRIVATE
		};

	/**
	 * cadena exacta de cada orden
	 */
	protected static final String[] _valid_user_commands_str = {
		"roomlist",
		"enter",
		"nick",
		"send",
		"exit",
		"info",
		"quit",
		"help",
		"change_room_name",
		"makeAdmin",
		"removeAdmin",
		"create",
		"history",
		"kick",
		"send_private"
	};

	/**
	 * Mensaje de ayuda para cada orden
	 */
	private static final String[] _valid_user_commands_help = {
		"provides a list of available rooms to chat",
		"enter a particular <room>",
		"to set the <nickname> in the server",
		"to send a <message> in the chat",
		"to leave the current room", 
		"shows the information of the room",
		"to quit the application",
		"shows this information",
		"changes the name of the room",
		"makes the user an admin of the room",
		"removes the user from the admins of the room",
		"creates a new room",
		"shows the history of the room",
		"kicks the user from the room",
		"sends a private message to the user"
	};

	private static Map<String, Byte> _commands_map;
	static {
		_commands_map = new TreeMap<>();
		for (int i = 0; i < _valid_user_commands_str.length; i++)
			_commands_map.put(_valid_user_commands_str[i].toLowerCase(),
					_valid_user_commands[i]);
	}
	
	/**
	 * Transforma una cadena introducida en el código de comando correspondiente
	 */
	public static byte stringToCommand(String comStr) {
		//Busca entre los comandos si es válido y devuelve su código. Si no, COM_INVALID
		return _commands_map.getOrDefault(comStr.toLowerCase(), COM_INVALID);
	}

	/**
	 * Imprime la lista de comandos y la ayuda de cada uno
	 */
	public static void printCommandsHelp() {
		System.out.println("List of commands:");
		for (int i = 0; i < _valid_user_commands_str.length; i++) {
			System.out.println(_valid_user_commands_str[i] + " -- "
					+ _valid_user_commands_help[i]);
		}		
	}
}	

