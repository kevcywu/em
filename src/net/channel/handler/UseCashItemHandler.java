package net.channel.handler;

import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;

import client.MapleClient;
import client.MapleInventoryType;
import net.AbstractMaplePacketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.MapleInventoryManipulator;
import net.packetcreator.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;
import server.MapleItemInformationProvider;

public class UseCashItemHandler extends AbstractMaplePacketHandler {

    private static Logger log = LoggerFactory.getLogger(UseCashItemHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        @SuppressWarnings("unused")
        byte mode = slea.readByte();
        slea.readByte();
        int itemId = slea.readInt();
        int itemType = itemId / 10000;
        MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, itemId, 1, true, false);
        try {
            if (itemType == 507) {
                int megaType = itemId / 1000 % 10;
                if (megaType == 2) {
                    c.getChannelServer().getWorldInterface().broadcastMessage(null, MaplePacketCreator.serverNotice(3, c.getChannel(), c.getPlayer().getName()
                            + " : " + slea.readMapleAsciiString()).getBytes());
                }
            } else if (itemType == 539) {
                List<String> lines = new LinkedList<String>();
                for (int i = 0; i < 4; i++) {
                    lines.add(slea.readMapleAsciiString());
                }
                c.getChannelServer().getWorldInterface().broadcastMessage(null, MaplePacketCreator.getAvatarMega(c.getPlayer(), c.getChannel(), itemId, lines).getBytes());
            } else if (itemType == 512) {
                MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

                c.getPlayer().getMap().startMapEffect(ii.getMsg(itemId).replaceFirst("%s", c.getPlayer().getName()).replaceFirst("%s", slea.readMapleAsciiString()), itemId);
            }
        } catch (RemoteException e) {
            c.getChannelServer().reconnectWorld();
            log.error("REOTE TRHOW", e);
        }
    }
}
