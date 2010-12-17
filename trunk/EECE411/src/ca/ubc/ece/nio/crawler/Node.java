package ca.ubc.ece.nio.crawler;

public interface Node extends Runnable {
	public void wake(String request);
	public void reset();
	public void kill();
	public String getMasterAddress();
	public int getTimeout();
}
