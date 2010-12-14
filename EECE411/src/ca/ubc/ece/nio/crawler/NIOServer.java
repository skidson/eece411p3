package ca.ubc.ece.nio.crawler;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
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
	private Vector<String> ultraList, leafList;
	private DataHandler resultHandler;
	private Vector<Crawler> crawlerList;
	private CrawlerNode owner;
	
	/* ************************************ INITIALIZATION ************************************ */
	
	public NIOServer(String hostName, int portNum, DataHandler resultHandler, CrawlerNode owner) {
		this.hostName = hostName;
		this.portNum = portNum;
		this.owner = owner;
		this.resultHandler = resultHandler;
		init();
		
		try {
			selector = SelectorProvider.provider().openSelector();
			serverChannel = ServerSocketChannel.open();
			serverChannel.configureBlocking(false);
			serverChannel.socket().bind(new InetSocketAddress(hostName, portNum));
			serverChannel.register(selector, SelectionKey.OP_ACCEPT);
			masterSocketChannel = createConnection(InetAddress.getLocalHost().getHostName(), 9090, 5);
		} catch (IOException e) {}
	}
	
	private void init() {
		this.ultraList = new Vector<String>();
		this.leafList = new Vector<String>();
		this.changeRequests = new Vector<ChangeRequest>();
		this.dataBuffer = ByteBuffer.allocate(BUFFER_SIZE);
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
							change.getSocketChannel().register(selector, change.getOps(), attachment);
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
	
	private byte[] tag(Attachment attachment) {
		byte[] addData = ("Address: " + attachment.getAddress() + 
				"\r\nPort: " + attachment.getPort() + 
				"\r\nStatus: " + attachment.getStatus().toString()).getBytes();
		return((dataBuffer.toString() + addData.toString()).getBytes());
	}
	
	private byte[] addTag(Attachment attachment, byte[] data) {
		byte[] addData = tag(attachment);
		return((data.toString()+ dataBuffer.toString() + addData.toString()).getBytes());
	}
	
	public void addCrawler(Crawler crawler) {
		crawlerList.add(crawler);
		new Thread(crawler).start();
	}
	
	public int getNumCrawlers() {
		return crawlerList.size();
	}
	
	private SocketChannel createConnection(String address, int port, int id) throws IOException{
		SocketChannel socketChannel = SocketChannel.open();
	    socketChannel.configureBlocking(false);
	  
	    // Kick off connection establishment
	    socketChannel.connect(new InetSocketAddress(address, port));
	  
	    // Queue a channel registration since the caller is not the 
	    // selecting thread. As part of the registration we'll register
	    // an interest in connection events. These are raised when a channel
	    // is ready to complete connection establishment.
	    changeRequests.add(new ChangeRequest(socketChannel, ChangeRequest.REGISTER, SelectionKey.OP_CONNECT, id));
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
			status = Status.TIMEOUT;
		} catch (UnknownHostException e) {
			status = Status.UNROUTABLE;
		} catch (ConnectException e) {
			status = Status.REFUSED;
		} catch (IOException e) {
			status = Status.INTERNAL;
		}
		
		attachment.setStatus(status);
		
		if (status != Status.CONNECTED) {
			crawlerList.get(attachment.getIdentifier()).abort();
			resultHandler.connectFailed(key);
			key.cancel();
			return;
		}
		
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
		byte[] data = addTag(attachment, (dataBuffer.toString().getBytes()));
		resultHandler.handle(data);
		resultHandler.finishRead(key);
		
		crawlerList.get(attachment.getIdentifier()).wake(); // potential abort problems here
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
	
	public void addUltrapeer(String node) {
		ultraList.add(node);
	}
	
	public void addLeaf(String node) {
		leafList.add(node);
	}
	
	public String getWork() {
		if (!ultraList.isEmpty())
			return ultraList.remove(FRONT);
		else if (!leafList.isEmpty())
			return leafList.remove(FRONT);
		else
			return null;
	}
	public void sendToMaster(byte[] data){
		send(masterSocketChannel, data);
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
	
}
