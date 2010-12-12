package ca.ubc.ece.nio.crawler;

import java.util.Vector;

public class MasterHandler implements DataHandler {
	private Master owner;
	private Vector<byte[]> dataList;
	
	public MasterHandler(Master owner) {
		this.owner = owner;
	}
	
	public void handle(byte[] data) {
		// TODO parse data into node objects and log
		// Can call Master public methods
		// Can delegate workers to parse and log
		// IPCache can be accessed with Master.ipCache
		// Use owner.addUltrapeer(String node) and owner.addLeaf(String node) to change lists
	}
	
	private int parseData(byte[] data) {
		// TODO this needs to be cleaned up
		// Parsing will now only be done at master.
		// Requirements:
		// 1. Take in a byte[] data
		// 2. Extract ultrapeers and leaves from data
		// 3. Check each ultrapeer & leaf if cached
		//		- if not cached, add to ultraList or leafList in the form hostName:portNum (as a string)
		// 4. Cache this node's address
		// 5. Construct Node object from data[]
		// 6. Return the constructed Node object
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
}
