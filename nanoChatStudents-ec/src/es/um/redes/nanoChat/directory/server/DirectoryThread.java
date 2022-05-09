package es.um.redes.nanoChat.directory.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import es.um.redes.nanoChat.directory.Opcodes;


public class DirectoryThread extends Thread {
	
	private static final int PACKET_MAX_SIZE = 128;
	protected Map<Integer,InetSocketAddress> servers;
	protected DatagramSocket socket = null;
	protected double messageDiscardProbability; //For simulate packet loss in the network




	public DirectoryThread(String name, int directoryPort,
			double corruptionProbability)
			throws SocketException {
		super(name);
		InetSocketAddress serverAddress = new InetSocketAddress(directoryPort);
		socket = new DatagramSocket(serverAddress);
		messageDiscardProbability = corruptionProbability;
		servers = new HashMap<Integer, InetSocketAddress>();
	}

	@Override
	public void run() {
		byte[] buf = new byte[PACKET_MAX_SIZE];

		System.out.println("[Directory] Starting...");
		boolean running = true;
		while (running) {

			
				DatagramPacket pckt = new DatagramPacket(buf, buf.length);

				try {
					socket.receive(pckt);
				} catch (IOException e) {
					e.printStackTrace();
				}

				
				InetSocketAddress clientAddress = (InetSocketAddress) pckt.getSocketAddress();

				double rand = Math.random();
				if (rand < messageDiscardProbability) {
					System.err.println("Directory DISCARDED corrupt request from... ");
					continue;
				}  //Check if the message should be discarded only for simulate packet loss in the network

	
			
				try {
					processRequestFromClient(pckt.getData(), clientAddress);
				} catch (IOException e) {
					e.printStackTrace();
					System.err.println("[Directory] Error processing request from client");
					//We dont end the communication if there is an error because we want to keep the server running
				}
				
		}
		socket.close();
	}

	
	public void processRequestFromClient(byte[] data, InetSocketAddress clientAddr) throws IOException { 
		// TOD 1) Extraemos el tipo de mensaje recibido
		ByteBuffer ret = ByteBuffer.wrap(data);
		byte opcode = ret.get();
		int protocol = ret.getInt();
		
		System.out.println("[Directory] New Request From "+clientAddr.getAddress()+"/"+ clientAddr.getPort());

		switch (opcode) {
			case Opcodes.REGISTER:
				System.out.println("	Solicitud registro servidor para protocolo:"+protocol);
				int port = ret.getInt();
				InetSocketAddress serverToRegister = new InetSocketAddress(clientAddr.getAddress(), port);
				servers.put(protocol, serverToRegister);
				sendOK(clientAddr);
				break;
			case Opcodes.QUERY:
				System.out.println("	Consulta servidor para protocolo: "+protocol);
				InetSocketAddress solicitedAddr = servers.get(protocol);

				if (solicitedAddr != null) {
					sendServerInfo(solicitedAddr, clientAddr);
				} else {
					sendEmpty(clientAddr);
				}

				break;
			
			default:
				throw new IllegalStateException("Unknown opcode: " + opcode);
		}
	



	}

	//Método para enviar una respuesta vacía (no hay servidor)
	private void sendEmpty(InetSocketAddress clientAddr) throws IOException {
		final int SIZE = 1; //Opcode  = 1 byte
		//TOD Construir respuesta
		ByteBuffer bb = ByteBuffer.allocate(SIZE);
		bb.put(Opcodes.EMPTY);
		byte[] resp = bb.array();
		//TOD Enviar respuesta
		DatagramPacket pckt = new DatagramPacket(resp, resp.length, clientAddr);
		socket.send(pckt);
	}

	//Método para enviar la dirección del servidor al cliente
	private void sendServerInfo(InetSocketAddress serverAddress, InetSocketAddress clientAddr) throws IOException {

		int port = serverAddress.getPort();
		byte[] dir = serverAddress.getAddress().getAddress();
		final int SIZE = 9; //2 Ints => 4 bytes Opcode => 1 byte
		ByteBuffer bb = ByteBuffer.allocate(SIZE);
		bb.put(Opcodes.SERVER_INFO);
		bb.putInt(port);
		bb.put(dir);									//Devuelve direccion ip en un array de  4 enteros
		
		byte[] buf = bb.array();
		DatagramPacket resp = new DatagramPacket(buf, buf.length, clientAddr);

		socket.send(resp);
	}


	private void sendOK(InetSocketAddress clientAddr) throws IOException {
		
		final int SIZE = 1; //Opcode  = 1 byte
		ByteBuffer bb = ByteBuffer.allocate(SIZE);
		bb.put(Opcodes.REGISTER_OK);
		byte[] resp = bb.array();
		//TOD Enviar respuesta
		DatagramPacket pckt = new DatagramPacket(resp, resp.length, clientAddr);
		socket.send(pckt);
		System.out.println("	Servidor registrado");
	}
	
}