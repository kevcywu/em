package net.login.handler;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import net.packetcreator.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;
import net.packetcreator.LoginPacketCreator;

public class RelogRequestHandler extends AbstractMaplePacketHandler {

    @Override
    public boolean validateState(MapleClient c) {
        return !c.isLoggedIn();
    }

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        c.getSession().write(LoginPacketCreator.getRelogResponse());
    }
}
