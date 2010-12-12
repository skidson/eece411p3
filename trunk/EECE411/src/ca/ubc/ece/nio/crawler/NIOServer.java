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
import java.util.Iterator;
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
	private Map<SocketChannel, List<ByteBuffer>> pendingData;
	private List<ChangeRequest> changeRequests;
	private DataHandler resultHandler;
	private Object syncA = new Integer(1);
	private Object syncB = new Integer(2);
	
	/* ************************************ INITIALIZATION ************************************ */
	
	public NIOServer(String hostName, int portNum, DataHandler resultHandler) {
		this.hostName = hostName;
		this.portNum = portNum;
		this.resultHandler = resultHandler;
		dataBuffer = ByteBuffer.allocate(BUFFER_SIZE);
		pendingData = new HashMap<SocketChannel, List<ByteBuffer>>();
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
		while(true) {
			try {
				synchronized(changeRequests) {
					Iterator<ChangeRequest> changes = changeRequests.iterator();
					while (changes.hasNext()) {
						ChangeRequest change = (ChangeRequest) changes.next();
						switch(change.getType()) {
						case ChangeRequest.CHANGEOPS:
							SelectionKey key = change.getSocketChannel().keyFor(selector);
							key.interestOps(change.getOps());
							break;
						case ChangeRequest.REGISTER:
							change.getSocketChannel().register(selector, change.getOps());
							break;
						}
					}
				}
				
				// Blocks until an event arrives at a channel
				selector.select();
				Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
				while(selectedKeys.hasNext()) {
					SelectionKey key = (SelectionKey) selectedKeys.next();
					selectedKeys.remove();
					
					if (key.isConnectable())
						connect(key);
					else if(key.isReadable())
						read(key);
					else if(key.isWritable())
						write(key);
				}
			} catch (IOException e) {}
		}
	}
	
	/* ************************************ HELPER METHODS ************************************ */
	
	private void updateAttachment(SelectionKey key) {
		Attachment attachment = (Attachment) key.attachment();
		String address = ((SocketChannel)key.channel()).socket().getInetAddress().getHostAddress();
		int port = ((SocketChannel)key.channel()).socket().getPort();
		attachment.setAddress(address);
		attachment.setPort(port);
	}
	
	private void wakeCrawler(int identifier) {
		if(identifier == (Integer)syncA){
			synchronized(syncA){
				syncA.notifyAll();
			}
		} else {
			synchronized(syncB){
				syncB.notifyAll();
			}
		}
	}
	
	private SocketChannel createConnection(String address, int port) throws IOException{
		SocketChannel socketChannel = SocketChannel.open();
	    socketChannel.configureBlocking(false);
	  
	    // Kick off connection establishment
	    socketChannel.connect(new InetSocketAddress(address, port));
	  
	    // Queue a channel registration since the caller is not the 
	    // selecting thread. As part of the registration we'll register
	    // an interest in connection events. These are raised when a channel
	    // is ready to complete connection establishment.
	    changeRequests.add(new ChangeRequest(socketChannel, ChangeRequest.REGISTER, SelectionKey.OP_CONNECT));
	    System.out.println("created");
	    selector.wakeup();
	    return socketChannel;
	}
	
	private void connect(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		updateAttachment(key);
		Attachment attachment = (Attachment) key.attachment();
		
		Status status = Status.CONNECTED;
		try {
			socketChannel.finishConnect();
		} catch (SocketTimeoutException e) {
			// TODO set node status TIMEOUT
			status = Status.TIMEOUT;
			key.cancel();
		} catch (UnknownHostException e) {
			// TODO set node status UNROUTABLE
			status = Status.UNROUTABLE;
			key.cancel();
		} catch (ConnectException e) {
			// TODO set node status REFUSED
			status = Status.REFUSED;
			key.cancel();
		} catch (IOException e) {
			// TODO set node status INTERNAL
			status = Status.INTERNAL;
			key.cancel();
		}
		
		if (status != Status.CONNECTED) {
			byte[] failData = ("Address: " + attachment.getAddress() + 
					"\r\nPort: " + attachment.getPort() + 
					"\r\nStatus: " + status.toString()).getBytes();
			resultHandler.handle(failData);
			return;
		}
		
		// TODO JEFF WOULD THIS SHIT WORK!?!!11!!?
		
		wakeCrawler(attachment.getIdentifier());
		
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
		
		Attachment attachment = (Attachment) key.attachment();
		key.channel().close();
		byte[] addData = ("Address: " + attachment.getAddress() + 
				"\r\nPort: " + attachment.getPort() + 
				"\r\nStatus: " + attachment.getStatus().toString()).getBytes();
		byte[] data = (dataBuffer.toString() + addData.toString()).getBytes();
		resultHandler.handle(data);
		
		wakeCrawler(attachment.getIdentifier());
	}
	
	private void write(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		
		synchronized(pendingData) {
			List<ByteBuffer> queue = (List<ByteBuffer>) pendingData.get(socketChannel);
			
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
			List<ByteBuffer> queue = (List<ByteBuffer>) pendingData.get(socket);
			if (queue == null) {
				queue = new Vector<ByteBuffer>();
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
	
	private class Attachment {
		private int identifier;
		private String address;
		private int port;
		private Status status;
		
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
}
