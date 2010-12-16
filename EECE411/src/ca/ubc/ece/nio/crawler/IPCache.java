package ca.ubc.ece.nio.crawler;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;

// TODO make more better

import java.util.Vector;

public class IPCache {
	// Constants
	private static final int FRONT = 0;
	
	// Program variables
	private Vector<IPAddress> cache;
	private int capacity;
	
	public IPCache() {
		this.cache = new Vector<IPAddress>();
	}
	
	public IPCache(int capacity) {
		this.capacity = capacity;
		this.cache = new Vector<IPAddress>();
	}
	
	/* ************ HELPER METHODS ************ */
	
	public void cache(String address) {
		IPAddress ip = new IPAddress(address.trim());
		if (!isCached(ip)) {
			if (cache.size() >= capacity)
				cache.remove(FRONT);
			this.cache.add(ip);
		}
	}
	
	public boolean isCached(String address) {
		IPAddress target = new IPAddress(address.trim());
		if (cache.isEmpty())
			return false;
		for (IPAddress ip : cache) {
			if (ip.equals(target))
				return true;
		}
		return false;
	}
	
	private boolean isCached(IPAddress address) {
		if (cache.isEmpty())
			return false;
		for (IPAddress ip : cache) {
			if (ip.equals(address))
				return true;
		}
		return false;
	}
	
	public Iterator<IPAddress> iterator() {
		return(cache.iterator());
	}
	
	public String toString() {
		String list = "";
		for (IPAddress ip : cache)
			list += ip.toString() + "\n";
		return list;
	}
	
	public void integrate(IPCache other) {
		Iterator<IPAddress> iterator = other.iterator();
		while(iterator.hasNext()) {
			IPAddress ip = iterator.next();
			if (!isCached(ip))
				cache.add(ip);
		}
	}
	
	/* ************ EMBEDDED CLASSES ************ */
	private class IPAddress {
		byte[] bytes;
		
		public IPAddress(String address) {
			try {
				bytes = InetAddress.getByName(address).getAddress();
			} catch (UnknownHostException e) {}
		}

		protected byte[] getBytes() {
			byte[] temp = new byte[4];
			for (int i = 0; i < bytes.length; i++)
				temp[i] = bytes[i];
			return temp;
		}
		
		public boolean equals(IPAddress other) {
			byte[] otherBytes = other.getBytes();
			for (int i = 0; i < bytes.length; i++) {
				if (bytes[i] != otherBytes[i])
					return false;
			}
			return true;
		}
		
		public String toString() {
			String out = "";
			for (int i = 0; i < bytes.length; i++) {
				if (bytes[i] < 0)
					out += (bytes[i] + 256);
				else
					out += bytes[i];
				if (i != bytes.length - 1)
					out += ".";
			}
			return out;	
		}
		
	}
	
}
