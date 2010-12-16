package ca.ubc.ece.nio.crawler;

public interface Node {
	public String getMasterAddress();
	public void wake(byte[] data);
	public void reset();
	public void kill();
	public int getTimeout();
}
