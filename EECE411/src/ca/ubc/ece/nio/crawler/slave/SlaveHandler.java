package ca.ubc.ece.nio.crawler.slave;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Vector;

import ca.ubc.ece.nio.crawler.Attachment;
import ca.ubc.ece.nio.crawler.DataHandler;
import ca.ubc.ece.nio.crawler.master.SliceCrawler;

public class SlaveHandler implements DataHandler {
	// Constants
	private static final int FRONT = 0;
	
	private Slave owner;
	private Vector<byte[]> dataList;
	private Vector<Relayer> workerList;
	private Vector<String> ultraList, leafList;
	public Object workSync;
	
	/* ************************************ INITIALIZATION ************************************ */
	public SlaveHandler(Slave owner) {
		this.owner = owner;
		this.dataList = new Vector<byte[]>();
		this.workerList = new Vector<Relayer>();
		this.ultraList = new Vector<String>();
		this.leafList = new Vector<String>();
		this.workSync = new Object();
		
		// testing
		ultraList.add("reala.ece.ubc.ca:5627");
	}
	
	/* ************************************ HELPER METHODS ************************************ */
	public void handle(byte[] data, SelectionKey key) {
		// TODO pass data directly to master
		// Can call public Slave functions
		// Address: a.b.c.d \r\n
		// Port: #### \r\n
		String request = new String(data);
		if (request.contains(SliceCrawler.WAKE_REQUEST)) {
			owner.wake(data);
			return;
		}
		
		SocketChannel socketChannel = (SocketChannel) key.channel();

		if(socketChannel.socket().getInetAddress().equals(owner.getMasterAddress())){
			String[] node = new String(data).split(";");
			if(node[1].equals("U"))
				addUltrapeer(node[0]);
			else
				addLeaf(node[0]);
		} else {
			synchronized(dataList) {
				dataList.add(data);
				dataList.notifyAll();
			}
		}
	}
	
	public void connectFailed(SelectionKey key) {
		Attachment attachment = (Attachment) key.attachment();
		byte[] failData = ("Address: " + attachment.getAddress() + 
				"\r\nPort: " + attachment.getPort() + 
				"\r\nStatus: " + attachment.getStatus().toString()).getBytes();
		handle(failData, key);
		key.cancel();
	}
	
	public void finishRead(SelectionKey key) throws IOException {
		key.cancel();
	}
	
	public void finishWrite(SelectionKey key) {
		// TODO don't think we need anything here
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
	
	public void addUltrapeer(String node) {
		ultraList.add(node);
		synchronized(workSync) {
			workSync.notifyAll();
		}
	}
	
	public void addLeaf(String node) {
		leafList.add(node);
		synchronized(workSync) {
			workSync.notifyAll();
		}
	}
	
	public String getWork() {
		if (!ultraList.isEmpty())
			return (ultraList.remove(FRONT));
		else if (!leafList.isEmpty())
			return (leafList.remove(FRONT));
		else
			return null;
	}
	
	/* ************************************ EMBEDDED CLASSES ************************************ */
	private class Relayer implements Runnable {
		boolean running = true;
//		int count = 0;
//		byte[] toBeSent = new byte[8192];
		
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
		//TODO CAN BE BUFFERED SOME HOW 	
			//toBeSent = (new String(toBeSent)+ new String(dataList.remove(FRONT)) + "\r\n").getBytes();
			System.out.println("Sending data: " + new String(dataList.get(FRONT)));
			owner.sendToMaster(dataList.remove(FRONT));
		}

		
		public void kill() {
			this.running = false;
		}
	}
	
}
