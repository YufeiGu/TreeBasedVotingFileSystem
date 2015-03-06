package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

import common.GlobalConstant;
import common.Message;
import common.MsgCommand;
import common.SaveInfo;
import common.UtilityFileSystem;

public class Client {
	// log tool
	SaveInfo logger = new SaveInfo();
	// grant servers
	HashSet<String> grantServers = new HashSet<String>();
	// current message serious number
	int currentSeriousNo = 0;
	// current access variable
	String currentAccessVar = "";
	// indicate if this access has completed
	boolean accessComplete;
	// isCommit
	boolean isCommit = false;
	//
	Timer timer = new Timer(false);
	// withdraw request
	int failAccess = 0;
	// array to record every time between issuing an access request and
	// receiving permission from the server tree.
	ArrayList<Long> roundTimes = new ArrayList<Long>();
	// request time
	private Calendar requestTime = null;

	/**
	 * main entry
	 */
	void run() {
		// open the log file
		logger.open(String.format("./ClientLog-%s.txt",
				UtilityFileSystem.getHostname()));

		// waiting for request from client
		try {
			new MultiThreadServer(this).start();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// command input
		//cmd();
		
		// access some times i.e. 500
		for (int i = 0; i < GlobalConstant.accessTimes; i++) {
			access();
		}

		stopClient();
	}

	/**
	 * print report and stop client
	 */
	private void stopClient() {
		// wait for 2 time unit to let withdraw sent
		try {
			Thread.sleep(2*GlobalConstant.awaitingGrant);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		printReport();
		
		logger.close();
		System.exit(0);
	}

	/**
	 * handle input command
	 */
	private void cmd() {
		String CurLine = "";
		System.out.println("Enter a line of text (type 'quit' to exit): ");
		InputStreamReader converter = new InputStreamReader(System.in);
		BufferedReader in = new BufferedReader(converter);
		while (true) {
			try {
				CurLine = in.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (!(CurLine.equals("quit"))) {
				// handleCmd(CurLine);
			} else {
				logger.close();
				System.exit(0);
			}
		}
	}

	private void printReport() {
		// The number of successful and unsuccessful accesses
		logger.writeLine(String.format("The number of successful access:%d",
				GlobalConstant.accessTimes - this.failAccess));
		logger.writeLine(String.format("The number of unsuccessful access:%d",
				this.failAccess));

		// For the successful accesses, the minimum, maximum, and average time
		// between issuing an access request
		// and receiving permission from the server tree.
		long min = Long.MAX_VALUE, max = 0, sum = 0;
		for (int i = 0; i < roundTimes.size(); i++) {
			long tmp = roundTimes.get(i);
			sum = sum + tmp;
			if (min > tmp)
				min = tmp;
			if (max < tmp)
				max = tmp;
		}
		long average = sum / roundTimes.size();
		logger.writeLine(String.format("Min:%d,Max:%d,Average:%d", min, max,
				average));
	}

	private void access() {
		// wait for 8 time unit
		try {
			Thread.sleep(GlobalConstant.timeUnit * 8);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// initialize commit state
		this.isCommit = false;
		// clear grantServers
		this.grantServers.clear();
		//send requests to all servers
		request();
//		logger.writeLine("access"+Boolean.toString(accessComplete));
//		while (!accessComplete) {
//
//		}
//		logger.writeLine("access"+Boolean.toString(accessComplete));
	}

	/**
	 * send request to all file servers
	 */
	private void request() {
		// access arbitrary one variable
		int index = UtilityFileSystem.createRandom(0, 3);
		currentAccessVar = "D" + Integer.toString(index);

		Message request = new Message();
		currentSeriousNo++;
		request.setSerialNo(this.currentSeriousNo);
		request.setId(UtilityFileSystem.getHostname());
		request.setCmd(MsgCommand.REQUEST);
		request.setDetail(currentAccessVar);
		request.setV(1);

		// set timer, if no response in 20 timer units, send withdraw to all
		// servers
		timer = new Timer(false);
		timer.schedule(new Worker(this, request), GlobalConstant.awaitingGrant);

		this.requestTime = Calendar.getInstance();

		// send requst to all servers
//		synchronized (this) {
			logger.writeLine("Request " + request.msgToString());
			for (String server : GlobalConstant.servers) {
				SocketClient socketclient = new SocketClient(server, this,
						GlobalConstant.fileserverPortNumber);
				socketclient.sendMsg(request);
			}
//		}
	}

	/**
	 * start point
	 * 
	 * @param args
	 */
	public static void main(String args[]) {
		Client client = new Client();
		client.run();
	}

	public void handleMsg(Message msg, String clientName, ObjectOutputStream out) {
		// if the request has been committed, no more action
		if (this.isCommit == true) {
			logger.writeLine("iscommit == true");
			return;
		}

		if (msg.getCmd() == MsgCommand.GRANT
				&& msg.getSerialNo() == this.currentSeriousNo) {
			logger.writeLine("Receive grant from server:" + msg.getId());
			// add to grant array

			this.grantServers.add(clientName);

			// determin whether satisfy the consistency
			boolean isConsistency = isConsistency(0);

			// if satisfy, then commit to all server
			if (isConsistency) {
				Calendar now = Calendar.getInstance();
				//add to round time
				this.roundTimes.add(now.getTimeInMillis()
						- this.requestTime.getTimeInMillis());

				logger.writeLine("Consistency!");
				// stop handle the msg
				this.isCommit = true;
				// stop timer
				this.timer.cancel();
				// sleep for HOLD_TIME
				try {
					Thread.sleep(GlobalConstant.holdTime);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				// commit
				commit(msg);
			}
		}

	}

	/**
	 * Determine if the tree rooted by i satisfies consistency
	 * 
	 * @param i
	 * @return
	 */
	private boolean isConsistency(int i) {
		synchronized (this.grantServers) {
			int serverNumber = GlobalConstant.servers.length;
			if (i >= serverNumber)
				return false;

			String root = GlobalConstant.servers[i];
			if (this.grantServers.contains(root)) {
				// it is a leaf
				if (2 * i + 1 >= serverNumber)
					return true;

				// it's a subtree, and if left consistency or right
				// consistency,return true
				if (isConsistency(2 * i + 1) || isConsistency(2 * i + 2))
					return true;
				else
					return false;
			} else {
				// it's a leaf
				if (2 * i + 1 >= serverNumber)
					return false;

				// it's a subtree, and if left consistency and right
				// consistency,return true
				if (isConsistency(2 * i + 1) && isConsistency(2 * i + 2))
					return true;
				else
					return false;
			}
		}
	}

	private void commit(Message msg) {
		logger.writeLine("commit:" + Integer.toString(currentSeriousNo));
		Message commit = new Message();
		commit.setSerialNo(msg.getSerialNo());
		commit.setId(UtilityFileSystem.getHostname());
		commit.setCmd(MsgCommand.COMMIT);
		commit.setDetail(msg.getDetail());
		commit.setV(msg.getV()); // v is 1

		logger.writeLine("Commit " + commit.msgToString());
		for (String server : GlobalConstant.servers) {
			SocketClient socketclient = new SocketClient(server, this,
					GlobalConstant.fileserverPortNumber);
			socketclient.sendMsg(commit);
		}

//		this.accessComplete = true;
	}

	void  withDraw(Message msg) {
			this.isCommit = true;

			this.failAccess++;

			Message withdrawMsg = new Message();
			withdrawMsg.setSerialNo(msg.getSerialNo());
			withdrawMsg.setId(msg.getId());
			withdrawMsg.setCmd(MsgCommand.WITHDRAW);
			withdrawMsg.setDetail(msg.getDetail());

			logger.writeLine("Withdraw" + withdrawMsg.msgToString());
//			logger.writeLine("before withdraw"+Boolean.toString(accessComplete));
			
			for (String server : GlobalConstant.servers) {
				SocketClient socketclient = new SocketClient(server, this,
						GlobalConstant.fileserverPortNumber);
				socketclient.sendMsg(withdrawMsg);
			}

//			this.accessComplete = true;
//			logger.writeLine("after withdraw"+Boolean.toString(accessComplete));
	}
}

/**
 * send periodic Info to MServer
 * 
 * @author GuYufei
 * 
 */
class Worker extends TimerTask {
	Client client;
	Message msg;

	public Worker(Client client, Message msg) {
		this.client = client;
		this.msg = msg;
	}

	public void run() {
		this.client.withDraw(this.msg);
	}
}