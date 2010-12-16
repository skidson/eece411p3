package ca.ubc.ece.nio.crawler;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

public class NodeController {
	// Constants
	
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
			while(true) {
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
	
	public String getAddress(int index) {
		return nodes.get(index).getAddress();
	}
	
	public String replaceWorker(String deadNode) {
		for (WorkerNode node : nodes) {
			if (node.getAddress().equals(deadNode)) {
				node.setAlive(false);
				node.setWorking(false);
				System.out.println("Removing unresponsive node " + node.getAddress() + " from working set..."); // debug
				break;
			}
		}
		String newWorker = selectWorker();
		System.out.println("Adding node " + newWorker + " to working set..."); // debug
		return(selectWorker());
	}
	
	public boolean isWorking(int index) {
		return nodes.get(index).getWorking();
	}
	
	public Vector<String> selectWorkers(int num) {
		Vector<String> workers = new Vector<String>();
		for (int i = 0; i < num; i++) {
			WorkerNode worker = nodes.get((i + workerCount)%nodes.size());
			workers.add(worker.getAddress());
			System.out.println(i + ".  SELECTING " + worker.getAddress()); // debug
			worker.setWorking(true);
		}
		workerCount += num;
		return workers;
	}
	
	public String selectWorker() {
		while(nodes.get(workerCount).skipped())
			workerCount++;
		
		WorkerNode worker = nodes.get(workerCount);
		System.out.println(workerCount + ".  SELECTING " + worker.getAddress()); // debug
		worker.setWorking(true);
		workerCount++;
		return worker.getAddress();
	}
	
	/* ************************************ EMBEDDED CLASSES ************************************ */
	private class WorkerNode {
		private String address;
		private boolean alive = true;
		private boolean working = false;
		private boolean skipped = false;
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
		
		public boolean skipped() {
			// Skip this node if it was dead last time, unless already skipped once
			boolean temp = skipped;
			if (skipped)
				skipped = false;
			return(temp);
		}
	}
	
	// TODO need to store time last accessed for each node and check up regularly
	
}
