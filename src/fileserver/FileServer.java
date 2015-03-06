package fileserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

import common.GlobalConstant;
import common.Message;
import common.MsgCommand;
import common.SaveInfo;
import common.UtilityFileSystem;

/**
 * @author GuYufei
 * 
 */
public class FileServer {
	// log tool
	SaveInfo logger = new SaveInfo();
	// data D0,D1,D2,D3 and their queue
	int[] D = { 0, 0, 0, 0 };
	ArrayList<Queue<Message>> queues = new ArrayList<Queue<Message>>();
	// Messages have been committed or withdrawed.
	HashSet<String> handledMsgs = new HashSet<String>();
	// sequence of updates
	ArrayList<String> updateSeqs = new ArrayList<String>();
	// The total number of messages exchange
	Integer totalMsgNumber = 0;

	/**
	 * the entrance
	 */
	private void run() {
		// initialize queues
		for (int i = 0; i < 4; i++) {
			this.queues.add(new LinkedList<Message>());
		}

		// open the log file
		logger.open(String.format("./FileServerLog-%s.txt",
				UtilityFileSystem.getHostname()));
		logger.writeLine("FileServer " + UtilityFileSystem.getHostname()
				+ " starting...");

		// waiting for request from client
		try {
			new MultiThreadServer(this).start();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// command input
		String CurLine = "";
		System.out.println("Enter a line of text (type 'q' to exit): ");
		InputStreamReader converter = new InputStreamReader(System.in);
		BufferedReader in = new BufferedReader(converter);
		while (true) {
			try {
				CurLine = in.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (!(CurLine.equals("q"))) {
				handleCmd(CurLine);
			} else {
				//print update sequences
				printUpdateSeqs();
				//print total msg number
				logger.writeLine(String.format("Total Message Number: %d",
						this.totalMsgNumber));
				logger.close();
				System.exit(0);
			}
		}
	}

	/**
	 * handle command from console input
	 * 
	 * @param cmd
	 */
	private void handleCmd(String cmd) {
		cmd = cmd.trim();
		if (cmd.equals(""))
			return;

		if (cmd.equals("lsq")) {
			printQueueState();
		} else if (cmd.equals("updateSeq")) {
			printUpdateSeqs();
		} else if (cmd.equals("msgNo")) {
			logger.writeLine(String.format("Total Message Number: %d",
					this.totalMsgNumber));
		} else {
			logger.writeLine("'" + cmd + "' is not recognized as an command.");
		}
	}

	/**
	 * print the update sequences
	 */
	private void printUpdateSeqs() {
		for (String value : updateSeqs) {
			logger.write(value+" ");
		}
		logger.writeLine("");
	}

	/**
	 * print every queue state: number of msg in queue
	 */
	private void printQueueState() {
		for (int i = 0; i < 4; i++) {
			logger.write(String.format("%d ", queues.get(i).size()));
		}
		logger.writeLine("");
	}

	/**
	 * entry
	 * 
	 * @param args
	 */
	public static void main(String args[]) {
		FileServer server = new FileServer();
		server.run();
	}

	/**
	 * handle message from client, called by MultiThreadServer
	 * 
	 * @param msg
	 * @param clientName
	 * @param out
	 */
	public void handleMsg(Message msg, String clientName, ObjectOutputStream out) {
		synchronized (this.totalMsgNumber) {
			totalMsgNumber++;
		}

		if (msg.getCmd() == MsgCommand.REQUEST) {
			handleRequest(msg);
		} else if (msg.getCmd() == MsgCommand.COMMIT) {
			handleCommit(msg);
			sendACK(out);
		} else if (msg.getCmd() == MsgCommand.WITHDRAW) {
			handleWithdraw(msg);
			sendACK(out);
		}
	}

	/**
	 * Send ACK message to client
	 * 
	 * @param out
	 */
	private void sendACK(ObjectOutputStream out) {
		synchronized (this.totalMsgNumber) {
			totalMsgNumber++;
		}

		try {
			Message responseMsg = initACKMsg();
			out.writeObject(responseMsg);
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * handle withdraw from clients
	 * 
	 * @param msg
	 * @return
	 */
	private void handleWithdraw(Message msg) {
		logger.writeLine("Withdraw" + msg.msgToString());

		if (msg.getDetail() != null && !msg.getDetail().trim().equals("")) {
			if (msg.getDetail().equals("D0"))
				withdraw(msg, 0);
			else if (msg.getDetail().equals("D1"))
				withdraw(msg, 1);
			else if (msg.getDetail().equals("D2"))
				withdraw(msg, 2);
			else if (msg.getDetail().equals("D3"))
				withdraw(msg, 3);
		}
	}

	/**
	 * handle commit from client
	 * 
	 * @param msg
	 * @return
	 */
	private void handleCommit(Message msg) {
		logger.writeLine("Commit" + msg.msgToString());

		if (msg.getDetail() != null && !msg.getDetail().trim().equals("")) {
			if (msg.getDetail().equals("D0"))
				commit(msg, 0);
			else if (msg.getDetail().equals("D1"))
				commit(msg, 1);
			else if (msg.getDetail().equals("D2"))
				commit(msg, 2);
			else if (msg.getDetail().equals("D3"))
				commit(msg, 3);
		}
	}

	/**
	 * add value to D[index],and pop the message from queue, and if some
	 * messages exist on the top of queue, then grant it.
	 * 
	 * @param msg
	 * @param index
	 */
	private void commit(Message msg, int index) {
		// logger.writeLine("commit out syn");
		synchronized (this.D) {
			// logger.writeLine("commit in syn");
			this.D[index] = this.D[index] + msg.getV();
			// add to update sequence
			this.updateSeqs.add(String.format("D%d:%d", index,this.D[index]));

			logger.writeLine(String.format("D%d:%d", index,this.D[index]));
		}
		handleQueue(msg, index);
	}

	/**
	 * Pop the message from queue, and if some messages exist on the top of
	 * queue, then grant it.
	 * 
	 * @param msg
	 * @param index
	 */
	private void withdraw(Message msg, int index) {
		handleQueue(msg, index);
	}

	/**
	 * add msg to handled hashset Pop the message from queue, and if some
	 * messages exist on the top of queue, then grant it.
	 * 
	 * @param index
	 */
	private void handleQueue(Message msg, int index) {
		// add msg to handled hashset
		synchronized (this.handledMsgs) {
			this.handledMsgs.add(msg.getId()
					+ Integer.toString(msg.getSerialNo()));
		}

		Queue<Message> queue = this.queues.get(index);
		Message topMsg = null;
		synchronized (queue) {
			// if it's on the top, then grant next top
			if (queue.size() > 0 && queue.peek().msgEquals(msg)) {
				logger.write("pop msg ");
				// pop msg
				queue.poll();
				printQueueState();
				// send grant to the top message of queue,if exists
				topMsg = queue.peek();
			} else {
				// delete the msg from queue
				Message msgToDelete = null;
				for (Message msgtmp : queue) {
					if (msgtmp.msgEquals(msg))
						msgToDelete = msgtmp;
				}
				if (msgToDelete != null) {
					logger.write("remove msg ");
					queue.remove(msgToDelete);
					printQueueState();
				}
			}
		}

		// out of synchronized to avoid deadlock
		if (topMsg != null) {
			sendGrantMsg(topMsg);
		}
	}

	/**
	 * handle request from client, if the server has not grant any request for
	 * that data object, then send grant to client,then add the client to queue
	 * and block, else add client to queue and block too.
	 * 
	 * @param msg
	 * @return
	 */
	private void handleRequest(Message msg) {
		logger.writeLine("Request" + msg.msgToString());

		// if msg has been handled(commit or withdraw),return
		if (this.handledMsgs.contains(msg.getId()
				+ Integer.toString(msg.getSerialNo())))
			return;

		if (msg.getDetail() != null && !msg.getDetail().trim().equals("")) {

			if (msg.getDetail().equals("D0"))
				request(0, msg);
			else if (msg.getDetail().equals("D1"))
				request(1, msg);
			else if (msg.getDetail().equals("D2"))
				request(2, msg);
			else if (msg.getDetail().equals("D3"))
				request(3, msg);
		}
	}

	/**
	 * if the server has not grant any request for that data object, then send
	 * grant to client,else send wait to client. then add the client to queue.
	 * 
	 * @param index
	 * @param client
	 * @param responseMsg
	 */
	private void request(int index, Message msg) {
		Queue<Message> queue = this.queues.get(index);

		if (queue.isEmpty()) {
			// lock the queue first,then send the grant, in case of multiple
			// lock
			addMsgToQueue(msg, queue);

			sendGrantMsg(msg);
		} else {
			addMsgToQueue(msg, queue);
		}
	}

	/**
	 * if this msg hasn't been commited or withdrawed, add msg to queue, then
	 * lock the queue
	 * 
	 * @param msg
	 * @param queue
	 */
	private void addMsgToQueue(Message msg, Queue<Message> queue) {
		if (!this.handledMsgs.contains(msg.getId()
				+ Integer.toString(msg.getSerialNo()))) {
			synchronized (queue) {
				queue.add(msg);

				logger.write("add msg ");
				printQueueState();
			}
		}
	}

	/**
	 * send GRANT message to client
	 * 
	 * @param serverAddr
	 * @param msg
	 */
	void sendGrantMsg(Message msg) {
		Message grantMsg = new Message();
		grantMsg.setId(UtilityFileSystem.getHostname());
		grantMsg.setCmd(MsgCommand.GRANT);
		grantMsg.setSerialNo(msg.getSerialNo());
		grantMsg.setDetail(msg.getDetail());
		grantMsg.setV(msg.getV());

		String clientAddr = msg.getId();

		logger.writeLine("Sent GRANT TO" + msg.msgToString());

		synchronized (this.totalMsgNumber) {
			totalMsgNumber++;
		}

		SocketClient socketclient = new SocketClient(clientAddr, this,
				GlobalConstant.clientPortNumber);
		socketclient.sendMsg(grantMsg);
	}

	/**
	 * initialize the response message
	 * 
	 * @return
	 */
	private Message initACKMsg() {
		Message responseMsg = new Message();
		responseMsg.setId(UtilityFileSystem.getHostname());
		responseMsg.setCmd(MsgCommand.ACK); // default is FAILURE
		responseMsg.setDetail("");
		return responseMsg;
	}
}
