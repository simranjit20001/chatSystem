package es.um.redes.nanoChat.messageFV;
public class NCInternalMessage extends NCMessage {

    public NCInternalMessage(byte type){

        this.opcode = type;

    }
	
	protected StringBuffer toBufferedString(){
		return new StringBuffer();
	}

	//Parseamos el mensaje contenido en message con el fin de obtener los distintos campos
	public static NCInternalMessage readFromString(byte code) {
		return new NCInternalMessage(code);
	}

    
}
