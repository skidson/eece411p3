package ca.ubc.ece.nio.crawler;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.Vector;

public class Master implements Runnable {
	// Constants
	private static final int MS_TO_SEC = 1000;
	private static final int NODE_COUNT = 550;
	
	// Run settings
	private boolean full;
	private boolean verbose;
	private int timeout;
	private int duration;
	private String hostName;
	private int portNum;
	
	// Program variables
	private NIOServer server;
	private NodeController controller;
	private Vector<String> ultraList, leafList;
	private Vector<byte[]> dataList;
	private Vector<Node> nodeList;
	private IPCache ipCache;
	
	/* ************************************ INITIALIZATION ************************************ */
	
	public Master(boolean full, boolean verbose, int timeout, int duration, String hostName, int portNum) {
		this.full = full;
		this.verbose = verbose;
		this.timeout = timeout;
		this.duration = duration;
		this.hostName = hostName;
		this.portNum = portNum;
		this.server = new NIOServer(hostName, portNum, new MasterHandler());
		this.nodeList = new Vector<Node>();
		this.ipCache = new IPCache();
		BufferedReader br;
		String[] allNodes = new String[NODE_COUNT];
		try {
			br = new BufferedReader(new FileReader("node_list_all"));
			for (int i = 0; i < allNodes.length; i++) {
				String newLine = br.readLine();
				if (newLine != null) {
					allNodes[i] = newLine;
					System.out.println(allNodes[i]);
				}
			}
		} catch (FileNotFoundException e) {
			System.err.println("Error: Could not find node list.");
		} catch (IOException e) {}
		controller = new NodeController(allNodes);
	}
	
	public static void main(String[] args) {
		// Default run settings
		boolean full = false;
		boolean verbose = false;
		int timeout = 20;
		int duration = 15;
		String hostName = "localhost";
		int portNum = 1337;
		
		try {
			hostName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {}
		
		try {
	    	for (String arg : args) {
	    		if (arg.equals("-full")) {
	    			full = true;
	    		} else if (arg.equals("-minimal")) {
	    			full = false;
	    		} else if (arg.equals("-v")) {
	    			verbose = true;
	    		} else if (arg.startsWith("timeout=")) {
	    			String[] temp = arg.split("=");
	    			timeout = Integer.parseInt(temp[1])*MS_TO_SEC;
	    		} else if (arg.indexOf(":") != -1) {
	    			String[] temp = arg.split(":");
	    			hostName = temp[0];
	    			portNum = Integer.parseInt(temp[1]);
	    		} else {
	    			duration = Integer.parseInt(arg);
	    		}
	
	    		if (full)
	    			System.out.println("Output mode: full");
	    		else
	    			System.out.println("Output mode: minimal");
	    		System.out.println("Verbose mode: set");
	    		System.out.println("Connection timeout: " + timeout/MS_TO_SEC + " second(s)");
	    		System.out.println("Execution time: " + duration + " minute(s)\n");
	    	}
        } catch (Exception e) {
        	System.out.println("Usage:\n\tMain <-full | -minimal> timeout=XX <address:port> <timetorun>");
        	return;
        }
        new Thread(new Master(full, verbose, timeout, duration, hostName, portNum)).start();
	}
	
	public void run() {
		new Thread(server).start();
		while(true) {
			// TODO provide commandline interface
			System.out.print("\r\ncrawler>$ ");
			Scanner in = new Scanner(System.in);
			String command = in.nextLine();
			
			if(command.equals("print")) {
				
			} else if (command.equals("quit")) {
				// TODO tell all nodes to stop
				System.exit(0);
			} else if (command.equals("status")) {
				
			} else {
				printHelp();
			}
		}
	}
	
	/* ************************************ HELPER METHODS ************************************ */
	
	private int parseData(byte[] data) {
		// TODO this needs to be cleaned up
		// Parsing will now only be done at master.
		// Requirements:
		// 1. Take in a byte[] data
		// 2. Extract ultrapeers and leaves from data
		// 3. Check each ultrapeer & leaf if cached
		//		- if not cached, add to ultraList or leafList in the form hostName:portNum (as a string)
		// 4. Cache this node's address
		// 5. Construct Node object from data[]
		// 6. Return the constructed Node object
		int status = 0;
		String[] tempArray;
		String[] tempArray2;
		String[] readArray;
		String ipPort;
		String dataS = new String(data);
		String Peers = new String();
		String Leaves = new String();
		int startIndex;
		int endIndex;        

		startIndex = dataS.indexOf("Peers: ");
		if (!(startIndex == -1)) {
			endIndex = dataS.indexOf("\n", startIndex);
			if (!(endIndex == -1)) {
				Peers = dataS.substring(startIndex+7, endIndex);

				tempArray = Peers.split(",");
				for (int j = 0; j < tempArray.length; j++) {
					ipPort = tempArray[j];
					readArray = ipPort.split(":");
					if (!(ipCache.isCached(readArray[0].toString()))) {
						readArray[1] = readArray[1].replaceAll("(\\r|\\n)", ""); 
						portNum = Integer.parseInt(readArray[1]);
						Node tempnode = new Node(readArray[0], portNum);
						ultraList.add(tempnode);
						synchronized(ultraList){
							ultraList.notifyAll();
						}
						ipCache.cache(readArray[0]);
						dumpList.add(tempnode);
					}
				}	
			} else status = -1;
		} else status = -1;
		startIndex = dataS.indexOf("Leaves: ");
		if (!(startIndex == -1)) {
			endIndex = dataS.indexOf("\n", startIndex);
			if (!(endIndex == -1)) {
				//System.out.println((startIndex+8) + "  " +  endIndex);
				Leaves = dataS.substring(startIndex+8,endIndex);

				tempArray2 = Leaves.split(",");
				if (!(tempArray2.length < 2)) {
					for (int k = 0; k< tempArray2.length; k++) {
						ipPort = tempArray2[k];
						readArray = ipPort.split(":");
						if (!(ipCache.isCached(readArray[0].toString()))) { 
							readArray[1] = readArray[1].replaceAll("(\\r|\\n)", ""); 
							int portNum2 = Integer.parseInt(readArray[1]);

							Node tempnode = new Node(readArray[0], portNum2);
							leafList.add(tempnode);
							//System.out.println(readArray[0]);
							ipCache.cache(readArray[0]);
							dumpList.add(tempnode);
						}
					}
				}
			} else {status = -1;}
		} else 
		{status = -1;}
		return status;
	}
	
	public void printHelp() {
		System.out.println("Commands: print, quit, status");
	}
	
	public void print() {
		
	}
	
	/* ************************************ EMBEDDED CLASSES ************************************ */
}
