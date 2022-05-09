package es.um.redes.nanoChat.client.application;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

import es.um.redes.nanoChat.client.comm.NCConnector;
import es.um.redes.nanoChat.client.shell.NCCommands;
import es.um.redes.nanoChat.client.shell.NCShell;
import es.um.redes.nanoChat.directory.connector.DirectoryConnector;
import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;


public class NCController {
	//Diferentes estados del cliente de acuerdo con el autómata
	private static final byte PRE_CONNECTION = 1;
	private static final byte PRE_REGISTRATION = 2;
	//Código de protocolo implementado por este cliente
	private static final byte REGISTERED = 3;
	private static final byte IN_ROOM = 4;
	//ODO Cambiar para cada grupo
	private static final int PROTOCOL = 127;
	//Conector para enviar y recibir mensajes del directorio
	private DirectoryConnector directoryConnector;
	//Conector para enviar y recibir mensajes con el servidor de NanoChat
	private NCConnector ncConnector;
	//Shell para leer comandos de usuario de la entrada estándar
	private NCShell shell;
	//Último comando proporcionado por el usuario
	private byte currentCommand;
	//Nick del usuario
	private String nickname;
	//Sala de chat en la que se encuentra el usuario (si está en alguna)
	private String room;
	//Mensaje enviado o por enviar al chat
	private String chatMessage;
	//Other parameter
	private String otherParameter;
	//Dirección de internet del servidor de NanoChat
	private InetSocketAddress serverAddress;
	//Estado actual del cliente, de acuerdo con el autómata
	private byte clientStatus = PRE_CONNECTION;


	//Constructor
	public NCController() {
		shell = new NCShell();
	}

	//Devuelve el comando actual introducido por el usuario
	public byte getCurrentCommand() {		
		return this.currentCommand;
	}

	//Establece el comando actual
	public void setCurrentCommand(byte command) {
		currentCommand = command;
	}

	//Registra en atributos internos los posibles parámetros del comando tecleado por el usuario
	public void setCurrentCommandArguments(String[] args) {
		//Comprobaremos también si el comando es válido para el estado actual del autómata
		switch (currentCommand) {
		case NCCommands.COM_NICK:
			if (clientStatus == PRE_REGISTRATION)
				nickname = args[0];
			break;
		case NCCommands.COM_ENTER:
		case NCCommands.COM_CHANGE_ROOM_NAME:
		case NCCommands.COM_CREATE_ROOM:
			room = args[0];
			break;
		case NCCommands.COM_SEND:
			chatMessage = args[0];
			break;
		case NCCommands.COM_SEND_PRIVATE:
			nickname = args[0];
			chatMessage = args[1];
			break;
		case NCCommands.COM_MAKE_ADMIN:
		case NCCommands.COM_REMOVE_ADMIN:
		case NCCommands.COM_KICK:
			otherParameter = args[0];
			break;
		default:
			break;
		}
	}

	//Procesa los comandos introducidos por un usuario que aún no está dentro de una sala
	public void processCommand() {
		switch (currentCommand) {
		case NCCommands.COM_NICK:
			if (clientStatus == PRE_REGISTRATION)
				registerNickName();
			else
				System.out.println("* You have already registered a nickname ("+nickname+")");
			break;
		case NCCommands.COM_ROOMLIST:
			if(clientStatus == REGISTERED)
				getAndShowRooms();
			else
				System.out.println("* You must register a nickname in first place. Please use nick (desired nick)");
			break;
		case NCCommands.COM_ENTER:

			if(clientStatus == REGISTERED){
				enterChat();
			}
			else
				System.out.println("* You must register a nickname in first place. Please use nick (desired nick)"); 

			break;
		
		case NCCommands.COM_CREATE_ROOM:
			if(clientStatus == REGISTERED){
				makeRoom();
			} else{
				System.out.println("* You must register a nickname in first place. Please use nick (desired nick)");
			}
			break;

		case NCCommands.COM_QUIT:
			//Cuando salimos tenemos que cerrar todas las conexiones y sockets abiertos
			ncConnector.disconnect();			
			directoryConnector.close();
			break;
		default:
		}
	}
	

