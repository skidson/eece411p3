package ca.ubc.ece.nio.crawler;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Vector;

public class Slave implements Runnable {
	// Constants
	public static final int MS_TO_SEC = 1000;
	
	// Program variables
	private Vector<String> workList;
	private NIOServer server;
	private SlaveHandler handler;
	
	// Run settings
	private boolean full;
	private int timeout;
	private int duration;
	private String hostName;
	private int portNum;
	
	/* ************************************ INITIALIZATION ************************************ */
	public Slave(boolean full, int timeout, int duration, String hostName, int portNum) {
		workList = new Vector<String>();
		this.full = full;
		this.timeout = timeout;
		this.duration = duration;
		this.hostName = hostName;
		this.portNum = portNum;
		this.handler = new SlaveHandler(this);
		this.server = new NIOServer(hostName, portNum, handler);
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
		new Thread(server).start();
		try {
			server.wait();
		} catch (InterruptedException e) {}
		reset();
	}
	
	/* ************************************ HELPER METHODS ************************************ */
	
	public void reset() {
		// TODO clear this node's data and wait until further instruction
		workList = new Vector<String>();
		idle();
	}
	
	public void idle() {
		
	}
	
	public void kill() {
		// Kill this node (cannot be woken up via java)
		System.exit(0);
	}
	
	public void spawnCrawler() {
		
	}
	
	public void sendToMaster(byte[] data){
		server.sendToMaster(data);
	}
	
	/* ************************************ EMBEDDED CLASSES ************************************ */

}
