package de.tu_berlin.cit;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;


public class SMTPServer {
    private static Charset messageCharset = StandardCharsets.US_ASCII;

    private static byte[] newLine = " \r\n".getBytes(messageCharset);
    private static byte[] HELP = "214".getBytes(messageCharset);
    private static byte[] SERVICE_READY = "220".getBytes(messageCharset);
    private static byte[] CLOSE_CHANNEL = "221".getBytes(messageCharset);
    private static byte[] OK = "250".getBytes(messageCharset);
    private static byte[] MAIL_INPUT = "354".getBytes(messageCharset);

    public static byte[] getMessageStatus(de.tu_berlin.cit.SMTPServerState state) {
        switch (state.getState()){
            case SMTPServerState.CONNECTED:
                return SERVICE_READY;
            case SMTPServerState.HELO_RECEIVED:
            case SMTPServerState.MAIL_FROM_RECEIVED:
            case SMTPServerState.RECEPT_OR_ECEIVED:
            case SMTPServerState.MESSAGE_RECEIVED:
                return OK;
            case SMTPServerState.DATA_RECEIVED:
                return MAIL_INPUT;
            case SMTPServerState.QUIT_RECEIVED:
                return CLOSE_CHANNEL;
            case SMTPServerState.HELP_RECEIVED:
                return HELP;
            default:
                throw new IllegalArgumentException(state.getState() + " is not a legal Input");
        }
    }

    /**
     * Get Port from args or get Default Port
     * @param args Just the arguments from CLI
     * @return port
     */
    public static int getPort(String[] args) {
        int DEFAULT_PORT = 8080;

        if(args.length == 0) {
            return DEFAULT_PORT;
        }

        try {
            return Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            return DEFAULT_PORT;
        }
    }

    public static void updateState(SMTPServerState state, Integer newState) {
        state.setPreviousState(state.getState());
        state.setState(newState);
    }

    public static String messageDecoder(ByteBuffer message) throws CharacterCodingException {
        CharsetDecoder decoder = messageCharset.newDecoder();
        return decoder.decode(message).toString();
    }

    /**
     * Initializing the server
     * @param port The Port to connect to
     * @return new server instance
     * @throws IOException
     */
    public static Selector initServer(Integer port)  throws IOException {
        Selector selector = Selector.open();

        ServerSocketChannel serverSocket = ServerSocketChannel.open();
        serverSocket.configureBlocking(false);

        serverSocket.socket().bind(new InetSocketAddress("localhost", port));

        // Notification about event OP_ACCEPT requested
        // OP_ACCEPT: new connection established by client (server)
        serverSocket.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("Welcome to the Server, server is running on " + serverSocket.getLocalAddress());

        return selector;
    }

    /**
     * Establishing connection to client
     * @param selector
     * @param key
     * @throws IOException
     */
    public static void acceptConnection(Selector selector, SelectionKey key) throws IOException {
        // set State
        SMTPServerState state = new SMTPServerState();
        state.setState(SMTPServerState.CONNECTED);

        // why do we cast here?
        ServerSocketChannel socket = (ServerSocketChannel) key.channel();
        SocketChannel client = socket.accept();
        client.configureBlocking(false);

        // Operation-set bit for write data and acceptance, send state
        client.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_WRITE | SelectionKey.OP_READ, state);

        System.out.println("Huston we have a new connection in the house 🔥 " + client.getLocalAddress());
    }

    public static void readMessage(Selector selector, SelectionKey key) throws IOException {
        // get state from the received message
        SMTPServerState state = (SMTPServerState) key.attachment();
        ByteBuffer buffer = state.getByteBuffer();

        SocketChannel socketChannel = (SocketChannel) key.channel();

        buffer.clear();
        socketChannel.read(buffer);
        buffer.flip();

        String clientResponse = messageDecoder(buffer);
        String clientMessageCode = clientResponse.substring(0, 4);
        System.out.print(clientResponse);

        switch (clientMessageCode) {
            case "HELO":
                updateState(state, SMTPServerState.HELO_RECEIVED);
                break;
            case "MAIL":
                updateState(state, SMTPServerState.MAIL_FROM_RECEIVED);
                break;
            case "RCPT":
                updateState(state, SMTPServerState.RECEPT_OR_ECEIVED);
                break;
            case "DATA":
                updateState(state, SMTPServerState.DATA_RECEIVED);
                break;
            case "QUIT":
                updateState(state, SMTPServerState.QUIT_RECEIVED);
                return;
            case "HELP":
                // if help is not requested already set
                if(state.getState() != SMTPServerState.HELP_RECEIVED) {
                    updateState(state, SMTPServerState.HELP_RECEIVED);
                }
                break;
            default:
                updateState(state, SMTPServerState.MESSAGE_RECEIVED);
        }

        // switch roles
        socketChannel.register(selector, SelectionKey.OP_WRITE, state);
    }

    public static void writeMessage(Selector selector, SelectionKey key) throws IOException {
        // get state from the received message
        SMTPServerState state = (SMTPServerState) key.attachment();
        ByteBuffer buffer = state.getByteBuffer();

        SocketChannel socketChannel = (SocketChannel) key.channel();

        byte[] messageStatus = getMessageStatus(state);

        buffer.clear();
        buffer.put(messageStatus);
        buffer.put(newLine);
        buffer.flip();
        socketChannel.write(buffer);

        buffer.clear();

        // change role
        socketChannel.register(selector, SelectionKey.OP_READ, state);
    }

    public static void main(String[] args) throws IOException {
        int port = getPort(args);
        Selector selector = initServer(port);

        // Infinite loop keep the server running
        while(true) {

            if(selector.select() == 0) {
                continue;
            }

            // token represent registration of SelectableChannel with selector P.24
            Set<SelectionKey> selectionKeySet = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectionKeySet.iterator();

            while(iter.hasNext()) {
                SelectionKey key = iter.next();

                // new connection accepted(server)
                if(key.isAcceptable()) {
                    acceptConnection(selector, key);
                }

                if(key.isReadable()) {
                    readMessage(selector, key);
                }

                if(key.isWritable()) {
                    writeMessage(selector, key);
                }

                // cleanup
                iter.remove();
            }

        }
    }
}