	private void makeRoom() {
		boolean result = false;

		try {
			result = ncConnector.makeRoom(room);
		} catch (IOException e) {
			// TOD Auto-generated catch block
			e.printStackTrace();
		}

		if (!result) {
			System.out.println("* Room already exists");
			return;
		}

		System.out.println("* Your are now in: " + room);
		clientStatus = IN_ROOM;

		do {
			readRoomCommandFromShell();
			processRoomCommand();
		} while (currentCommand != NCCommands.COM_EXIT && clientStatus == IN_ROOM);
		System.out.println("* Your are out of the room");

		clientStatus = REGISTERED;
	}
	

	private void registerNickName() {
		try {
			boolean registered = ncConnector.registerNickname(nickname);
			if (registered) {
				System.out.println("* Your nickname is now " + nickname);
				clientStatus = REGISTERED;
			}
			else
				//En este caso el nick ya existía
				System.out.println("* The nickname is already registered. Try a different one.");			
		} catch (IOException e) {
			System.err.println("* There was an error registering the nickname");
			e.printStackTrace();
		}
	}

	private void getAndShowRooms() {
		List<NCRoomDescription> rooms = null;
		try {
			rooms = ncConnector.getRooms();

		} catch (IOException e) {
			e.printStackTrace();
			return ;
		}

		System.out.println("\n*******************\n");
		System.out.println("* Rooms available:");
		for(NCRoomDescription room : rooms){
			System.out.println("-------------");
			System.out.println(room.toString());
		}
	}

