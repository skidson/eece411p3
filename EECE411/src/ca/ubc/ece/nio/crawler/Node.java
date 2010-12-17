package ca.ubc.ece.nio.crawler;

public interface Node extends Runnable {
	public static final Object sync = new Object();
	
	public void wake(String request);
	public void reset();
	public void kill();
	public String getMasterAddress();
	public String getBackupAddress();
	public int getTimeout();
}
