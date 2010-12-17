package ca.ubc.ece.nio.crawler;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
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
import java.util.logging.Handler;

public class NIOServer implements Runnable {
	// Constants
	private static final int BUFFER_SIZE = 8192;
	private static final int FRONT = 0;

	// Run settings
	private String hostName;
	private int portNum;
	
	// Program variables
	private Selector selector;
	private ServerSocketChannel serverChannel;
	private ByteBuffer dataBuffer;
	private SocketChannel masterSocketChannel;
	private Map<SocketChannel, List<ByteBuffer>> pendingData;
	private List<ChangeRequest> changeRequests;
	private DataHandler resultHandler;
	private Vector<Crawler> crawlerList;
	private Node owner;
	
	/* ************************************ INITIALIZATION ************************************ */
	
	public NIOServer(String hostName, int portNum, DataHandler resultHandler, Node owner) {
		this.hostName = hostName;
		this.portNum = portNum;
		this.owner = owner;
		this.resultHandler = resultHandler;
		//TODO LOL FIX PL0X
		
		init();
		
		try {
			selector = SelectorProvider.provider().openSelector();
			serverChannel = ServerSocketChannel.open();
			serverChannel.configureBlocking(false);
			serverChannel.socket().bind(new InetSocketAddress(hostName, portNum));
			serverChannel.socket().setSoTimeout(owner.getTimeout());
			serverChannel.register(selector, SelectionKey.OP_ACCEPT);
//			try {
//				masterSocketChannel = createConnection(owner.getMasterAddress(), portNum, -1);
//			} catch (IOException e) {
//				 If this fails, resort to backup
//				try {
//					masterSocketChannel = createConnection(backup, portNum, 5);
//				} catch (IOException e1) { /* your fucked */ }
//			} 
		} catch (IOException e) {}
	}
	
