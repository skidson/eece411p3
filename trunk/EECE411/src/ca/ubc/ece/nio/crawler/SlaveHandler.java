package ca.ubc.ece.nio.crawler;

public class SlaveHandler implements DataHandler {
	private Slave owner;
	
	public SlaveHandler(Slave owner) {
		this.owner = owner;
	}
	
	public void handle(byte[] data) {
		// TODO pass data directly to master
		// Can call public Slave functions
	}
}
