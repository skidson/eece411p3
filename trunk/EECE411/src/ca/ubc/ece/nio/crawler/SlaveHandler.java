package ca.ubc.ece.nio.crawler;

import java.util.Vector;

public class SlaveHandler implements DataHandler {
	// Constants
	private static final int FRONT = 0;
	
	private Slave owner;
	private Vector<byte[]> dataList;
	private Vector<Relayer> workerList;
	
	/* ************************************ INITIALIZATION ************************************ */
	public SlaveHandler(Slave owner) {
		this.owner = owner;
		this.dataList = new Vector<byte[]>();
		this.workerList = new Vector<Relayer>();
	}
	
	/* ************************************ HELPER METHODS ************************************ */
	public void handle(byte[] data) {
		// TODO pass data directly to master
		// Can call public Slave functions
		// TODO need to tack address and port onto data in format (may need to be done before this stage):
		// Address: a.b.c.d \r\n
		// Port: #### \r\n
		dataList.add(data);
	}
	
	/* Workers may be spawned or killed based on free memory */
	public void killWorker(int index) {
		workerList.get(index).kill();
	}
	
	public void spawnWorker() {
		Relayer relayer = new Relayer();
		workerList.add(relayer);
		new Thread(relayer).start();
	}
	
	/* ************************************ EMBEDDED CLASSES ************************************ */
	private class Relayer implements Runnable {
		boolean running = true;
		
		public void run() {
			while(running) {
				if (dataList.isEmpty()) {
					try {
						dataList.wait();
					} catch (InterruptedException e) { continue; }
				}
			}
			// TODO NIO send data to master
		}
		
		public void kill() {
			this.running = false;
		}
	}
	
}
