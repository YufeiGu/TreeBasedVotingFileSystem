package client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import common.Message;
import common.MsgCommand;

public class SocketClient {
	String address;
	int portNumber;
	Socket requestSocket;
	ObjectOutputStream out;
	ObjectInputStream in;
	String message;

	Client client;

	public SocketClient(String address, Client client, int portNumber) {
		this.address = address;
		this.client = client;
		this.portNumber = portNumber;
	}

	public void sendMsg(Message msg) {
		try {
			createConnection();
			do {
				try {
					message = (String) in.readObject();
					if (message == null)
						message = "";
					if(message.equals("bye")) break;
					
					// send a message object
					out.writeObject(msg);
					out.flush();

					//commit,withdraw
					if (msg.getCmd() == MsgCommand.COMMIT) {
						Message response = (Message) in.readObject();
					} else if (msg.getCmd() == MsgCommand.WITHDRAW) {
						Message response = (Message) in.readObject();
					}

					// if message is not "bye", send bye
					if (!message.equals("bye"))
						sendMessage("bye");
				} catch (ClassNotFoundException classNot) {
					System.err.println("data received in unknown format");
				}
			} while (true);

		} catch (UnknownHostException unknownHost) {
			System.err.println(address + ":"
					+ "You are trying to connect to an unknown host!");
		} catch (IOException ioException) {
			System.err.println(address + ":" + ioException.getMessage());
			ioException.printStackTrace();
		} finally {
			closeConnection();
		}
	}

	public void sendMsg(String msg) {
		try {
			createConnection();
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

	public void createConnection() throws UnknownHostException, IOException {
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
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}

	public static void main(String[] args) throws IOException {
		// SocketClient client = new SocketClient("127.0.0.1");
		// client.testConnection("localhost");
		// client.sendMessage("Hello");
	}
}
