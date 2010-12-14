package ca.ubc.ece.nio.crawler;

import java.util.Iterator;

// TODO Remove items from cache as size gets large
// TODO make more better

import java.util.Vector;

public class IPCache {
	private Vector<IPAddress> cache;
	
	public IPCache() {
		this.cache = new Vector<IPAddress>();
	}
	
	public IPCache(int capacity) {
		this.cache = new Vector<IPAddress>(capacity);
	}
	
	public void cache(String address) {
		IPAddress ip = new IPAddress(address.trim());
		if (!isCached(ip))
			this.cache.add(ip);
	}
	
	/* ************ HELPER METHODS ************ */
	
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
	
	private static byte[] toDomains(String address) {
		String[] stringDomains = address.split("\\.");
		byte[] domains = new byte[4];
		
		// Encode unsigned value for use with signed bytes
		int i = 0;
		for(String domain : stringDomains) {
			Integer temp = Integer.parseInt(domain);
			if (temp > 127)
				temp -= 256;
			domains[i] = (byte)temp.intValue();
			i++;
		}
		return domains;
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
			bytes = IPCache.toDomains(address);
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
