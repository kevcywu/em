package net.channel.handler;

import client.MapleCharacter;
import client.MapleClient;
import net.AbstractMaplePacketHandler;
import net.channel.ChannelServer;
import server.MaplePortal;
import server.maps.MapleMap;
import net.packetcreator.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangeMapHandler extends AbstractMaplePacketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChangeMapHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.readByte(); //?
        int targetid = slea.readInt(); //FF FF FF FF
        String startwp = slea.readMapleAsciiString();
        MaplePortal portal = c.getPlayer().getMap().getPortal(startwp);

        MapleCharacter player = c.getPlayer();
        if (targetid != -1 && !c.getPlayer().isAlive()) {
            boolean executeStandardPath = true;
            if (player.getEventInstance() != null) {
                executeStandardPath = player.getEventInstance().revivePlayer(player);
            }
            if (executeStandardPath) {
                player.setHp(50);
                MapleMap to = c.getPlayer().getMap().getReturnMap();
                MaplePortal pto = to.getPortal(0);
                player.setStance(0);
                player.changeMap(to, pto);
            }
        } else if (targetid != -1 && c.getPlayer().isGM()) {
            MapleMap to = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(targetid);
            MaplePortal pto = to.getPortal(0);
            player.changeMap(to, pto);
        } else if (targetid != -1 && !c.getPlayer().isGM()) {
            log.warn("Player {} attempted Mapjumping without being a gm", c.getPlayer().getName());
        } else {
            if (portal != null) {
                portal.enterPortal(c);
            } else {
                c.getSession().write(MaplePacketCreator.enableActions());
                log.warn("Portal {} not found on map {}", startwp, c.getPlayer().getMap().getId());
            }
        }
    }

}
