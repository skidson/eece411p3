package ca.ubc.ece.nio.crawler.master;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;

import ca.ubc.ece.nio.crawler.Crawler;
import ca.ubc.ece.nio.crawler.NIOServer;

public class InternalCrawler implements Crawler {
	// Constants
	public static final int FRONT = 0;
	public static final String WAKE_REQUEST = "WAKEUP;";
	public static final String KILL_REQUEST = "DIE;";
	
	private boolean abort = false;
	private boolean running = true;
	private String request = WAKE_REQUEST;
	private SocketChannel socketChannel;
	private Object sync; //Used to determine which crawler needs to handle stuff
	private int id;
	private MasterHandler handler;
	private NIOServer server;

	public InternalCrawler(int id, MasterHandler handler, NIOServer server){
		this.sync = new Object();
		this.id = id;
		this.handler = handler;
		this.server = server;
		this.request = request + server.getMasterAddress();
	}

	public void abort() {
		this.abort = true;
	}

	public void kill() {
		this.running = false;
	}

	public void wake() {
		synchronized(sync) {
			sync.notifyAll();
		}
	}

	public void run(){
		while(running){
			String address = handler.getWork();
			System.out.println("InternalCrawler " + id + " crawling " + address);
			try {
				socketChannel = server.createConnection(address, server.getPort(), id);
			} catch (UnresolvedAddressException e) {
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			// Wait for connection to finish before writing	
			synchronized(sync) {
				try {
					System.out.println("InternalCrawler " + id + " waiting for connection..."); // debug
					sync.wait();
				} catch (InterruptedException e) {}
			}

			if(abort) {
				System.out.println("InternalCrawler " + id + " connection aborted"); // debug
				abort = false;
				handler.replaceWorker(address);
				continue;
			}

			System.out.println("InternalCrawler " + id + " attempting to wake " + address); // debug
			server.send(socketChannel, request.getBytes(), id);

			// Wait for this connection to be closed so we can open another
			synchronized(sync) {
				try {
					System.out.println("InternalCrawler " + id + " waiting for close..."); // debug
					sync.wait();
				} catch (InterruptedException e) {}
			}
		}
	}
	
	public void setRequest(String newRequest) {
		this.request = newRequest;
	}
	
	public void setToWake() {
		this.request = WAKE_REQUEST;
	}
	
	public void setToKill() {
		this.request = KILL_REQUEST;
	}
}