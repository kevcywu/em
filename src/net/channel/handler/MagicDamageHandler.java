package net.channel.handler;

import java.util.List;
import client.ISkill;
import client.MapleCharacter;
import client.MapleClient;
import client.SkillFactory;
import net.MaplePacket;
import server.MapleStatEffect;
import net.packetcreator.MaplePacketCreator;
import tools.Pair;
import tools.data.input.SeekableLittleEndianAccessor;

public class MagicDamageHandler extends AbstractDealDamageHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        //attack air
        //23 00 03 01 00 00 00 00 00 90 01 04 DB 82 A9 00 FB FC D7 00
        //attack air
        //25 00 03 01 BE BC 21 00 00 2F 06 06 A1 1B 66 01 00 00 5F 00

        AttackInfo attack = parseDamage(slea, false);
        MapleCharacter player = c.getPlayer();

        MaplePacket packet = MaplePacketCreator.magicAttack(player.getId(), attack.skill, attack.stance,
                attack.numAttackedAndDamage, attack.allDamage);
        player.getMap().broadcastMessage(player, packet, false, true);
        //		MaplePacket packet = MaplePacketCreator.magicAttack(30000, attack.skill, attack.stance,
        //			attack.numAttackedAndDamage, attack.allDamage);
        //		c.getPlayer().getMap().broadcastMessage(null, packet);

        MapleStatEffect effect = attack.getAttackEffect(c.getPlayer());
        int maxdamage;
        // if (!effect.isHeal()) {
        // double magic = c.getPlayer().getTotalMagic();
        // double int_ = c.getPlayer().getTotalInt();
        // double ampMod = 1.0;
        // ISkill fpAmp = SkillFactory.getSkill(2110001);
        // ISkill ilAmp = SkillFactory.getSkill(2210001);
        // int fpAmpLevel = c.getPlayer().getSkillLevel(fpAmp);
        // int ilAmpLevel = c.getPlayer().getSkillLevel(ilAmp);
        // if (fpAmpLevel > 0) {
        // ampMod = fpAmp.getEffect(fpAmpLevel).getY() * 0.01;
        // } else if (ilAmpLevel > 0) {
        // ampMod = ilAmp.getEffect(ilAmpLevel).getY() * 0.01;
        // }
        //
        // // TODO better magic damage calculation + calculate the elemental modifier
        // double elementalMod = 1.5;
        // maxdamage = (int) ((magic * 3.3 + magic * magic * 0.003365 + int_ * 0.5) *
        // ((effect.getMatk() * ampMod) * 0.01) * elementalMod) + 10;
        // } else {
        // maxdamage = 8000;
        // }
        // TODO fix magic damage calculation
        maxdamage = 40000;

        applyAttack(attack, player, maxdamage, effect.getAttackCount());

        // MP Eater
        for (int i = 1; i <= 3; i++) {
            ISkill eaterSkill = SkillFactory.getSkill(2000000 + i * 100000);
            int eaterLevel = player.getSkillLevel(eaterSkill);
            if (eaterLevel > 0) {
                for (Pair<Integer, List<Integer>> singleDamage : attack.allDamage) {
                    eaterSkill.getEffect(eaterLevel).applyPassive(player, player.getMap().getMapObject(singleDamage.getLeft()), 0);
                }
                break;
            }
        }
    }
}
