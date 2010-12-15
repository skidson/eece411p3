package ca.ubc.ece.nio.crawler;

public interface CrawlerNode {
	public String getMasterAddress();
	public void wake(byte[] data);
	public void reset();
	public void kill();
}
