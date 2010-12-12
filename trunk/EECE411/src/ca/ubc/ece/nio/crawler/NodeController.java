package ca.ubc.ece.nio.crawler;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Vector;

public class NodeController {
	private Vector<WorkerNode> nodes;
	
	/* ************************************ INITIALIZATION ************************************ */
	public NodeController(String[] nodes) {
		for (String address : nodes) {
			WorkerNode worker = new WorkerNode(address);
			this.nodes.add(worker);
		}
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
		private long latency;
		
		public WorkerNode(String address) {
			this.address = address;
		}
		
		protected void setLatency(long latency) {
			this.latency = latency;
		}
		
		protected void setAlive(boolean alive) {
			this.alive = alive;
		}
		
		public String getAddress() { return this.address; }
		public long getLatency() { return this.latency; }
		public boolean isAlive() { return this.alive; }
	}
	
}