	private void init() {
		this.crawlerList = new Vector<Crawler>();
		this.changeRequests = new Vector<ChangeRequest>();
		this.dataBuffer = ByteBuffer.allocate(BUFFER_SIZE);
		this.pendingData = new HashMap<SocketChannel, List<ByteBuffer>>();
		this.pendingData = new HashMap<SocketChannel, List<ByteBuffer>>();
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
							Attachment attachment = new Attachment();
							attachment.setIdentifier(change.getId());
							change.getSocketChannel().register(selector, change.getOps(), attachment);
							break;
						}
					}
					this.changeRequests.clear();
				}
				
				// Blocks until an event arrives at a channel
				selector.select();
				Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
				while(selectedKeys.hasNext()) {
					SelectionKey key = (SelectionKey) selectedKeys.next();
					selectedKeys.remove();
					
					if (key.isAcceptable()) {
						accept(key);
					} else if (key.isConnectable())
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
	
	private byte[] tag(Attachment attachment) {
		byte[] addData = ("\r\nAddress: " + attachment.getAddress() + 
				"\r\nPort: " + attachment.getPort() + 
				"\r\nStatus: " + attachment.getStatus().toString()).getBytes();
		return((new String(addData)).getBytes());
	}
	
	private byte[] addTag(Attachment attachment, byte[] data) {
		byte[] addData = tag(attachment);
		return((new String(data) + new String(addData)).getBytes());
	}
	
	public void addCrawler(Crawler crawler) {
		crawlerList.add(crawler);
		new Thread(crawler).start();
	}
	
	public int getNumCrawlers() {
		return crawlerList.size();
	}
	
	public int getPort() {
		return portNum;
	}
	
	public String getHostName() {
		return hostName;
	}
	
	public SocketChannel createConnection(String address, int port, int id) throws IOException{
		SocketChannel socketChannel = SocketChannel.open();
	    socketChannel.configureBlocking(false);
	  
	    // Kick off connection establishment
	    
	    socketChannel.connect(new InetSocketAddress(address, port));
	  
	    // Queue a channel registration since the caller is not the 
	    // selecting thread. As part of the registration we'll register
	    // an interest in connection events. These are raised when a channel
	    // is ready to complete connection establishment.
	    changeRequests.add(new ChangeRequest(socketChannel, ChangeRequest.REGISTER, SelectionKey.OP_CONNECT, id));
	    System.out.println("Crawler " + id + " requested connection to " + address + ":" + port);
	    selector.wakeup();
	    return socketChannel;
	}
	
	private void accept(SelectionKey key) throws IOException {
	    // For an accept to be pending the channel must be a server socket channel.
	    ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

	    // Accept the connection and make it non-blocking
	    SocketChannel socketChannel = serverSocketChannel.accept();
	    socketChannel.configureBlocking(false);
	    
	    // Register the new SocketChannel with our Selector, indicating
	    // we'd like to be notified when there's data waiting to be read
	    Attachment attachment = new Attachment();
	    attachment.setIdentifier(-1);
	    socketChannel.register(this.selector, SelectionKey.OP_READ, attachment);
	  }
	
	private void connect(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		System.out.println("Crawler " + ((Attachment)key.attachment()).getIdentifier() + " connecting..."); // debug
		Status status = Status.CONNECTED;
		try {
			socketChannel.finishConnect();
			updateAttachment(key);
			System.out.println("Crawler " + ((Attachment)key.attachment()).getIdentifier() + " successfully connected to " + ((Attachment)key.attachment()).getAddress()); // debug
		} catch (SocketTimeoutException e) {
			status = Status.TIMEOUT;
		} catch (UnknownHostException e) {
			status = Status.UNROUTABLE;
		} catch (ConnectException e) {
			status = Status.REFUSED;
		} catch (IOException e) {
			status = Status.INTERNAL;
		}
		
		Attachment attachment = (Attachment) key.attachment();
		attachment.setStatus(status);
		if (status != Status.CONNECTED) {
			System.out.println("Crawler " + ((Attachment)key.attachment()).getIdentifier() + " failed to connect (" + status.toString() + ")");
			crawlerList.get(attachment.getIdentifier()).abort();
			resultHandler.connectFailed(key);
		}
		System.out.println("Crawler " + attachment.getIdentifier() + " waking up...");
		crawlerList.get(attachment.getIdentifier()).wake();
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
		byte[] data = addTag(attachment, (dataBuffer.array()));
		byte[] dataCopy = new byte[data.length];
		System.arraycopy(data, 0, dataCopy, 0, data.length);
		
		System.out.println("Crawler " + attachment.getIdentifier() + " has read data: " + (new String(data)));
		
		resultHandler.handle(dataCopy, key);
		resultHandler.finishRead(key);
		
		System.out.println("Crawler " + attachment.getIdentifier() + " waking up!");
		
		if (attachment.getIdentifier() != -1)
			crawlerList.get(attachment.getIdentifier()).wake();
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
			resultHandler.finishWrite(key);
		}
	}
	
	public void send(SocketChannel socket, byte[] data) {
		System.out.println("Attempting to write!");
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
	
	public void sendToMaster(byte[] data){
		send(masterSocketChannel, data);
	}
	
	public String getMasterAddress() {
		return owner.getMasterAddress();
	}
	
	public void reset() {
		init();
		for (Crawler crawler : crawlerList)
			crawler.kill();
	}

	/* ************************************ EMBEDDED CLASSES ************************************ */
	private class ChangeRequest {
		public static final int REGISTER = 1;
		public static final int CHANGEOPS = 2;
		private SocketChannel channel;
		private int type, ops, id;
		
		public ChangeRequest(SocketChannel channel, int type, int ops) {
			this.channel = channel;
			this.type = type;
			this.ops = ops;
		}
		
		public ChangeRequest(SocketChannel channel, int type, int ops, int id) {
			this.channel = channel;
			this.type = type;
			this.ops = ops;
			this.id = id;
		}
		
		public SocketChannel getSocketChannel() { return (channel); }
		public int getType() { return type; }
		public int getOps() { return ops; }
		public int getId() { return id; }
	}
	
	private class Listener {
		
	}
	
}
