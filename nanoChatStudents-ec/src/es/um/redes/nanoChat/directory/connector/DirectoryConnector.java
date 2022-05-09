package es.um.redes.nanoChat.directory.connector;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import es.um.redes.nanoChat.directory.Opcodes;

/**
 * Cliente con métodos de consulta y actualización específicos del directorio
 */
public class DirectoryConnector {
	
	private static final int PACKET_MAX_SIZE = 128;
	private static final int DEFAULT_PORT = 6868;
	private static final int TIMEOUT = 1000;
	private static final int MAX_REENVIOS = 4;

	private DatagramSocket socket; // socket UDP
	private InetSocketAddress directoryAddress; // dirección del servidor de directorio

	public DirectoryConnector(String agentAddress) throws IOException {
		directoryAddress = new InetSocketAddress(InetAddress.getByName(agentAddress), DEFAULT_PORT);
		socket = new DatagramSocket(); 
	} //Add a comunication socket

	/**
	 * Envía una solicitud para obtener el servidor de chat asociado a un determinado protocolo
	 * 
	 */
	public InetSocketAddress getServerForProtocol(int protocol) throws IOException {
		//Prepare the message to be send
		byte[] req = buildQuery(protocol);
		DatagramPacket packet = new DatagramPacket(req, req.length, directoryAddress);
		socket.send(packet);
		byte[] response = new byte [PACKET_MAX_SIZE];
		DatagramPacket reciv  = new DatagramPacket(response, response.length);
		socket.setSoTimeout(TIMEOUT);
		//Try to receive the response 
		
		boolean recived = false;
		for(int i = 0; i < MAX_REENVIOS; i++) {
			try {
				socket.receive(reciv);
				recived = true;
				break;
			} catch (IOException e) {
				socket.send(packet);
				System.err.println("No hay respuesta del directorio... Reintentando "+(i+1));
			}
		} //We will try to receive the response MAX_REENVIOS times if it fails
		
		if(!recived)
		{
			System.err.println("No se ha obtenido respuesta del servidor");
			return null;
		} //If we don't receive a response, we will return null
		
		
		
		return getAddressFromResponse(reciv); 
	}


	//Método para generar el mensaje de consulta (para obtener el servidor asociado a un protocolo)
	private byte[] buildQuery(int protocol) {
		final int SIZE = 5; //Int => 4 bytes Opcode => 1 byte
		ByteBuffer bb = ByteBuffer.allocate(SIZE);
		bb.put(Opcodes.QUERY);
		bb.putInt(protocol);
		return bb.array();
	}

	//Método para obtener la dirección de internet a partir del mensaje UDP de respuesta

	private InetSocketAddress getAddressFromResponse(DatagramPacket packet) throws UnknownHostException {
		InetSocketAddress dir = null;
		ByteBuffer bb = ByteBuffer.wrap(packet.getData());

		int opcode = bb.get();
		switch (opcode) {
		case Opcodes.EMPTY: {
				System.err.println("No server for protocol");
			break;
		}
		case Opcodes.SERVER_INFO: {
			int port = bb.getInt();
			byte[] buf = new byte[4];
			bb.get(buf, 0, buf.length);
			String ip = Integer.toString(buf[0]) + "." +Integer.toString(buf[1]) + "."+Integer.toString(buf[2]) + "." +Integer.toString(buf[3]);
			
			dir = new InetSocketAddress(ip, port);
			break;
		}
		default:
			throw new IllegalArgumentException("Unexpected value: " + opcode);
		}
		
		return dir;
	}
	
	/**
	 * Envía una solicitud para registrar el servidor de chat asociado a un determinado protocolo
	 * 
	 */
	public boolean registerServerForProtocol(int protocol, int port) throws IOException {

		
		byte[] req = buildRegistration(protocol, port);
		DatagramPacket packet = new DatagramPacket(req, req.length, directoryAddress);
		socket.send(packet);
		byte[] response = new byte [PACKET_MAX_SIZE];
		DatagramPacket reciv  = new DatagramPacket(response, response.length);
		socket.setSoTimeout(TIMEOUT);
		
		
		for(int i = 0; i < MAX_REENVIOS; i++) {
			try {
				socket.receive(reciv);
				ByteBuffer ret = ByteBuffer.wrap(reciv.getData());
				byte opcode = ret.get();
				return opcode == Opcodes.REGISTER_OK;
			} catch (IOException e) {
				socket.send(packet);
				System.err.println("No response to register server. Attemp: "+(i+1));
			}
		}
		
		return false;
	}


	private byte[] buildRegistration(int protocol, int port) {
		final int SIZE = 9; //2 Ints => 4 bytes Opcode => 1 byte
		ByteBuffer bb = ByteBuffer.allocate(SIZE);  //two int -> 8 bytes. Opcode -> 1 byte.
		bb.put(Opcodes.REGISTER);
		bb.putInt(protocol);
		bb.putInt(port);
		return bb.array();
	}

	public void close() {
		socket.close();
	}
}