	//Método para tramitar la solicitud de acceso del usuario a una sala concreta
	private void enterChat() {
		boolean result = false;
		try {
			result = ncConnector.enterRoom(room);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(!result) {
			System.out.println("* The room does not exist");
			return;
		}
			
		System.out.println("* Your are now in: "+ room);
		clientStatus = IN_ROOM;
		do {
			//Pasamos a aceptar sólo los comandos que son válidos dentro de una sala
			readRoomCommandFromShell();
			processRoomCommand();
		} while (currentCommand != NCCommands.COM_EXIT && clientStatus == IN_ROOM);
		System.out.println("* Your are out of the room");
		clientStatus = REGISTERED;
	}

	//Método para procesar los comandos específicos de una sala
	private void processRoomCommand() {
		switch (currentCommand) {
		case NCCommands.COM_ROOMINFO:
			//El usuario ha solicitado información sobre la sala y llamamos al método que la obtendrá
			getAndShowInfo();
			break;
		case NCCommands.COM_SEND:
			//El usuario quiere enviar un mensaje al chat de la sala
			sendChatMessage();
			break;
		case NCCommands.COM_SEND_PRIVATE:
			sendPrivateMessage();
			break;
		case NCCommands.COM_SOCKET_IN:
			//En este caso lo que ha sucedido es que hemos recibido un mensaje desde la sala y hay que procesarlo
			processIncommingMessage();
			break;
		
		case NCCommands.COM_MAKE_ADMIN:
			processMakeAdminRequest();
			break;

		case NCCommands.COM_REMOVE_ADMIN:
			processRemoveAdminRequest();
			break;
		
		case NCCommands.COM_KICK:
			processKickRequest();
			break;

		case NCCommands.COM_CHANGE_ROOM_NAME:
			
			processChangeRoomNameRequest();
			break;
		
		case NCCommands.COM_HISTORY:
			getAndShowHistory();
			break;
		
		case NCCommands.COM_EXIT:
			exitTheRoom();
			break;
		}		
	}

	private void sendPrivateMessage() {

	
		try {
			ncConnector.sendPrivateMessage(nickname, chatMessage);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("There was an error sending the message");;
		}


		
	}

	private void processKickRequest() {
		try{
			ncConnector.kickUser(otherParameter);
		}
		catch(IOException e){
			System.err.println("* There was an error kicking the user");
			e.printStackTrace();
		}
	}

	private void getAndShowHistory() {
		String history = null;
		try {
			history = ncConnector.getHistory();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("The history of messages for this room and this minute is:" );
		System.out.println("\n****************************************\n");
		System.out.println(history);
		System.out.println("\n****************************************\n");
	}

	private void processChangeRoomNameRequest() {
		boolean sucess = false;
		try {
			sucess = ncConnector.changeRoomName(room);

		} catch (IOException e) {
			e.printStackTrace();
		}
		if(!sucess) {
			System.out.println("* The room name has not been changed");
		}
	}

	private void processRemoveAdminRequest() {
		boolean result = false;
		try {
			result = ncConnector.removeAdmin(otherParameter);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if(result){
			System.out.println("* Petiton fulfilled");
		} else{
			System.out.println("*You are not an admin");
		}
	}

	private void processMakeAdminRequest() {
		boolean result = false;
		try {
			result = ncConnector.makeAdmin(otherParameter);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if(result){
			System.out.println("* Petiton fulfilled");
		} else{
			System.out.println("* You are not an admin");
		}
	}

	private void getAndShowInfo() {
		try {
			NCRoomDescription description = ncConnector.getCurrentRoomInfo();
			System.out.println("\n****************************************\n");
			System.out.println(description.toString());
			System.out.println("\n****************************************\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//Método para notificar al servidor que salimos de la sala
	private void exitTheRoom() {
		
		try {
			ncConnector.leaveRoom(room);
			clientStatus = REGISTERED;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	private void sendChatMessage() {
		try {
			ncConnector.sendMessage(chatMessage);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//Método para procesar los mensajes recibidos del servidor mientras que el shell estaba esperando un comando de usuario
	private void processIncommingMessage() {		
			
		String text = "";
		try {
			text = ncConnector.getIncomingMessage();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println(text);

		if(text.equals("You got banned")){
			clientStatus = REGISTERED;
			try {
				ncConnector.leaveRoom(room);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	
	}

	//MNétodo para leer un comando de la sala 
	public void readRoomCommandFromShell() {
		//Pedimos un nuevo comando de sala al shell (pasando el conector por si nos llega un mensaje entrante)
		shell.readChatCommand(ncConnector);
		//Establecemos el comando tecleado (o el mensaje recibido) como comando actual
		setCurrentCommand(shell.getCommand());
		//Procesamos los posibles parámetros (si los hubiera)
		setCurrentCommandArguments(shell.getCommandArguments());
	}

	//Método para leer un comando general (fuera de una sala)
	public void readGeneralCommandFromShell() {
		//Pedimos el comando al shell
		shell.readGeneralCommand();
		//Establecemos que el comando actual es el que ha obtenido el shell
		setCurrentCommand(shell.getCommand());
		//Analizamos los posibles parámetros asociados al comando
		setCurrentCommandArguments(shell.getCommandArguments());
	}

	//Método para obtener el servidor de NanoChat que nos proporcione el directorio
	public boolean getServerFromDirectory(String directoryHostname) {
		//Inicializamos el conector con el directorio y el shell
		System.out.println("* Connecting to the directory...");
		//Intentamos obtener la dirección del servidor de NanoChat que trabaja con nuestro protocolo
		try {
			directoryConnector = new DirectoryConnector(directoryHostname);
			serverAddress = directoryConnector.getServerForProtocol(PROTOCOL);
		} catch (IOException e1) {
			serverAddress = null;
		}
		//Si no hemos recibido la dirección entonces nos quedan menos intentos
		if (serverAddress == null) {
			System.out.println("* Check your connection, the directory is not available.");		
			return false;
		}
		else return true;
	}
	
	//Método para establecer la conexión con el servidor de Chat (a través del NCConnector)
	public boolean connectToChatServer() {
			try {
				//Inicializamos el conector para intercambiar mensajes con el servidor de NanoChat (lo hace la clase NCConnector)
				ncConnector = new NCConnector(serverAddress);
			} catch (IOException e) {
				System.err.println("* Check your connection, the game server is not available.");
				serverAddress = null;
			}
			//Si la conexión se ha establecido con éxito informamos al usuario y cambiamos el estado del autómata
			if (serverAddress != null) {
				System.out.println("* Connected to "+serverAddress);
				clientStatus = PRE_REGISTRATION;
				return true;
			}
			else return false;
	}

	//Método que comprueba si el usuario ha introducido el comando para salir de la aplicación
	public boolean shouldQuit() {
		return currentCommand == NCCommands.COM_QUIT;
	}

}
