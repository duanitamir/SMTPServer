package de.tu_berlin.cit;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class SMTPServer {
		
		public final static int CONNECTED = 0;
		public final static int RECEIVEDWELCOME = 1;
		public final static int MAILFROMSENT = 2;
		public final static int RCPTTOSENT = 3;
		public final static int DATASENT = 4;
		public final static int MESSAGESENT = 5;
		public final static int QUITSENT = 6;
		public final static int HELPSENT = 7;

		@SuppressWarnings("unused")
		public static void main(String[] args) throws IOException {

			// Selector: multiplexor of SelectableChannel objects
			Selector selector = Selector.open(); // selector is open here

			// ServerSocketChannel: selectable channel for stream-oriented listening sockets
			ServerSocketChannel socket = ServerSocketChannel.open();
			InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 3200);

			// Binds the channel's socket to a local address and configures the socket to listen for connections
			socket.bind(addr);

			// Adjusts this channel's blocking mode.
			socket.configureBlocking(false);

			int ops = socket.validOps();
			SelectionKey selectKy = socket.register(selector, ops, null);

			// Infinite loop..
			// Keep server running
			while (true) {

				// Selects a set of keys whose corresponding channels are ready for I/O operations
				selector.select();


				// token representing the registration of a SelectableChannel with a Selector
				Set<SelectionKey> keys = selector.selectedKeys();
				Iterator<SelectionKey> iterator = keys.iterator();

				while (iterator.hasNext()) {
					SelectionKey key = iterator.next();

					// Tests whether this key's channel is ready to accept a new socket connection
					if (key.isAcceptable()) {
						SocketChannel client = socket.accept();

						// Adjusts this channel's blocking mode to false
						client.configureBlocking(false);
						// Operation-set bit for read operations
						client.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, 0);
						log("Connection Accepted: " + client.getLocalAddress() + "\n");
	
					} else if (key.isReadable()) {
						SocketChannel client = (SocketChannel) key.channel();
						ByteBuffer buffer = ByteBuffer.allocate(8192);
						client.read(buffer);
						String result = new String(buffer.array()).trim();
						
						System.out.println(result);
						System.out.println("KEY "+key.attachment());
						
						if(result.contains("HELO")) {
				            key.attach(RECEIVEDWELCOME);
						} else if(result.contains("MAIL FROM")) {
							key.attach(MAILFROMSENT);
						} else if(result.contains("RCPT TO")) {
							key.attach(RCPTTOSENT);
						} else if(result.contains("DATA")) {
							key.attach(DATASENT);
						} else if(result.contains("QUIT")) {
							key.attach(QUITSENT);
						} else if(result.contains("HELP")) {
							key.attach(HELPSENT);
						}

					} else if (key.isConnectable()) {
						log("Connectable");

					} else if (key.isWritable()) {
						SocketChannel client = (SocketChannel) key.channel();
						int attachment = (int)key.attachment();
						ByteBuffer buffer = null;
						switch(attachment) {
							case CONNECTED:
					            buffer = ByteBuffer.wrap(new String("220 \r\n").getBytes());
					            client.write(buffer);
							case RECEIVEDWELCOME:
					            buffer = ByteBuffer.wrap(new String("250 \r\n").getBytes());
					            client.write(buffer);
							case MAILFROMSENT:
					            buffer = ByteBuffer.wrap(new String("250 \r\n").getBytes());
					            client.write(buffer);
							case RCPTTOSENT:
					            buffer = ByteBuffer.wrap(new String("250 \r\n").getBytes());
					            client.write(buffer);
							case DATASENT:
					            buffer = ByteBuffer.wrap(new String("354 \r\n").getBytes());
					            client.write(buffer);
							case MESSAGESENT:
					            buffer = ByteBuffer.wrap(new String("250 \r\n").getBytes());
					            client.write(buffer);
							case QUITSENT:
					            buffer = ByteBuffer.wrap(new String("221 \r\n").getBytes());
					            client.write(buffer);
							case HELPSENT:
					            buffer = ByteBuffer.wrap(new String("214 \r\n").getBytes());
					            client.write(buffer);
					        default:
					        	//System.out.println(attachment);
						}
											

					} else if (key.isValid()) {
						log("Valid");

					}
					iterator.remove();
				}
			}
		}

		private static void log(String str) {
			System.out.println(str);
		}
}
