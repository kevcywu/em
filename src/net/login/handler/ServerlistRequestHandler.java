package net.login.handler;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import net.login.LoginServer;
import tools.data.input.SeekableLittleEndianAccessor;
import net.packetcreator.LoginPacketCreator;

public class ServerlistRequestHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        c.getSession().write(LoginPacketCreator.getWorldInformation(0, "Zenith", "welcome", LoginServer.getInstance().getLoad()));
        c.getSession().write(LoginPacketCreator.getEndOfWorldInformation());
    }
}
