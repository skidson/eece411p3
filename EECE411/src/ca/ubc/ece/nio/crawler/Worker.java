package ca.ubc.ece.nio.crawler;

public interface Worker extends Runnable {
	public void run();
	public void kill();
}
