package es.um.redes.nanoChat.messageFV;



/*
 * ROOM
----

operation:<operation>
name:<name>

Defined operations:
Nick
*/

public class NCRoomMessage extends NCMessage {

	private String value;
	
	//Campo específico de este tipo de mensaje
	static protected final String NAME_FIELD = "value";

	/**
	 * Creamos un mensaje de tipo Room a partir del código de operación y del nombre
	 */
	public NCRoomMessage(byte type, String value) {
		this.opcode = type;
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	//Pasamos los campos del mensaje a la codificación correcta en field:value
	@Override
	protected StringBuffer toBufferedString() {
		StringBuffer sb = new StringBuffer();			
		sb.append(NAME_FIELD+DELIMITER+value+END_LINE); //Construimos el campo
		return sb;
	}
	

	//Parseamos el mensaje contenido en message con el fin de obtener los distintos campos
	public static NCRoomMessage readFromString(byte code, String message) {
		String[] lines = message.split(String.valueOf(END_LINE));
		String name = null;
		int idx = lines[1].indexOf(DELIMITER); // Posición del delimitador
		String field = lines[1].substring(0, idx).toLowerCase();                                                                                                                                                // minúsculas
		String value = lines[1].substring(idx + 1).trim();
		if (field.equalsIgnoreCase(NAME_FIELD))
			name = value;

		return new NCRoomMessage(code, name);
	}
	
}
