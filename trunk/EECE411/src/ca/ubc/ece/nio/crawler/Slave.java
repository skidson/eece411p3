package ca.ubc.ece.nio.crawler;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class Slave implements Runnable, CrawlerNode {
	// Constants
	public static final int MS_TO_SEC = 1000;
	public static final int NIO_PORT = 1337;
	public static final int MANAGEMENT_PORT = 1377;
	public static final int NUM_CRAWLERS = 2;
	
	// Program variables
	private NIOServer server;
	private SlaveHandler handler;
	private boolean running = true;
	
	// Run settings
	private boolean full;
	private int timeout;
	private int duration;
	private String hostName;
	private int portNum;
	private String masterAddress;
	
	/* ************************************ INITIALIZATION ************************************ */
	public Slave(boolean full, int timeout, int duration, String hostName, int portNum) {
		this.full = full;
		this.timeout = timeout;
		this.duration = duration;
		this.hostName = hostName;
		this.portNum = portNum;
		this.handler = new SlaveHandler(this);
		this.server = new NIOServer(hostName, portNum, handler, this);
	}
	
	public static void main(String args[]) {
		// Default run settings
		boolean full = false;
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
		new Thread(new Slave(full, timeout, duration, hostName, portNum)).start();
	}
	
	public void run() {
		while(running) {
			idle(); // Idle until connection from master
			new Thread(server).start();
			for (int i = 0; i < NUM_CRAWLERS; i++)
				server.addCrawler(new GnutellaCrawler(server.getNumCrawlers(), server));
			reset(); // clear this run's data
		}
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
			handler.killWorker(0);
		this.handler = new SlaveHandler(this);
		run();
	}
	
	public void idle() {
		try {
			server.wait();
		} catch (InterruptedException e) {}
	}
	
	public void start() {
		synchronized(server) {
			server.notifyAll();
		}
	}
	
	public void kill() {
		// Kill this node, restart requires bash script or manual configuraton
		running = false;
		System.exit(0);
	}
	
	public String getMasterAddress() {
		return masterAddress;
	}
	
	public void sendToMaster(byte[] data){
		server.sendToMaster(data);
	}
	
	/* ************************************ EMBEDDED CLASSES ************************************ */
	public class MasterListener implements Runnable {
		ServerSocket listenerServer;
		
		public MasterListener(int portNum) {
			try {
				listenerServer = new ServerSocket(portNum);
			} catch (IOException e) {}
		}
		
		public void run() {
			while(true) {
				try {
					Socket socket = listenerServer.accept();
					// TODO can design custom Action class for use here to allow remote control
					start(); // Break from idle() loop
				} catch (IOException e) {}
			}
		}
	}
}
