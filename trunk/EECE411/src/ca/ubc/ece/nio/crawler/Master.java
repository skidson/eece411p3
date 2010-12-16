package ca.ubc.ece.nio.crawler;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.util.Vector;

public class Master implements Runnable, CrawlerNode {
	// Constants
	private static final int MS_TO_SEC = 1000;
	private static final String NODE_LIST = "node_list_all.txt";
	public static final int MANAGEMENT_PORT = 1377;
	public static final int NUM_CRAWLERS = 5;
	public static final int NUM_WORKERS = 100;
	
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
	private Vector<Node> nodeList;
	private MasterHandler handler;
	public IPCache ipCache;
	private String masterAddress;
	private boolean running = true;
	
	/* ************************************ INITIALIZATION ************************************ */
	
	public Master(boolean full, boolean verbose, int timeout, int duration, String hostName, int portNum) {
		this.full = full;
		this.verbose = verbose;
		this.timeout = timeout;
		this.duration = duration;
		this.hostName = hostName;
		this.portNum = portNum;
		this.handler = new MasterHandler(this);
		this.server = new NIOServer(hostName, portNum, handler, this);
		this.nodeList = new Vector<Node>();
		this.ipCache = new IPCache();
		this.controller = new NodeController(NODE_LIST);
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
		Vector<String> workers = controller.getWorkers(NUM_WORKERS);
		for(String worker : workers)
			handler.addNodeToWake(worker);
		
		for (int i = 0; i < NUM_CRAWLERS; i++)
			server.addCrawler(new SliceCrawler(server.getNumCrawlers(), handler, server));
		
		while(running) {
			// TODO provide commandline interface
			
			System.out.print("\r\ncrawler>$ ");
			Scanner in = new Scanner(System.in);
			String command = in.nextLine();
			
			if(command.equals("print")) {
				print();
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
	
	public void printHelp() {
		System.out.println("Commands: print, quit, status");
	}
	
	public void print() {
		// TODO
	}
	
	public void reset() {
		this.server.reset();
		try {
			// Allow for other threads to shutdown
			Thread.sleep(5000);
		} catch (InterruptedException e) {}
		int workerCount = handler.getNumWorkers();
		for(int i = 0; i < workerCount; i++)
			handler.killWorker(0);
		this.handler = new MasterHandler(this);
		run();
	}
	
	public void kill() {
		// Kill this node, restart requires bash script or manual configuraton
		running = false;
		synchronized(server) { 
			server.notifyAll();
		}
		System.exit(0);
	}
	
	public void addNode(Node node) {
		nodeList.add(node);
	}
	
	public void sendWork(String work, SocketChannel socketChannel){
		server.send(socketChannel, work.getBytes());
	}
	
	public boolean wakeNode(int index) {
		controller.getAddress(index);
		return false;
	}
	
	public String getMasterAddress() {
		return masterAddress;
	}
	
	public void wake(byte[] data) {
		// TODO wakeup and act as backup
		synchronized(server) {
			server.notifyAll();
		}
	}
	
	public void backup(Vector<Node> nodelist) {
		// TODO send list to master
	}

	public int getPortNum() {
		return portNum;
	}
	
	/* ************************************ EMBEDDED CLASSES ************************************ */
}
