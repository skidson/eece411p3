package ca.ubc.ece.nio.crawler;

public interface Crawler extends Runnable {
	public void run();
	public void abort();
	public void wake();
	public void kill();
}
