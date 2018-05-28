package net.handler;

import client.MapleClient;
import net.MaplePacketHandler;
import tools.data.input.SeekableLittleEndianAccessor;

public final class NoOpHandler implements MaplePacketHandler {

    private static final NoOpHandler instance = new NoOpHandler();

    private NoOpHandler() {
        // singleton
    }

    public static NoOpHandler getInstance() {
        return instance;
    }

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        // no op
    }

    @Override
    public boolean validateState(MapleClient c) {
        return true;
    }
}
