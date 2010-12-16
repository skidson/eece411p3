/*	Name: node.java
 * 		- node object to store information about crawled nodes
 * 
 * 	Authors:	Stephen Kidson - #15345077
 * 				David Lo - #20123071
 * 				Jeffrey Payan - #18618074
 * 
 * 	Last updated: December 17, 2010
 */

package ca.ubc.ece.nio.crawler.master;

import java.util.Vector;

import ca.ubc.ece.nio.crawler.Status;

public class Node {
	private int portNum;
	private String address;
	private String hostName;
	private String agent;
	private Status status;
	
	/* ************************************ INITIALIZATION ************************************ */
	public Node(String address, int portNum) {
		this.address = address;
		this.portNum = portNum;
	}
	
	/* ************************************ HELPER METHODS ************************************ */
	public boolean containedIn(Vector<Node> list) {
		for (Node other : list) {
			if (this.equals(other))
				return true;
		}
		return false;
	}
	
	public boolean equals(Node other) {
		if (this.address.equals(other.getAddress()) && this.portNum == other.getPortNum())
			return true;
		return false;
	}
	
	public String toString() {
		String ret = "Address: " + this.address + "\n" +
			"Port: " + this.portNum + "\r\n" + 
			"Status: " + this.status + "\r\n";
		if (this.status == Status.CONNECTED) {
			ret += "Hostname: " + this.hostName + "\r\n" +
				"Agent: " + this.agent + "\r\n";
		}
		return ret;
	}
	
	/* ************************************ GETTERS ************************************ */
	public String getAddress() { return address; }
	public String getHostName() { return hostName; }
	public int getPortNum() { return portNum; }
	public Status getStatus() { return this.status; }
	public String getAgent() { return agent; }
	
	public String getStatusMessage() {
		String message = "Unknown";
		switch(status) {
		case CONNECTED:
			message = "Connected"; break;
		case TIMEOUT:
			message = "Timed Out"; break;
		case UNROUTABLE:
			message = "Unroutable IP"; break;
		case REFUSED:
			message = "Connection Refused"; break;
		case INTERNAL:
			message = "Internal Error"; break;
		}
		return message;
	}
	
	/* ************************************ SETTERS ************************************ */
	public void setAddress(String address) {
		this.address = address;
	}

	public void setAgent(String agent) {
		this.agent = agent;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public void setPortNum(int portNum) {
		this.portNum = portNum;
	}
	
	public void setStatus(Status status) {
		this.status = status;
	}
	
}
