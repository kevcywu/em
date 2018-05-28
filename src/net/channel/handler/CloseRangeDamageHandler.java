package net.channel.handler;

import client.ISkill;
import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleStat;
import client.SkillFactory;
import net.MaplePacket;
import server.MapleStatEffect;
import net.packetcreator.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class CloseRangeDamageHandler extends AbstractDealDamageHandler {

    private boolean isFinisher(int skillId) {
        return skillId >= 1111003 && skillId <= 1111006;
    }

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        AttackInfo attack = parseDamage(slea, false);
        MapleCharacter player = c.getPlayer();

        MaplePacket packet = MaplePacketCreator.closeRangeAttack(player.getId(), attack.skill, attack.stance, attack.numAttackedAndDamage, attack.allDamage);
        player.getMap().broadcastMessage(player, packet, false, true);
        // MaplePacket packet = MaplePacketCreator.closeRangeAttack(30000, attack.skill, attack.stance,
        // attack.numAttackedAndDamage, attack.allDamage);
        // player.getMap().broadcastMessage(player, packet, true);

        // handle combo orbconsume
        int numFinisherOrbs = 0;
        Integer comboBuff = player.getBuffedValue(MapleBuffStat.COMBO);
        if (isFinisher(attack.skill)) {
            if (comboBuff != null) {
                numFinisherOrbs = comboBuff - 1;
            }
            player.handleOrbconsume();
        } else if (attack.numAttacked > 0 && comboBuff != null) {
            // handle combo orbgain
            if (attack.skill != 1111008) { // shout should not give orbs
                player.handleOrbgain();
            }
        }

        // handle sacrifice hp loss
        if (attack.numAttacked > 0 && attack.skill == 1311005) {
            int totDamageToOneMonster = attack.allDamage.get(0).getRight().get(0).intValue(); // sacrifice attacks only 1 mob with 1 attack
            player.setHp(player.getHp() - totDamageToOneMonster * attack.getAttackEffect(player).getX() / 100);
            player.updateSingleStat(MapleStat.HP, player.getHp());
        }

        // handle charged blow
        if (attack.numAttacked > 0 && attack.skill == 1211002) {
            boolean advcharge_prob = false;
            int advcharge_level = player.getSkillLevel(SkillFactory.getSkill(1220010));
            if (advcharge_level > 0) {
                MapleStatEffect advcharge_effect = SkillFactory.getSkill(1220010).getEffect(advcharge_level);
                advcharge_prob = advcharge_effect.makeChanceResult();
            } else {
                advcharge_prob = false;
            }
            if (!advcharge_prob) {
                player.cancelEffectFromBuffStat(MapleBuffStat.WK_CHARGE);
            }
        }

        int maxdamage = c.getPlayer().getCurrentMaxBaseDamage();
        int attackCount = 1;
        if (attack.skill != 0) {
            MapleStatEffect effect = attack.getAttackEffect(c.getPlayer());
            attackCount = effect.getAttackCount();
            maxdamage *= effect.getDamage() / 100.0;
            maxdamage *= attackCount;
        }
        maxdamage = Math.min(maxdamage, 99999);
        if (attack.skill == 4211006) {
            maxdamage = 700000;
        } else if (numFinisherOrbs > 0) {
            maxdamage *= numFinisherOrbs;
        } else if (comboBuff != null) {
            ISkill combo = SkillFactory.getSkill(1111002);
            int comboLevel = player.getSkillLevel(combo);
            MapleStatEffect comboEffect = combo.getEffect(comboLevel);
            double comboMod = 1.0 + (comboEffect.getDamage() / 100.0 - 1.0) * (comboBuff.intValue() - 1);
            maxdamage *= comboMod;
        }
        if (numFinisherOrbs == 0 && isFinisher(attack.skill)) {
            return; // can only happen when lagging o.o
        }
        if (isFinisher(attack.skill)) {
            maxdamage = 99999; // FIXME reenable damage calculation for finishers
        }
        applyAttack(attack, player, maxdamage, attackCount);
    }
}
