package net;

import tools.HexTool;

public class ByteArrayMaplePacket implements MaplePacket {

    private final byte[] data;
    private Runnable onSend;

    public ByteArrayMaplePacket(byte[] data) {
        this.data = data;
    }

    @Override
    public byte[] getBytes() {
        return data;
    }

    @Override
    public String toString() {
        return HexTool.toString(data);
    }

    @Override
    public Runnable getOnSend() {
        return onSend;
    }

    @Override
    public void setOnSend(Runnable onSend) {
        this.onSend = onSend;
    }
}
