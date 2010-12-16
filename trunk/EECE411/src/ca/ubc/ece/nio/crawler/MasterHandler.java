package ca.ubc.ece.nio.crawler;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Vector;

public class MasterHandler implements DataHandler {
	// Constants
	private static final int FRONT = 0;
	private static final String OUTPUT_FILE = "results.txt";
	
	// Program variables
	private Master owner;
	private Vector<byte[]> dataList;
	private Vector<Logger> loggerList;
	private Vector<String> ultraList, leafList, workingList, wakeList;
	
	/* ************************************ INITIALIZATION ************************************ */
	public MasterHandler(Master owner) {
		this.owner = owner;
		this.dataList = new Vector<byte[]>();
		this.ultraList = new Vector<String>();
		this.leafList = new Vector<String>();
		this.wakeList = new Vector<String>();
		this.workingList = new Vector<String>();
		this.loggerList = new Vector<Logger>();
	}
	
	/* ************************************ HELPER METHODS ************************************ */
	public void handle(byte[] data, SelectionKey Key) {
		if (data.toString().contains("WAKEUP")) {
			owner.wake(data);
			return;
		} else if (data.toString().contains("DIE")){
			owner.reset();
		}
		
		synchronized(dataList){
			dataList.add(data);
			dataList.notifyAll();
		}
	}
	
	// TODO make a diagnosticCrawler that crawls nodes that should be awake, if dead, add another to wakelist
	public void connectFailed(SelectionKey key) {
//		owner.
	}
	
	public void finishRead(SelectionKey key) throws IOException {	
		SocketChannel socketChannel = (SocketChannel)key.channel();
		if (!ultraList.isEmpty())
			owner.sendWork(ultraList.remove(FRONT) + ";U", socketChannel);
		else if (!leafList.isEmpty())
			owner.sendWork(leafList.remove(FRONT) + ";L", socketChannel);
	}
	
	public void finishWrite(SelectionKey key) throws IOException {
		// TODO close channel
	}
	
	/* Workers may be spawned or killed based on free memory */
	public void spawnWorker() {
		Logger logger = new Logger();
		loggerList.add(logger);
		new Thread(logger).start();
	}
	
	public void killWorker(int index) {
		loggerList.get(index).kill();
	}
	
	public int getNumWorkers() {
		return loggerList.size();
	}
	
	private void addUltrapeer(String node) {
		ultraList.add(node);
		synchronized(ultraList) {
			ultraList.notifyAll();
		}
	}
	
	private void addLeaf(String node) {
		leafList.add(node);
		synchronized(leafList) {
			leafList.notifyAll();
		}
	}
	
	public void addNodeToWake(String node) {
		wakeList.add(node);
	}
	
	public void removeWorkerNode(String address) {
		for (int i = 0; i < workingList.size(); i++) {
			if (workingList.get(i).equals(address))
				workingList.removeElementAt(i);
		}
	}
	
	public String getWork() {
		if (!wakeList.isEmpty())
			return wakeList.remove(FRONT);
		else {
			// If no nodes to wake, check up on already working node
			String worker = workingList.remove(FRONT);
			workingList.add(worker);
			return(worker);
		}
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
						addUltrapeer(addressPort);
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
							addLeaf(addressPort);
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
