package net.channel.handler;

import client.ISkill;
import client.MapleClient;
import client.SkillFactory;
import net.AbstractMaplePacketHandler;
import net.MaplePacketHandler;
import server.MapleStatEffect;
import tools.data.input.SeekableLittleEndianAccessor;
import net.packetcreator.MaplePacketCreator;

public class CancelBuffHandler extends AbstractMaplePacketHandler implements MaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int sourceid = slea.readInt();
        MapleStatEffect effect;
        ISkill skill = SkillFactory.getSkill(sourceid);

        if (sourceid == 3121004 || sourceid == 3221001) { // pierce and hurricane
            c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.skillCancel(c.getPlayer(), sourceid), false);
        }

        effect = skill.getEffect(1); // hack but we don't know the level that was casted on us ï¿½.o
        c.getPlayer().cancelEffect(effect, false, -1);
    }
}
