package ca.ubc.ece.nio.crawler;

public interface DataHandler {
	public void handle(byte[] data);
	public void killWorker(int index);
	public void spawnWorker();
}
