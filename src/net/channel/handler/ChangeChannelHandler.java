package net.channel.handler;

import java.net.InetAddress;
import client.MapleClient;
import java.net.UnknownHostException;
import net.AbstractMaplePacketHandler;
import net.MaplePacket;
import net.channel.ChannelServer;
import server.MapleTrade;
import net.packetcreator.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class ChangeChannelHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int channel = slea.readByte() + 1; // there is some int after it...but...wtf?
        c.getPlayer().cancelAllBuffs();
        String ip = ChannelServer.getInstance(c.getChannel()).getIP(channel);
        // ip = "127.0.0.1:7575";
        // System.out.println("Changing channel towards " + ip);
        String[] socket = ip.split(":");
        if (c.getPlayer().getTrade() != null) {
            MapleTrade.cancelTrade(c.getPlayer());
        }
        c.getPlayer().saveToDB(true);
        c.getPlayer().getMap().removePlayer(c.getPlayer());
        ChannelServer.getInstance(c.getChannel()).removePlayer(c.getPlayer());
        c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION);
        try {
            MaplePacket packet = MaplePacketCreator.getChannelChange(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1]));
            c.announce(packet);
        } catch (NumberFormatException | UnknownHostException e) {
            c.getSession().close();
            throw new RuntimeException(e);
        }
    }

}
