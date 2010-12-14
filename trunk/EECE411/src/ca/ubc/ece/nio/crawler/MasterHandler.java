package ca.ubc.ece.nio.crawler;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.Vector;

public class MasterHandler implements DataHandler {
	// Constants
	private static final int FRONT = 0;
	private static final String OUTPUT_FILE = "results.txt";
	
	// Program variables
	private Master owner;
	private Vector<byte[]> dataList;
	private Vector<Logger> workerList;
	
	/* ************************************ INITIALIZATION ************************************ */
	public MasterHandler(Master owner) {
		this.owner = owner;
		dataList = new Vector<byte[]>();
		workerList = new Vector<Logger>();
	}
	
	/* ************************************ HELPER METHODS ************************************ */
	public void handle(byte[] data) {
		synchronized(dataList){
			dataList.add(data);
			dataList.notifyAll();
		}
	}
	
	public void connectFailed(SelectionKey key) {
		
	}
	
	public void finishRead(SelectionKey key) throws IOException {
		// TODO don't think we need anything here
	}
	
	public void finishWrite(SelectionKey key) throws IOException {
		
	}
	
	/* Workers may be spawned or killed based on free memory */
	public void spawnWorker() {
		Logger logger = new Logger();
		workerList.add(logger);
		new Thread(logger).start();
	}
	
	public void killWorker(int index) {
		workerList.get(index).kill();
	}
	
	public int getNumWorkers() {
		return workerList.size();
	}
	
	private Node parseData(byte[] data) {
		// Parsing will now only be done at master.
		// 1. Take in a byte[] data
		// 2. Extract ultrapeers and leaves from data
		// 3. Check each ultrapeer & leaf if cached
		//		- if not cached, add to ultraList or leafList in the form hostName:portNum (as a string)
		// 4. Construct Node object from data[]
		// 5. Return the constructed Node object
		Node tempnode = null;
		String[] tempArray, tempArray2, readArray;
		String ipPort, Address, Port, addressPort;
		String dataS = new String(data);
		String Peers = new String();
		String Leaves = new String();
		int startIndex, portNum, endIndex;        
		
		startIndex = dataS.indexOf("Address: ");
		endIndex = dataS.indexOf("\n", startIndex);
		Address = dataS.substring(startIndex+9, endIndex);
		
		startIndex = dataS.indexOf("Port: ");
		endIndex = dataS.indexOf("\n", startIndex);
		Port = dataS.substring(startIndex+6, endIndex);
		tempnode = new Node(Address, Integer.parseInt(Port));
		
		startIndex = dataS.indexOf("Peers: ");
		if (!(startIndex == -1)) {
			endIndex = dataS.indexOf("\n", startIndex);
			if (!(endIndex == -1)) {
				Peers = dataS.substring(startIndex+7, endIndex);

				tempArray = Peers.split(",");
				for (int j = 0; j < tempArray.length; j++) {
					ipPort = tempArray[j];
					readArray = ipPort.split(":");
					if (!(owner.ipCache.isCached(readArray[0].toString()))) {
						readArray[1] = readArray[1].replaceAll("(\\r|\\n)", ""); 
						portNum = Integer.parseInt(readArray[1]);
						addressPort = readArray[1] + ":" + portNum;
						owner.addUltrapeer(addressPort);
					}
				}	
			} 
		} 
		startIndex = dataS.indexOf("Leaves: ");
		if (!(startIndex == -1)) {
			endIndex = dataS.indexOf("\n", startIndex);
			if (!(endIndex == -1)) {
				//System.out.println((startIndex+8) + "  " +  endIndex);
				Leaves = dataS.substring(startIndex+8,endIndex);

				tempArray2 = Leaves.split(",");
				if (!(tempArray2.length < 2)) {
					for (int k = 0; k< tempArray2.length; k++) {
						ipPort = tempArray2[k];
						readArray = ipPort.split(":");
						if (!(owner.ipCache.isCached(readArray[0].toString()))) { 
							readArray[1] = readArray[1].replaceAll("(\\r|\\n)", ""); 
							int portNum2 = Integer.parseInt(readArray[1]);
							addressPort = readArray[1] + ":" + portNum2;
							owner.addLeaf(addressPort);
						}
					}
				}
			} 
		} 
		return tempnode;
	}
	
	/* ************************************ EMBEDDED CLASSES ************************************ */
	private class Logger implements Runnable {
		boolean running = true;
		
		public void run() {
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(OUTPUT_FILE));
			} catch (IOException e1) {
				System.err.println("Could not access '" + OUTPUT_FILE + "'");
			}
			while(running) {
				if (dataList.isEmpty()) {
					try {
						dataList.wait();
					} catch (InterruptedException e) { continue; }
				}
				Node node = parseData(dataList.remove(FRONT));
				owner.addNode(node);
				owner.ipCache.cache(node.getAddress());
			}
		}
		
		public void kill() {
			this.running = false;
		}
	}
	
}
