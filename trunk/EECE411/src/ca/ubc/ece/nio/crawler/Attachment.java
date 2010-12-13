package ca.ubc.ece.nio.crawler;

public class Attachment {
	private int identifier = 0;
	private String address = "";
	private int port = 0;
	private Status status = Status.REFUSED;
	
	public Attachment() {}
	
	public Attachment(int identifier, String address, int port, Status status) {
		this.identifier = identifier;
		this.address = address;
		this.port = port;
		this.status = status;
	}

	public void setIdentifier(int identifier) { this.identifier = identifier; }
	public void setAddress(String address) { this.address = address; }
	public void setPort(int port) { this.port = port; }
	public void setStatus(Status status) { this.status = status; }
	
	public int getIdentifier() { return identifier; }
	public String getAddress() { return address; }
	public int getPort() { return port; }
	public Status getStatus() { return status; }
}