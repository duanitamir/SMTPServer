package com.smtp.server;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;
import java.util.Set;

public class SMTPServer {

    // Selector assigning to all registered channels  selectionKey which contains info about channels status
    // Each key holds information about who is making the request and what type of the request is.
    //each instance of Selector can monitor more socket channels and thus more connections. When something happens on the channel,
    // the selector informs the application to process the request.

    private static Selector selector = null;

    private final static int CONNECTED = 0;
    private final static int HELO = 1;
    private final static int MAIL_FROM = 2;
    private final static int RCPT_TO = 3;
    private final static int MESSAGE = 4;
    private final static int DATA = 5;
    private final static int QUIT = 6;
    private final static int HELP = 7;
    private final static int MSG_RCVED = 8;

    private static Mail mail = new Mail();

    private final static int PORT = 8000;

    public static Selector serverInit(Integer port)  throws IOException {
        // Init the selector
        selector = Selector.open();

        // Creating a socket for server and address to bind to it.
        ServerSocketChannel socket = ServerSocketChannel.open();
        socket.configureBlocking(false);    // Non blocking mode
        InetSocketAddress addr = new InetSocketAddress("localhost", port);

        socket.bind(addr);                  // Bind the server

        socket.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("Server is up on localhost 8000..");
        return selector;
    }

    public static String decodeCharset(SocketChannel client) throws IOException{
        ByteBuffer buffer = ByteBuffer.allocate(8192);

        // Filling the buffer with clients msg

        client.read(buffer);
        buffer.flip();

        // Decoding`
        Charset messageCharset = Charset.forName("US-ASCII");;
        CharsetDecoder decoder = messageCharset.newDecoder();

        CharBuffer charBuf = decoder.decode(buffer);

        return charBuf.toString();
    }

    private static void handleAccept( SelectionKey key) throws IOException {
        System.out.println("Connection Accepted...");
        ServerSocketChannel sock = (ServerSocketChannel) key.channel();
        SocketChannel client = sock.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, CONNECTED );
    }

    private static void handleRead(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();

        String data = decodeCharset(client);


        if (data.length() > 0) {
            if (data.contains(".\r\n")){
                mail.setData(data);
                key.attach(MSG_RCVED); }
            else if(data.contains("HELO")){
                key.attach(HELO);  }
            else if(data.contains("MAIL FROM")){
                String address = data.split(": ")[1];
                mail.setMailFrom(address);
                key.attach(MAIL_FROM); }
            else if(data.contains("RCPT TO")){
                String rcpt_to = data.split(": ")[1];
                mail.setRCPTTo(rcpt_to);
                key.attach(RCPT_TO); }
            else if(data.contains("DATA")){
                key.attach(DATA);  }
            else if( data.contains("HELP")){
                key.attach(HELP);  }
            else if (data.contains("QUIT")){
                mail.handleWriteFile();
                key.attach(QUIT); }
        }
        System.out.println(data);
        client.register(selector, SelectionKey.OP_WRITE, key.attachment());
    }

    private static void  handleWrite(SelectionKey key) throws IOException {

        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = null;

        int attachment = (int) key.attachment();

        switch (attachment){

            case CONNECTED: {
                buffer = ByteBuffer.wrap("220 \r\n".getBytes());
                client.write(buffer);
                break;
            }
            case HELO: case RCPT_TO: case MESSAGE:case MAIL_FROM: case MSG_RCVED:{
                buffer = ByteBuffer.wrap("250 \r\n".getBytes());
                client.write(buffer);
                break;
            }
            case DATA: {
                buffer = ByteBuffer.wrap("354 \r\n".getBytes());
                client.write(buffer);
                buffer.clear();
                break;
            }
            case QUIT: {
                buffer = ByteBuffer.wrap("221 \r\n".getBytes());
                client.write(buffer);
                buffer.clear();
                client.close();
                break;
            }
            case HELP: {
                buffer = ByteBuffer.wrap("214 \r\n".getBytes());
                client.write(buffer);
                buffer.clear();
                break;
            }
            default:
                break;

        }
        if (client.isConnected()){
            client.register(selector, SelectionKey.OP_READ, key.attachment());
        }
    }

    public static void main(String[] args) {
        try {
          Selector  selector = serverInit(PORT);

            while (true) {

                if (selector.select() == 0) {
                    continue;
                }

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();

                    if (key.isAcceptable()) {
                       handleAccept(key);
                    }
                    else if (key.isReadable()) {
                        handleRead(key);
                    }
                    else if (key.isWritable()){
                        handleWrite(key);
                    }
                }
                iter.remove();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
};

