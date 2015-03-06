package common;

import java.io.*;
import java.net.*;

public class SocketClient {
	static final int portNumber = 2005;
	Socket requestSocket;
	ObjectOutputStream out;
	ObjectInputStream in;
	String message;
	String address;

	public SocketClient(String address) {
		this.address = address;
	}

	boolean testConnection(String address) {
		try {
			createConnection(address);
			do {
				try {
					message = (String) in.readObject();
					System.out.println("server>" + message);
					sendMessage("Hi my server");
					message = "bye";
					sendMessage(message);
				} catch (ClassNotFoundException classNot) {
					System.err.println("data received in unknown format");
				}
			} while (!message.equals("bye"));

			//
			if (requestSocket != null && in != null && out != null) {
				this.address = address;
				return true;
			}

		} catch (UnknownHostException unknownHost) {
			System.err.println(address + ":"
					+ "You are trying to connect to an unknown host!");
		} catch (IOException ioException) {
			System.err.println(address + ":" + ioException.getMessage());
		} finally {
			closeConnection();
		}
		return false;
	}

	public void sendMsg(Message msg) {
		try {
			createConnection(this.address);
			do {
				try {
					message = (String) in.readObject();
					// send a message object
					out.writeObject(msg);
					out.flush();
					//
					Message response = (Message)in.readObject();
					System.out.println(response.getDetail());
					// send bye
					message = "bye";
					sendMessage(message);
				} catch (ClassNotFoundException classNot) {
					System.err.println("data received in unknown format");
				}
			} while (!message.equals("bye"));

		} catch (UnknownHostException unknownHost) {
			System.err.println(address + ":"
					+ "You are trying to connect to an unknown host!");
		} catch (IOException ioException) {
			System.err.println(address + ":" + ioException.getMessage());
		} finally {
			closeConnection();
		}
	}

	public void sendMsg(String msg) {
		try {
			createConnection(this.address);
			do {
				try {
					message = (String) in.readObject();
					sendMessage(msg);

					message = "bye";
					sendMessage(message);
				} catch (ClassNotFoundException classNot) {
					System.err.println("data received in unknown format");
				}
			} while (!message.equals("bye"));

		} catch (UnknownHostException unknownHost) {
			System.err.println(address + ":"
					+ "You are trying to connect to an unknown host!");
		} catch (IOException ioException) {
			System.err.println(address + ":" + ioException.getMessage());
		} finally {
			closeConnection();
		}
	}

	public void createConnection(String address) throws UnknownHostException,
			IOException {
		// 1. creating a socket to connect to the server
		requestSocket = new Socket(address, portNumber);
		// 2. get Input and Output streams
		out = new ObjectOutputStream(requestSocket.getOutputStream());
		out.flush();
		in = new ObjectInputStream(requestSocket.getInputStream());
	}

	private void closeConnection() {
		try {
			if (in != null)
				in.close();
			if (out != null)
				out.close();
			if (requestSocket != null)
				requestSocket.close();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}

	private void sendMessage(String msg) {
		try {
			out.writeObject(msg);
			out.flush();
			// System.out.println("client>" + msg);
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}

	public static void main(String[] args) throws IOException {
		SocketClient client = new SocketClient("127.0.0.1");
		client.testConnection("localhost");
		client.sendMessage("Hello");
	}
}
