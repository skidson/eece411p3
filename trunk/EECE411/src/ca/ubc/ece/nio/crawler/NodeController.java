package ca.ubc.ece.nio.crawler;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Vector;

public class NodeController {
	// Constants
	private static final int NODE_COUNT = 550;
	
	// Program variables
	private Vector<WorkerNode> nodes;
	
	/* ************************************ INITIALIZATION ************************************ */
	public NodeController(String[] nodes) {
		for (String address : nodes) {
			WorkerNode worker = new WorkerNode(address);
			this.nodes.add(worker);
		}
	}
	
	public NodeController(String filename) {
		// Reads in a file given by filename that contains a list of worker nodes
		BufferedReader br;
		String[] allNodes = new String[NODE_COUNT];
		try {
			br = new BufferedReader(new FileReader("node_list_all"));
			for (int i = 0; i < allNodes.length; i++) {
				String newLine = br.readLine();
				if (newLine != null) {
					WorkerNode worker = new WorkerNode(newLine);
					this.nodes.add(worker);
					System.out.println(allNodes[i]); // debug
				}
			}
		} catch (FileNotFoundException e) {
			System.err.println("Error: Could not find file '" + filename + "'");
		} catch (IOException e) {}
	}
	
	/* ************************************ HELPER METHODS ************************************ */
	
	private Socket connect(WorkerNode node, int port) throws UnknownHostException, IOException {
		Socket socket = null;
		socket = new Socket(InetAddress.getByName(node.getAddress()), port);
		return socket;
	}
	
	private Socket connect(int index, int port) throws UnknownHostException, IOException {
		Socket socket = null;
		socket = new Socket(InetAddress.getByName(nodes.get(index).getAddress()), port);
		return socket;
	}
	
	public int getNumAlive() {
		int count = 0;
		for (WorkerNode node : nodes) {
			if (node.isAlive())
				count++;
		}
		return count;
	}
	
	public int getNumDead() {
		return (getNumTotal() - getNumAlive());
	}
	
	public int getNumTotal() {
		return nodes.size();
	}
	
	public String getAddress(int index) {
		return nodes.get(index).getAddress();
	}
	
	/* ************************************ EMBEDDED CLASSES ************************************ */
	private class WorkerNode {
		private String address;
		private boolean alive;
		private long lastAccess;
		
		public WorkerNode(String address) {
			this.address = address;
		}
		
		protected void setAlive(boolean alive) {
			this.alive = alive;
		}
		
		protected void setLastAccess(long time) {
			this.lastAccess = time;
		}
		
		public String getAddress() { return this.address; }
		public long getLastAccess() { return this.lastAccess; }
		public boolean isAlive() { return this.alive; }
	}
	
	// TODO need to store time last accessed for each node and check up regularly
	
}
