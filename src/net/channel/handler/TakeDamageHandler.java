package net.channel.handler;

import java.util.ArrayList;
import java.util.List;

import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleStat;
import net.AbstractMaplePacketHandler;
import server.life.MapleMonster;
import net.packetcreator.MaplePacketCreator;
import tools.Pair;
import tools.data.input.SeekableLittleEndianAccessor;

public class TakeDamageHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.readInt();
        int damagefrom = slea.readByte();
        slea.readByte();
        int damage = slea.readInt();
        int oid = 0;
        int monsteridfrom = 0;

        if (damagefrom != -2) {
            monsteridfrom = slea.readInt();
            oid = slea.readInt();
        }

        MapleCharacter player = c.getPlayer();

        if (damage < 0 || damage > 60000) {
            //AutobanManager.getInstance().addPoints(c, 1000, 60000, "Taking abnormal amounts of damge from " + monsteridfrom + ": " + damage);
            return;
        }

        if (damage > 0 && !player.isHidden()) {
            if (damagefrom == -1) {
                Integer pguard = player.getBuffedValue(MapleBuffStat.POWERGUARD);
                if (pguard != null) {
                    // why do we have to do this? -.- the client shows the damage...
                    MapleMonster attacker = (MapleMonster) player.getMap().getMapObject(oid);
                    if (attacker != null && !attacker.isBoss()) {
                        int bouncedamage = (int) (damage * (pguard.doubleValue() / 100));
                        bouncedamage = Math.min(bouncedamage, attacker.getMaxHp() / 10);
                        player.getMap().damageMonster(player, attacker, bouncedamage);
                        damage -= bouncedamage;
                        player.getMap().broadcastMessage(player, MaplePacketCreator.damageMonster(oid, bouncedamage), false, true);
                    }
                }
            }
            Integer mguard = player.getBuffedValue(MapleBuffStat.MAGIC_GUARD);
            Integer mesoguard = player.getBuffedValue(MapleBuffStat.MESOGUARD);
            if (mguard != null) {
                List<Pair<MapleStat, Integer>> stats = new ArrayList<Pair<MapleStat, Integer>>(2);
                int mploss = (int) (damage * (mguard.doubleValue() / 100.0));
                int hploss = damage - mploss;
                if (mploss > player.getMp()) {
                    hploss += mploss - player.getMp();
                    mploss = player.getMp();
                }

                player.setHp(player.getHp() - hploss);
                player.setMp(player.getMp() - mploss);
                stats.add(new Pair<>(MapleStat.HP, player.getHp()));
                stats.add(new Pair<>(MapleStat.MP, player.getMp()));
                c.getSession().write(MaplePacketCreator.updatePlayerStats(stats));
            } else if (mesoguard != null) {
                damage = (damage % 2 == 0) ? damage / 2 : (damage / 2) + 1;
                int mesoloss = (int) (damage * (mesoguard.doubleValue() / 100.0));
                if (player.getMeso() < mesoloss) {
                    player.gainMeso(-player.getMeso(), false);
                    player.cancelBuffStats(MapleBuffStat.MESOGUARD);
                } else {
                    player.gainMeso(-mesoloss, false);
                }
                player.addHP(-damage);
            } else {
                player.addHP(-damage);
            }
        }
        // player.getMap().broadcastMessage(null, MaplePacketCreator.damagePlayer(oid, 30000, damage));
        if (!player.isHidden()) {
            player.getMap().broadcastMessage(player,
                    MaplePacketCreator.damagePlayer(damagefrom, monsteridfrom, player.getId(), damage), false);
        }
    }
}
