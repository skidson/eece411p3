package ca.ubc.ece.nio.crawler;

import java.util.Vector;

public class MasterHandler implements DataHandler {
	// Constants
	private static final int FRONT = 0;
	
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
		dataList.add(data);
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
	
	private Node parseData(byte[] data) {
		// TODO this needs to be cleaned up
		// Parsing will now only be done at master.
		// Requirements:
		// 1. Take in a byte[] data
		// 2. Extract ultrapeers and leaves from data
		// 3. Check each ultrapeer & leaf if cached
		//		- if not cached, add to ultraList or leafList in the form hostName:portNum (as a string)
		// 4. Construct Node object from data[]
		// 5. Return the constructed Node object
		int status = 0;
		String[] tempArray;
		String[] tempArray2;
		String[] readArray;
		String ipPort;
		String dataS = new String(data);
		String Peers = new String();
		String Leaves = new String();
		int startIndex;
		int endIndex;        
		startIndex = dataS.indexOf("Peers: ");
		if (!(startIndex == -1)) {
			endIndex = dataS.indexOf("\n", startIndex);
			if (!(endIndex == -1)) {
				Peers = dataS.substring(startIndex+7, endIndex);

				tempArray = Peers.split(",");
				for (int j = 0; j < tempArray.length; j++) {
					ipPort = tempArray[j];
					readArray = ipPort.split(":");
					if (!(ipCache.isCached(readArray[0].toString()))) {
						readArray[1] = readArray[1].replaceAll("(\\r|\\n)", ""); 
						portNum = Integer.parseInt(readArray[1]);
						Node tempnode = new Node(readArray[0], portNum);
						ultraList.add(tempnode);
						synchronized(ultraList){
							ultraList.notifyAll();
						}
						ipCache.cache(readArray[0]);
						dumpList.add(tempnode);
					}
				}	
			} else status = -1;
		} else status = -1;
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
						if (!(ipCache.isCached(readArray[0].toString()))) { 
							readArray[1] = readArray[1].replaceAll("(\\r|\\n)", ""); 
							int portNum2 = Integer.parseInt(readArray[1]);

							Node tempnode = new Node(readArray[0], portNum2);
							leafList.add(tempnode);
							//System.out.println(readArray[0]);
							ipCache.cache(readArray[0]);
							dumpList.add(tempnode);
						}
					}
				}
			} else {status = -1;}
		} else 
		{status = -1;}
		return status;
	}
	
	/* ************************************ EMBEDDED CLASSES ************************************ */
	private class Logger implements Runnable {
		boolean running = true;
		
		public void run() {
			while(running) {
				if (dataList.isEmpty()) {
					try {
						dataList.wait();
					} catch (InterruptedException e) { continue; }
				}
				Node node = parseData(dataList.remove(FRONT));
				owner.addNode(node);
//				owner.ipCache.cache(node.getAddress());
			}
		}
		
		public void kill() {
			this.running = false;
		}
	}
	
}
