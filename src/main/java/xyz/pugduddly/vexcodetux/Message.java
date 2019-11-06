package xyz.pugduddly.vexcodetux;

public class Message {
    private byte[] rx;
    private byte[] tx;
    private byte[] payload;
    private byte command;
    private byte[] internalRx;

    public Message(byte[] rx, byte[] tx) {
        this(rx, tx, rx);
    }

    public Message(byte[] rx, byte[] tx, byte[] internalRx) {
        this.rx = rx;
        this.tx = tx;
        this.internalRx = internalRx;
    }

    public Message(byte[] rx, byte[] tx, byte command, byte[] payload) {
        this.rx = rx;
        this.tx = tx;
        this.command = command;
        this.payload = payload;
    }

    public byte getCommand() {
        return this.command;
    }

    public byte[] getPayload() {
        return this.payload;
    }

    public byte[] getRX() {
        return this.rx;
    }

    public byte[] getTX() {
        return this.tx;
    }

    public byte[] getInternalRX() {
        return this.internalRx;
    }
}