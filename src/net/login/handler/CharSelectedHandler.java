package net.login.handler;

import java.net.InetAddress;
import java.net.UnknownHostException;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import net.login.LoginServer;
import net.packetcreator.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CharSelectedHandler extends AbstractMaplePacketHandler {

    private static final Logger log = LoggerFactory.getLogger(CharSelectedHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int charId = slea.readInt();
        String macs = slea.readMapleAsciiString();
        c.updateMacs(macs);
        if (c.hasBannedMac()) {
            c.getSession().close();
            return;
        }
        try {
            if (c.getIdleTask() != null) {
                c.getIdleTask().cancel(true);
            }
            //c.getSession().write(MaplePacketCreator.getServerIP(InetAddress.getByName("127.0.0.1"), 7575, charId));
            c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION);
            String channelServerIP = MapleClient.getChannelServerIPFromSubnet(c.getSession().getRemoteAddress().toString().replace("/", "").split(":")[0], c.getChannel());
            if (channelServerIP.equals("0.0.0.0")) {
                String[] socket = LoginServer.getInstance().getIP(c.getChannel()).split(":");
                c.getSession().write(MaplePacketCreator.getServerIP(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1]), charId));
            } else {
                String[] socket = LoginServer.getInstance().getIP(c.getChannel()).split(":");
                c.getSession().write(MaplePacketCreator.getServerIP(InetAddress.getByName(channelServerIP), Integer.parseInt(socket[1]), charId));
            }
        } catch (UnknownHostException e) {
            log.error("Host not found", e);
        }
    }
}
