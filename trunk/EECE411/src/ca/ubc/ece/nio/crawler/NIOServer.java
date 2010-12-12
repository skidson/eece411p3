package ca.ubc.ece.nio.crawler;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class NIOServer implements Runnable {
	// Constants
	private static final int BUFFER_SIZE = 50000;
	private static final int FRONT = 0;
	
	// Run settings
	private String hostName;
	private int portNum;
	
	// Program variables
	private Selector selector;
	private ServerSocketChannel serverChannel;
	private ByteBuffer dataBuffer;
	private Map pendingData;
	private List<ChangeRequest> changeRequests;
	private DataHandler resultHandler;
	
	/* ************************************ INITIALIZATION ************************************ */
	
	public NIOServer(String hostName, int portNum, DataHandler resultHandler) {
		this.hostName = hostName;
		this.portNum = portNum;
		this.resultHandler = resultHandler;
		dataBuffer = ByteBuffer.allocate(BUFFER_SIZE);
		pendingData = new HashMap();
		changeRequests = new Vector<ChangeRequest>();
		try {
			selector = SelectorProvider.provider().openSelector();
			serverChannel = ServerSocketChannel.open();
			serverChannel.configureBlocking(false);
			serverChannel.socket().bind(new InetSocketAddress(hostName, portNum));
			serverChannel.register(selector, SelectionKey.OP_ACCEPT);
		} catch (IOException e) {}
	}
	
	public void run() {
		
	}
	
	/* ************************************ HELPER METHODS ************************************ */
	
	private void connect(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		
		try {
			socketChannel.finishConnect();
		} catch (SocketTimeoutException e) {
			// TODO set node status TIMEOUT
			key.cancel();
		} catch (UnknownHostException e) {
			// TODO set node status UNROUTABLE
			key.cancel();
		} catch (ConnectException e) {
			// TODO set node status REFUSED
			key.cancel();
		} catch (IOException e) {
			// TODO set node status INTERNAL
			key.cancel();
		}
		// TODO notifyAll on crawler
	}
	
	
	private void read(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		
		dataBuffer.clear();
		int bytesRead = 0;
		
		try {
			bytesRead = socketChannel.read(dataBuffer);
		} catch (IOException e) {
			bytesRead = -1;
		}
		
		// If channel was closed, cancel key and close this end
		if (bytesRead == -1) {
			key.cancel();
			socketChannel.close();
			return;
		}
		
		// TODO handle the data
		
		// TODO notifyAll on crawler
	}
	
	private void write(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		
		synchronized(pendingData) {
			List queue = (List) pendingData.get(socketChannel);
			
			while(!queue.isEmpty()) {
				ByteBuffer buffer = (ByteBuffer) queue.get(FRONT);
				socketChannel.write(buffer);
				
				// Check if all data has been read from this buffer
				if (buffer.remaining() > 0)
					break;
				
				queue.remove(FRONT);
			}
			
			// If no more to write, switch channel back to read
			if(queue.isEmpty())
				key.interestOps(SelectionKey.OP_READ);
		}
	}
	
	private void send(SocketChannel socket, byte[] data) {
		changeRequests.add(new ChangeRequest(socket, ChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));
		
		synchronized(pendingData) {
			List queue = (List) pendingData.get(socket);
			if (queue == null) {
				queue = new Vector();
				pendingData.put(socket, queue);
			}
			queue.add(ByteBuffer.wrap(data));
		}
		selector.wakeup();
	}
	
	/* ************************************ EMBEDDED CLASSES ************************************ */
	
	private class ChangeRequest {
		public static final int REGISTER = 1;
		public static final int CHANGEOPS = 2;
		private SocketChannel channel;
		private int type, ops;
		
		public ChangeRequest(SocketChannel channel, int type, int ops) {
			this.channel = channel;
			this.type = type;
			this.ops = ops;
		}
		
		public SocketChannel getSocketChannel() { return (channel); }
		public int getType() { return (type); }
		public int getOps() { return (ops); }
	}
}
