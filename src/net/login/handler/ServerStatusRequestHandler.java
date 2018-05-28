package net.login.handler;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import tools.data.input.SeekableLittleEndianAccessor;
import net.packetcreator.LoginPacketCreator;

public class ServerStatusRequestHandler extends AbstractMaplePacketHandler {
    
    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        c.getSession().write(LoginPacketCreator.getServerStatus(LoginPacketCreator.ServerStatus.NORMAL));
    }
}
