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
		// Address: a.b.c.d \r\n
		// Port: #### \r\n
		synchronized(dataList){
			dataList.add(data);
			dataList.notifyAll();
		}
		
		synchronized(dataList) {
			dataList.notifyAll();
		}
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
	
	public int getNumWorkers() {
		return workerList.size();
	}
	
	/* ************************************ EMBEDDED CLASSES ************************************ */
	private class Relayer implements Runnable {
		boolean running = true;
		int count = 0;
		byte[] toBeSent = new byte[8192];
		
		public void run() {
			while(running) {
				if (dataList.isEmpty()) {
					try {
						synchronized(dataList){
							dataList.wait();
						}
					} catch (InterruptedException e) { continue; }
				}
			}
			
			toBeSent = (toBeSent.toString() + dataList.remove(FRONT).toString() + "\r\n").getBytes();
			count++;
			
			if (count>10){
				owner.sendToMaster(toBeSent);
				count = 0;
			}
		}
		
		public void kill() {
			this.running = false;
		}
	}
	
}
