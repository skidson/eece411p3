package ca.ubc.ece.nio.crawler;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public class GnutellaCrawler implements Crawler {
	// Constants
	public static final String REQUEST = "GNUTELLA CONNECT/0.6\r\n" + "User-Agent: UBCECE (crawl)\r\n" + "Query-Routing: 0.2\r\n" + "X-Ultrapeer: False\r\n" + "Crawler: 0.1\r\n" + "\r\n";
	public static final int FRONT = 0;
	
	private String[] node;
	private boolean abort = false;
	private boolean running = true;
	private SocketChannel socketChannel;
	private Object sync; //Used to determine which crawler needs to handle stuff
	private int id;
	private NIOServer server;

	public GnutellaCrawler(int id, NIOServer server){
		this.sync = new Object();
		this.id = id;
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
			String address = server.getWork();
			if (address != null)
				node = address.split(":");
			else
				continue; // TODO this might cause super processor consumption
			
			try {
				socketChannel = server.createConnection(node[0], Integer.parseInt(node[1]), id);
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
				continue;
			}

			System.out.println("Attempting to write  " + sync); // debug
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