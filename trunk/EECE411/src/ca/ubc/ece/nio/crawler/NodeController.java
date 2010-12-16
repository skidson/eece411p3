package ca.ubc.ece.nio.crawler;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

public class NodeController {
	// Constants
	private static final int NODE_COUNT = 550;
	
	// Program variables
	private Vector<WorkerNode> nodes;
	private int workerCount = 0;
	
	/* ************************************ INITIALIZATION ************************************ */
	public NodeController(String[] addresses) {
		nodes = new Vector<WorkerNode>();
		for (String address : addresses) {
			WorkerNode worker = new WorkerNode(address);
			this.nodes.add(worker);
			System.out.println(address);
		}
	}
	
	public NodeController(String filename) {
		// Reads in a file given by filename that contains a list of worker nodes
		nodes = new Vector<WorkerNode>();
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(filename));
			for (int i = 0; i < NODE_COUNT; i++) {
				String newLine = br.readLine();
				if (newLine != null) {
					WorkerNode worker = new WorkerNode(newLine);
					this.nodes.add(worker);
				} else {
					break;
				}
			}
		} catch (FileNotFoundException e) {
			System.err.println("Error: Could not find file '" + filename + "'");
			e.printStackTrace();
		} catch (IOException e) {}
	}
	
	/* ************************************ HELPER METHODS ************************************ */
	
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
	
	public int getNumWorking() {
		int count = 0;
		for (WorkerNode node : nodes) {
			if (node.getWorking())
				count++;
		}
		return count;
	}
	
	public void setWorking(int index, boolean working) {
		nodes.get(index).setWorking(working);
	}
	
	public String getAddress(int index) {
		return nodes.get(index).getAddress();
	}
	
	public boolean isWorking(int index) {
		return nodes.get(index).getWorking();
	}
	
	public Vector<String> selectWorkers(int num) {
		Vector<String> workers = new Vector<String>();
		for (int i = 0; i < num; i++) {
			workers.add(nodes.get((i + workerCount)%nodes.size()).getAddress());
			System.out.println(i + ".  SELECTING " + nodes.get((i + workerCount)%nodes.size()).getAddress());
		}
		workerCount += num;
		return workers;
	}
	
	public String selectWorker() {
		String worker = nodes.get(workerCount).getAddress();
		System.out.println("SELECTING " + nodes.get((workerCount)%nodes.size()).getAddress());
		workerCount++;
		return worker;
	}
	
	/* ************************************ EMBEDDED CLASSES ************************************ */
	private class WorkerNode {
		private String address;
		private boolean alive = false;
		private boolean working = false;
		private long lastAccess = 0;
		
		public WorkerNode(String address) {
			this.address = address;
		}
		
		protected void setAlive(boolean alive) {
			this.alive = alive;
		}
		
		protected void setLastAccess(long time) {
			this.lastAccess = time;
		}
		
		protected void setWorking(boolean working) {
			this.working = working;
		}
		
		public String getAddress() { return this.address; }
		public long getLastAccess() { return this.lastAccess; }
		public boolean isAlive() { return this.alive; }
		public boolean getWorking() { return(this.working); }
	}
	
	// TODO need to store time last accessed for each node and check up regularly
	
}
