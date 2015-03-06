package common;

/**
 * global constant
 * @author GuYufei
 *
 */
public class GlobalConstant {
	//file server port number
	public final static int fileserverPortNumber =2006;
	//server in client port number
	public final static int clientPortNumber = 2005;
	//time unit 1000 ms(1 second)
	public final static int timeUnit = 80;
	//access times, the number of access each client 
	public final static int accessTimes=500;
	//awaiting grant time
	public final static int awaitingGrant = 20* timeUnit;
	//HOLD_TIME
	public final static int holdTime = (int)(1.5 * timeUnit);
	//server list
//	public final static String[] servers = {"GuYufei-PC"};	
	public final static String[] servers = {"net39.utdallas.edu"
		,"net04.utdallas.edu","net38.utdallas.edu","net06.utdallas.edu","net07.utdallas.edu",
		"net08.utdallas.edu","net09.utdallas.edu"};	
}
