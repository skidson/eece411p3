package ca.ubc.ece.nio.crawler;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public class SliceCrawler implements Crawler {
	// Constants
	public static final int FRONT = 0;
	public static final String REQUEST = "WAKEUP";
	
	private boolean abort = false;
	private boolean running = true;
	private SocketChannel socketChannel;
	private Object sync; //Used to determine which crawler needs to handle stuff
	private int id;
	private MasterHandler handler;
	private NIOServer server;

	public SliceCrawler(int id, MasterHandler handler, NIOServer server){
		this.sync = new Object();
		this.id = id;
		this.handler = handler;
		this.server = server;
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
			
			try {
				socketChannel = server.createConnection(address, server.getPort(), id);
			} catch (IOException e) {}
			// Wait for connection to finish before writing	
			synchronized(sync) {
				try {
					System.out.println("crawler : " + sync + " waiting"); // debug
					sync.wait();
				} catch (InterruptedException e) {}
			}

			if(abort) {
				abort = false;
				handler.removeWorkerNode(address);
				continue;
			}

			System.out.println("Attempting to wake  " + sync); // debug
			server.send(socketChannel, REQUEST.getBytes());

			// Wait for this connection to be closed so we can open another
			synchronized(sync) {
				try {
					System.out.println("Crawler : " + sync + " waiting");
					sync.wait();
				} catch (InterruptedException e) {}
			}
		}	
	}
}