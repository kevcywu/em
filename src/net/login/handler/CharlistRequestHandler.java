package net.login.handler;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import tools.data.input.SeekableLittleEndianAccessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CharlistRequestHandler extends AbstractMaplePacketHandler {

    private static final Logger log = LoggerFactory.getLogger(CharlistRequestHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int server = slea.readByte();
        int channel = slea.readByte() + 1;
        c.setWorld(server);
        log.info("Client is connecting to server {} channel {}", server, channel);
        c.setChannel(channel);
        c.sendCharList(server);
    }
}
