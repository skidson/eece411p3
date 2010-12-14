package ca.ubc.ece.nio.crawler;

public interface CrawlerNode {
	public String getMasterAddress();
	public void wake();
}
