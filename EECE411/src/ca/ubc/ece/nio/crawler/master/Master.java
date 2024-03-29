package ca.ubc.ece.nio.crawler.master;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.util.Vector;

import ca.ubc.ece.nio.crawler.Node;
import ca.ubc.ece.nio.crawler.NIOServer;

public class Master implements Node {
	// Constants
	private static final int MS_TO_SEC = 1000;
	private static final String NODE_LIST = "node_list.txt";
	public static final int MANAGEMENT_PORT = 1377;
	public static final int NUM_CRAWLERS = 1;
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
	private Vector<NodeData> nodeList;
	private MasterHandler handler;
	public IPCache ipCache;
	private String masterAddress;
	private String backupAddress;
	private boolean running = true;
	private long startTime;
	
	/* ************************************ INITIALIZATION ************************************ */
	
	public Master(boolean full, boolean verbose, int timeout, int duration, String hostName, int portNum) {
		try {
			this.masterAddress = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		this.full = full;
		this.verbose = verbose;
		this.timeout = timeout;
		this.duration = duration;
		this.hostName = hostName;
		this.portNum = portNum;
		this.handler = new MasterHandler(this);
		this.server = new NIOServer(hostName, portNum, handler, this);
		this.nodeList = new Vector<NodeData>();
		this.ipCache = new IPCache();
		this.controller = new NodeController(NODE_LIST);
		this.startTime = System.currentTimeMillis();
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
		
		while(running) {
			// TODO provide commandline interface
			System.out.print("\r\ncrawler>$ ");
			Scanner in = new Scanner(System.in);
			String command = in.nextLine();
			
			if(command.equals("print")) {
				print();
			} else if (command.equals("start")) {
				start();
			} else if (command.equals("quit")) {
				// TODO tell all nodes to stop
				System.exit(0);
			} else if (command.equals("reset")) {
				reset();
			} else if (command.equals("status")) {
				printStatus();
			} else if (command.equals("reset")) {
				break;
			} else {
				printHelp();
			}
		}
		reset();
	}
	
	/* ************************************ HELPER METHODS ************************************ */
	
	public void printHelp() {
		System.out.println("Commands: start, print, quit, status");
	}
	
	public void print() {
		for (NodeData node : nodeList)
			System.out.println(node.toString());
	}
	
	public void printStatus() {
		System.out.println("MEMORY: " + Runtime.getRuntime().freeMemory()/1000 + "/" + Runtime.getRuntime().totalMemory()/1000 + "MB");
		System.out.println("WORKERS: " + handler.getNumWorkers());
		System.out.println("CRAWLERS: " + server.getNumCrawlers());
		System.out.println("NODES CRAWLED: " + nodeList.size());
		System.out.println("RUNNING TIME: " + ((System.currentTimeMillis() - startTime)/MS_TO_SEC) + " seconds");
	}
	
	public void start() {
		Vector<String> workers = controller.selectWorkers(NUM_WORKERS);
		for(String worker : workers)
			handler.addNodeToWake(worker);
		
		for (int i = 0; i < NUM_CRAWLERS; i++) {
			server.addCrawler(new InternalCrawler(server.getNumCrawlers(), handler, server));
		}
	}
	
	public void reset() {
		this.server.reset();
		try {
			// Allow for other threads to shutdown
			Thread.sleep(5000);
		} catch (InterruptedException e) {}
		int workerCount = handler.getNumWorkers();
		for(int i = 0; i < workerCount; i++)
			handler.killWorker();
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
	
	public void addNode(NodeData node) {
		nodeList.add(node);
	}
	
	public void sendWork(String work, SocketChannel socketChannel){
		server.send(socketChannel, work.getBytes(), -1);
	}
	
	public String getMasterAddress() {
		return masterAddress;
	}
	
	public String getBackupAddress() {
		return backupAddress;
	}
	
	public void wake(String request) {
		// TODO wakeup and act as backup
		synchronized(sync) {
			sync.notifyAll();
		}
		synchronized(server) {
			server.notifyAll();
		}
	}
	
	public void backup(Vector<NodeData> nodelist) {
		// TODO send list to backup
	}

	public int getPortNum() {
		return portNum;
	}
	
	public int getTimeout() {
		return timeout;
	}
	
	public String replaceSlave(String deadNode) {
		return(controller.replaceWorker(deadNode));
	}
	
	/* ************************************ EMBEDDED CLASSES ************************************ */
}
