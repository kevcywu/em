package net.channel.handler;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import net.packetcreator.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class SkillEffectHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.readInt();
        int skillId = slea.readInt();
        slea.readByte(); // unknown at this point
        byte flags = slea.readByte();
        c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.skillEffect(c.getPlayer(), skillId, flags), false);
    }
}
