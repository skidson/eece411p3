package ca.ubc.ece.nio.crawler.slave;

import java.net.InetAddress;
import java.net.UnknownHostException;

import ca.ubc.ece.nio.crawler.Node;
import ca.ubc.ece.nio.crawler.NIOServer;

public class Slave implements Node {
	// Constants
	public static final int MS_TO_SEC = 1000;
	public static final int NIO_PORT = 1337;
	public static final int MANAGEMENT_PORT = 1377;
	public static final int NUM_CRAWLERS = 2;
	public static final String DEFAULT_MASTER = "146-179.surfsnel.dsl.internl.net";
	
	// Program variables
	private NIOServer server;
	private SlaveHandler handler;
	private boolean running = false;
	
	// Run settings
	private boolean full;
	private int timeout;
	private int duration;
	private String hostName;
	private int portNum;
	private String masterAddress;
	private String backupAddress;
	
	/* ************************************ INITIALIZATION ************************************ */
	public Slave(boolean full, int timeout, int duration, String hostName, int portNum) {
		this.full = full;
		this.timeout = timeout;
		this.duration = duration;
		this.hostName = hostName;
		this.portNum = portNum;
		this.handler = new SlaveHandler(this);
		this.masterAddress = DEFAULT_MASTER;
		this.server = new NIOServer(hostName, portNum, handler, this);
	}
	
	public static void main(String args[]) {
		// Default run settings
		boolean full = false;
		int timeout = 5;
		int duration = 15;
		String hostName = null;
		try {
			hostName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e1) {}
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
	    		System.out.println("Connection timeout: " + timeout/MS_TO_SEC + " second(s)");
	    		System.out.println("Execution time: " + duration + " minute(s)\n");
	    	}
        } catch (Exception e) {
        	System.out.println("Usage:\n\tMain <-full | -minimal> timeout=XX <address:port> <timetorun>");
        	return;
        }
		new Thread(new Slave(full, timeout, duration, hostName, portNum)).start();
	}
	
	public void run() {
		//idle(); // Idle until connection from master
		running = false;
		new Thread(server).start();
		idle();
		running = true;
		for (int i = 0; i < NUM_CRAWLERS; i++)
			server.addCrawler(new ExternalCrawler(server.getNumCrawlers(), handler, server));
		handler.spawnWorker();
		idle();
	}
	
	/* ************************************ HELPER METHODS ************************************ */
	
	public void reset() {
		this.server.reset();
		try {
			// Allow for other threads to shutdown
			Thread.sleep(5000);
		} catch (InterruptedException e) {}
		int workerCount = handler.getNumWorkers();
		for(int i = 0; i < workerCount; i++)
			handler.killWorker();
		this.handler = new SlaveHandler(this);
		run();
	}
	
	public void idle() {
		System.out.println("Idling...");
		try {
			synchronized(server) {
				server.wait();
			}
		} catch (InterruptedException e) {}
		System.out.println("Waking up...");
	}
	
	public void wake(String request) {
		if (request.contains("MASTER")) {
			this.masterAddress = request.split("=")[1];
		}
		
		if (request.contains("BACKUP")) {
			this.masterAddress = request.split("=")[1];
		}
		
		synchronized(sync) {
			sync.notifyAll();
		}
		
		if (!running) {
			synchronized(server) {
				server.notifyAll();
			}
		}
	}
	
	public void kill() {
		// Kill this node, restart requires bash script or manual configuraton
		running = false;
		synchronized(server) { 
			server.notifyAll();
		}
		System.exit(0);
	}
	
	public String getMasterAddress() {
		return masterAddress;
	}
	
	public String getBackupAddress() {
		return backupAddress;
	}
	
	public void sendToMaster(byte[] data, int id){
		server.sendToMaster(data, id);
	}
	
	public int getTimeout() {
		return timeout;
	}
	
	/* ************************************ EMBEDDED CLASSES ************************************ */
	
}
