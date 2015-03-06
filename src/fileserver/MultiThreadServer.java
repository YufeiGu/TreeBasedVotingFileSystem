package fileserver;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import common.GlobalConstant;
import common.Message;


public class MultiThreadServer extends Thread {
	private int port = GlobalConstant.fileserverPortNumber;
	private ServerSocket serverSocket;
	private ExecutorService executorService;// Thread pool
	private final int POOL_SIZE = 10;//size of Thread Pool
	FileServer node = null;

	public MultiThreadServer(FileServer obj) throws IOException {
		this.node = obj;
		serverSocket = new ServerSocket(port);
		executorService = Executors.newFixedThreadPool(Runtime.getRuntime()
				.availableProcessors() * POOL_SIZE);
	}

	//entry
	public void run() {
		while (true) {
			Socket socket = null;
			try {
				socket = serverSocket.accept();
				executorService.execute(new Handler(socket,this.node));

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) throws IOException {
	}
}

/**
 * socket handle
 * @author GuYufei
 *
 */
class Handler implements Runnable {
	private Socket socket;
	ObjectOutputStream out;
	ObjectInputStream in;
	FileServer node = null;
	boolean stop = false;

	public Handler(Socket socket,FileServer node) {
		this.socket = socket;
		this.node = node;
	}

	/**
	 * send msg to connecting client
	 * @param msg
	 */
	void sendMessage(String msg) {
		try {
			out.writeObject(msg);
			out.flush();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}
	
	/**
	 * receive data from client or file server and handle them
	 * @param clientName
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private String receiveData(String clientName) throws IOException,
			ClassNotFoundException {
		Object obj = in.readObject();
		if (obj == null){
			this.node.logger.writeLine("Server: the object recieved is null");
			return "";
		}

		Message msg = null;

		if (obj instanceof Message) {
			msg = (Message) obj;
			
			this.node.handleMsg(msg,clientName,out);
		}

		String strMsg = "";
		if (obj instanceof String) {
			strMsg = (String) obj;

			if (strMsg.equals("bye"))
				sendMessage("bye");
		}
		return strMsg;
	}

	//entry
	public void run() {
		try {
			// get Input and Output streams
			out = new ObjectOutputStream(socket.getOutputStream());
			out.flush();
			in = new ObjectInputStream(socket.getInputStream());
			sendMessage("Connection successful");
			//The two parts communicate via the input and output streams
			String message = null;
			do {
				try {
					message = receiveData(socket.getInetAddress().getHostName());
				} catch (ClassNotFoundException classnot) {
					System.err.println("Data received in unknown format");
				}
			} while (!message.equals("bye"));
		} catch (IOException ioException) {
			ioException.printStackTrace();
		} finally {
			// Closing connection
			try {
				in.close();
				out.close();
				socket.close();
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}
		}
	}
}