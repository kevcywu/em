package net.channel.handler;

import client.ISkill;
import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleClient;
import client.SkillFactory;
import net.AbstractMaplePacketHandler;
import net.MaplePacketHandler;
import server.maps.MapleSummon;
import net.packetcreator.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class DamageSummonHandler extends AbstractMaplePacketHandler implements MaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        // 83 00 FA FE 30 00 FF 19 00 00 00 C9 F5 90 00 00
        int skillid = slea.readInt();
        int unkByte = slea.readByte();
        int damage = slea.readInt();
        int monsterIdFrom = slea.readInt();

        ISkill skill = SkillFactory.getSkill(skillid);
        if (skill != null) {
            MapleCharacter player = c.getPlayer();
            MapleSummon summon = player.getSummons().get(skillid);

            if (summon != null) {
                summon.addHP(-damage);
                if (summon.getHP() <= 0) {
                    player.cancelEffectFromBuffStat(MapleBuffStat.PUPPET);
                }
            }
            player.getMap().broadcastMessage(player, MaplePacketCreator.damageSummon(player.getId(), skillid, damage, unkByte, monsterIdFrom), summon.getPosition());
        }
    }

}
