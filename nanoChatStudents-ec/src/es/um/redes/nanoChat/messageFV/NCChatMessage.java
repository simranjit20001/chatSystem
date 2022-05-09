package es.um.redes.nanoChat.messageFV;

public class NCChatMessage extends NCRoomMessage {

    private String message;
    protected static final String MESSAGE_FIELD = "message";

    public NCChatMessage(byte type, String nick, String message) {

        super(type, nick);
        this.message = message;
    }
  
    public String getMessage() {
        return message;
    }

    public String toPrintableString() {
        return getValue() + ": " + getMessage();
    }

    @Override
    protected StringBuffer toBufferedString() {
        StringBuffer sb = super.toBufferedString();
        sb.append(MESSAGE_FIELD+DELIMITER+message+END_LINE);
        return sb;
    }

    public static NCChatMessage readFromString(byte code, String message) {

        String[] lines = message.split(String.valueOf(END_LINE));
        String nick = null;
		int idx = lines[1].indexOf(DELIMITER); // Posición del delimitador
		String field = lines[1].substring(0, idx).toLowerCase();                                                                                                                                                // minúsculas
		String value = lines[1].substring(idx + 1).trim();
		if (field.equalsIgnoreCase(NAME_FIELD))
            nick = value;

        String msg = null;
        idx = lines[2].indexOf(DELIMITER); // Posición del delimitador
        field = lines[2].substring(0, idx).toLowerCase(); // minúsculas
        value = lines[2].substring(idx + 1).trim();
        if (field.equalsIgnoreCase(MESSAGE_FIELD))
            msg = value;

        return new NCChatMessage(code, nick, msg);
        
    }

    
}
