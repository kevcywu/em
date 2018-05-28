package net.channel.handler;

import java.rmi.RemoteException;
import client.MapleCharacter;
import client.MapleClient;
import net.AbstractMaplePacketHandler;
import net.channel.ChannelServer;
import net.packetcreator.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class WhisperHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        byte mode = slea.readByte();
        if (mode == 6) { // whisper
            // System.out.println("in whisper handler");
            String recipient = slea.readMapleAsciiString();
            String text = slea.readMapleAsciiString();

            MapleCharacter player = c.getChannelServer().getPlayerStorage().getCharacterByName(recipient);
            if (player != null) {
                player.getClient().getSession().write(MaplePacketCreator.getWhisper(c.getPlayer().getName(), c.getChannel(), text));
                c.getSession().write(MaplePacketCreator.getWhisperReply(recipient, (byte) 1));
            } else { // not found
                try {
                    if (ChannelServer.getInstance(c.getChannel()).getWorldInterface().isConnected(recipient)) {
                        ChannelServer.getInstance(c.getChannel()).getWorldInterface().whisper(
                                c.getPlayer().getName(), recipient, c.getChannel(), text);
                        c.getSession().write(MaplePacketCreator.getWhisperReply(recipient, (byte) 1));
                    } else {
                        c.getSession().write(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
                    }
                } catch (RemoteException e) {
                    c.getSession().write(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
                    c.getChannelServer().reconnectWorld();
                }
            }
        } else if (mode == 5) { // - /find
            String recipient = slea.readMapleAsciiString();
            MapleCharacter player = c.getChannelServer().getPlayerStorage().getCharacterByName(recipient);
            if (player != null && (c.getPlayer().isGM() || !player.isHidden())) {
                c.getSession().write(MaplePacketCreator.getFindReplyWithMap(recipient, player.getMap().getId()));
            } else { // not found
                try {
                    int channel = ChannelServer.getInstance(c.getChannel()).getWorldInterface().find(recipient);
                    if (channel > -1) {
                        c.getSession().write(MaplePacketCreator.getFindReply(recipient, channel));
                    } else {
                        c.getSession().write(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
                    }
                } catch (RemoteException e) {
                    c.getChannelServer().reconnectWorld();
                }
            }
        }
    }
}
