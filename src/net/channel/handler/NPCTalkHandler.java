package net.channel.handler;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import scripting.npc.NPCScriptManager;
import server.life.MapleNPC;
import tools.data.input.SeekableLittleEndianAccessor;

public class NPCTalkHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int oid = slea.readInt();
        slea.readInt(); // dont know
        MapleNPC npc = (MapleNPC) c.getPlayer().getMap().getMapObject(oid);
        if (npc.hasShop()) {
            npc.sendShop(c);
        } else {
            NPCScriptManager.getInstance().start(c, npc.getId());
            // NPCMoreTalkHandler.npc = npc.getId();
            // 0 = next button
            // 1 = yes no
            // 2 = accept decline
            // 5 = select a link
            // c.getSession().write(MaplePacketCreator.getNPCTalk(npc.getId(), (byte) 0,
            // "Yo! I'm #p" + npc.getId() + "#, lulz! I can warp you lululululu.", "00 01"));
        }

    }
}
