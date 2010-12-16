package ca.ubc.ece.nio.crawler;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public interface DataHandler {
	public void handle(byte[] data, SelectionKey key);
	public void killWorker();
	public void spawnWorker();
	public int getNumWorkers();
	
	public String getWork();
	
	public void finishRead(SelectionKey key) throws IOException;
	public void finishWrite(SelectionKey key) throws IOException;
	public void connectFailed(SelectionKey key);
}
