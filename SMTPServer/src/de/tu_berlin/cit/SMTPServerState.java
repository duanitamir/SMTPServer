package de.tu_berlin.cit;

import java.nio.ByteBuffer;

public class SMTPServerState {

    public final static int CONNECTED = 0;
    public final static int HELO_RECEIVED = 1;
    public final static int MAIL_FROM_RECEIVED = 2;
    public final static int RECEPT_OR_ECEIVED = 3;
    public final static int DATA_RECEIVED = 4;
    public final static int MESSAGE_RECEIVED = 5;
    public final static int QUIT_RECEIVED= 6;
    public final static int HELP_RECEIVED = 7;

    private int state;
    private int previousState;
    private ByteBuffer buffer;
    private byte [] from;
    private byte [] to;
    private byte [] message;

    public SMTPServerState() {
        this.state = CONNECTED;
        this.buffer = ByteBuffer.allocate(8192);
    }

    public int getState() {
        return this.state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public ByteBuffer getByteBuffer() {
        return this.buffer;
    }

    public byte[] getFrom() {
        return from;
    }

    public void setFrom(byte[] from) {
        this.from = from;
    }

    public byte[] getTo() {
        return to;
    }

    public void setTo(byte[] to) {
        this.to = to;
    }

    public byte[] getMessage() {
        return message;
    }

    public void setMessage(byte[] message) {
        this.message = message;
    }

    public int getPreviousState() {
        return previousState;
    }

    public void setPreviousState(int previousState) {
        this.previousState = previousState;
    }
}
