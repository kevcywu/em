package net.channel.handler;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import client.ISkill;
import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleJob;
import client.SkillFactory;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import net.AbstractMaplePacketHandler;
import server.MapleStatEffect;
import server.TimerManager;
import server.life.Element;
import server.life.ElementalEffectiveness;
import server.life.MapleMonster;
import server.maps.MapleMap;
import server.maps.MapleMapItem;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import net.packetcreator.MaplePacketCreator;
import tools.Pair;
import tools.data.input.LittleEndianAccessor;

public abstract class AbstractDealDamageHandler extends AbstractMaplePacketHandler {
    // private static Logger log = LoggerFactory.getLogger(AbstractDealDamageHandler.class);

    protected static class AttackInfo {

        public int numAttacked, numDamage, numAttackedAndDamage;
        public int skill, stance, direction;
        public List<Pair<Integer, List<Integer>>> allDamage;

        private MapleStatEffect getAttackEffect(MapleCharacter chr, ISkill theSkill) {
            ISkill mySkill = theSkill;
            if (mySkill == null) {
                mySkill = SkillFactory.getSkill(skill);
            }
            int skillLevel = chr.getSkillLevel(mySkill);
            if (skillLevel == 0) {
                return null;
            }
            return mySkill.getEffect(skillLevel);
        }

        public MapleStatEffect getAttackEffect(MapleCharacter chr) {
            return getAttackEffect(chr, null);
        }
    }

    protected void applyAttack(AttackInfo attack, MapleCharacter player, int maxDamagePerMonster, int attackCount) {

        ISkill theSkill = null;
        MapleStatEffect attackEffect = null;
        if (attack.skill != 0) {
            theSkill = SkillFactory.getSkill(attack.skill);
            attackEffect = attack.getAttackEffect(player, theSkill);
            if (attackEffect == null) {
//                AutobanManager.getInstance().autoban(player.getClient(),
//                        "Using a skill he doesn't have (" + attack.skill + ")");
            }
            if (attack.skill != 2301002) {
                // heal is both an attack and a special move (healing)
                // so we'll let the whole applying magic live in the special move part
                if (player.isAlive()) {
                    attackEffect.applyTo(player);
                } else {
                    player.getClient().getSession().write(MaplePacketCreator.enableActions());
                }
            }
        }
        if (!player.isAlive()) {
            return;
        }
        // meso explosion has a variable bullet count
        if (attackCount != attack.numDamage && attack.skill != 4211006) {
        }
        int totDamage = 0;
        MapleMap map = player.getMap();

        if (attack.skill == 4211006) { // meso explosion
            for (Pair<Integer, List<Integer>> oned : attack.allDamage) {
                MapleMapObject mapobject = map.getMapObject(oned.getLeft().intValue());

                if (mapobject != null && mapobject.getType() == MapleMapObjectType.ITEM) {
                    MapleMapItem mapitem = (MapleMapItem) mapobject;
                    if (mapitem.getMeso() > 0) {
                        synchronized (mapitem) {
                            if (mapitem.isPickedUp()) {
                                return;
                            }
                            map.removeMapObject(mapitem);
                            map.broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 4, 0), mapitem.getPosition());
                            mapitem.setPickedUp(true);
                        }
                    } else if (mapitem.getMeso() == 0) {
                        return;
                    }
                } else if (mapobject != null && mapobject.getType() != MapleMapObjectType.MONSTER) {
                    return; // etc explosion, exploding nonexistant things, etc.
                }
            }
        }

        for (Pair<Integer, List<Integer>> oned : attack.allDamage) {
            MapleMonster monster = map.getMonsterByOid(oned.getLeft().intValue());

            if (monster != null) {
                int totDamageToOneMonster = 0;
                for (Integer eachd : oned.getRight()) {
                    totDamageToOneMonster += eachd.intValue();
                }
                totDamage += totDamageToOneMonster;

                Point playerPos = player.getPosition();

                checkHighDamage(player, monster, attack, theSkill, attackEffect, totDamageToOneMonster, maxDamagePerMonster);
                double distance = playerPos.distanceSq(monster.getPosition());

                if (!monster.isControllerHasAggro()) {
                    if (monster.getController() == player) {
                        monster.setControllerHasAggro(true);
                    } else {
                        monster.switchController(player, true);
                    }
                }
                // only ds, sb, assaulter, normal (does it work for thieves, bs, or assasinate?)
                if ((attack.skill == 4001334 || attack.skill == 4201005 || attack.skill == 0 || attack.skill == 4211002 || attack.skill == 4211004)
                        && player.getBuffedValue(MapleBuffStat.PICKPOCKET) != null) {
                    handlePickPocket(player, monster, oned);
                }
                if (attack.skill == 4101005) { // drain
                    ISkill drain = SkillFactory.getSkill(4101005);
                    int gainhp = (int) ((double) totDamageToOneMonster
                            * (double) drain.getEffect(player.getSkillLevel(drain)).getX() / 100.0);
                    gainhp = Math.min(monster.getMaxHp(), Math.min(gainhp, player.getMaxHp() / 2));
                    player.addHP(gainhp);
                }

                if (player.getJob().isA(MapleJob.WHITEKNIGHT)) {
                    int[] charges = new int[]{1211005, 1211006};
                    for (int charge : charges) {
                        ISkill chargeSkill = SkillFactory.getSkill(charge);

                        if (player.isBuffFrom(MapleBuffStat.WK_CHARGE, chargeSkill)) {
                            final ElementalEffectiveness iceEffectiveness = monster.getEffectiveness(Element.ICE);
                            if (totDamageToOneMonster > 0 && iceEffectiveness == ElementalEffectiveness.NORMAL || iceEffectiveness == ElementalEffectiveness.WEAK) {
                                MapleStatEffect chargeEffect = chargeSkill.getEffect(player.getSkillLevel(chargeSkill));
                                MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.FREEZE, 1), chargeSkill, false);
                                monster.applyStatus(player, monsterStatusEffect, false, chargeEffect.getY() * 2000);
                            }
                            break;
                        }
                    }
                }

                if (totDamageToOneMonster > 0 && attackEffect != null && attackEffect.getMonsterStati().size() > 0) {
                    if (attackEffect.makeChanceResult()) {
                        MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(attackEffect.getMonsterStati(), theSkill, false);
                        monster.applyStatus(player, monsterStatusEffect, attackEffect.isPoison(), attackEffect.getDuration());
                    }
                }
                map.damageMonster(player, monster, totDamageToOneMonster);
            }
        }
        if (totDamage > 1) {
            final int offenseLimit;
            if (attack.skill != 3121004) {
                offenseLimit = 100;
            } else {
                offenseLimit = 300;
            }
        }
    }

    private void handlePickPocket(MapleCharacter player, MapleMonster monster, Pair<Integer, List<Integer>> oned) {
        ISkill pickpocket = SkillFactory.getSkill(4211003);
        int delay = 0;
        int maxmeso = player.getBuffedValue(MapleBuffStat.PICKPOCKET).intValue();
        int reqdamage = 20000;
        Point monsterPosition = monster.getPosition();

        for (Integer eachd : oned.getRight()) {
            if (pickpocket.getEffect(player.getSkillLevel(pickpocket)).makeChanceResult()) {
                double perc = (double) eachd / (double) reqdamage;

                final int todrop = Math.min((int) Math.max(perc * (double) maxmeso, (double) 1),
                        maxmeso);
                final MapleMap tdmap = player.getMap();
                final Point tdpos = new Point((int) (monsterPosition.getX() + (Math.random() * 100) - 50),
                        (int) (monsterPosition.getY()));
                final MapleMonster tdmob = monster;
                final MapleCharacter tdchar = player;

                TimerManager.getInstance().schedule(new Runnable() {
                    public void run() {
                        tdmap.spawnMesoDrop(todrop, todrop, tdpos, tdmob, tdchar, false);
                    }
                }, delay);

                delay += 200;
            }
        }
    }

    private void checkHighDamage(MapleCharacter player, MapleMonster monster, AttackInfo attack, ISkill theSkill,
            MapleStatEffect attackEffect, int damageToMonster, int maximumDamageToMonster) {
        int elementalMaxDamagePerMonster;
        Element element = Element.NEUTRAL;
        if (theSkill != null) {
            element = theSkill.getElement();
        }
        if (player.getBuffedValue(MapleBuffStat.WK_CHARGE) != null) {
            int chargeSkillId = player.getBuffSource(MapleBuffStat.WK_CHARGE);
            switch (chargeSkillId) {
                case 1211003:
                case 1211004:
                    element = Element.FIRE;
                    break;
                case 1211005:
                case 1211006:
                    element = Element.ICE;
                    break;
                case 1211007:
                case 1211008:
                    element = Element.LIGHTING;
                    break;
                case 1221003:
                case 1221004:
                    element = Element.HOLY;
                    break;
            }
            ISkill chargeSkill = SkillFactory.getSkill(chargeSkillId);
            maximumDamageToMonster *= chargeSkill.getEffect(player.getSkillLevel(chargeSkill)).getDamage() / 100.0;
        }
        if (element != Element.NEUTRAL) {
            double elementalEffect;
            if (attack.skill == 3211003 || attack.skill == 3111003) { // inferno and blizzard
                elementalEffect = attackEffect.getX() / 200.0;
            } else {
                elementalEffect = 0.5;
            }
            switch (monster.getEffectiveness(element)) {
                case IMMUNE:
                    elementalMaxDamagePerMonster = 1;
                    break;
                case NORMAL:
                    elementalMaxDamagePerMonster = maximumDamageToMonster;
                    break;
                case WEAK:
                    elementalMaxDamagePerMonster = (int) (maximumDamageToMonster * (1.0 + elementalEffect));
                    break;
                case STRONG:
                    elementalMaxDamagePerMonster = (int) (maximumDamageToMonster * (1.0 - elementalEffect));
                    break;
                default:
                    throw new RuntimeException("Unknown enum constant");
            }
        } else {
            elementalMaxDamagePerMonster = maximumDamageToMonster;
        }
        if (damageToMonster > elementalMaxDamagePerMonster) {
            // log.info("[h4x] Player {} is doing high damage to one monster: {} (maxdamage: {}, skill:
            // {})",
            // new Object[] { player.getName(), Integer.valueOf(totDamageToOneMonster),
            // Integer.valueOf(maxDamagePerMonster), Integer.valueOf(attack.skill) });
            if (damageToMonster > elementalMaxDamagePerMonster * 3) { // * 3 until implementation of lagsafe pingchecks for buff expiration
//                AutobanManager.getInstance().autoban(player.getClient(), damageToMonster
//                        + " damage (level: " + player.getLevel() + " watk: " + player.getTotalWatk()
//                        + " skill: " + attack.skill + ", monster: " + monster.getId() + " assumed max damage: "
//                        + elementalMaxDamagePerMonster + ")");
            }
        }
    }

    public AttackInfo parseDamage(LittleEndianAccessor lea, boolean ranged) {
        AttackInfo ret = new AttackInfo();
        lea.readByte();
        ret.numAttackedAndDamage = lea.readByte();
        ret.numAttacked = (ret.numAttackedAndDamage >>> 4) & 0xF; // guess why there are no skills damaging more than 15 monsters...
        ret.numDamage = ret.numAttackedAndDamage & 0xF; // how often each single monster was attacked o.o
        ret.allDamage = new ArrayList<>();
        ret.skill = lea.readInt();
        lea.readByte(); // always 0 (?)
        ret.stance = lea.readByte();

        if (ret.skill == 4211006) {
            return parseMesoExplosion(lea, ret);
        }

        if (ranged) {
            lea.readByte();
            lea.readByte();
            lea.readByte();
            ret.direction = lea.readByte(); // contains direction on some 4th job skills
            lea.skip(7);
            // hurricane and pierce have extra 4 bytes :/
            if (ret.skill == 3121004 || ret.skill == 3221001) {
                lea.skip(4);
            }
        } else {
            lea.skip(6);
        }
        for (int i = 0; i < ret.numAttacked; i++) {
            int oid = lea.readInt();
            // System.out.println("Unk2: " + HexTool.toString(lea.read(14)));
            lea.skip(14); // seems to contain some position info o.o

            List<Integer> allDamageNumbers = new ArrayList<>();
            for (int j = 0; j < ret.numDamage; j++) {
                int damage = lea.readInt();
                // System.out.println("Damage: " + damage);
                allDamageNumbers.add(damage);
            }
            ret.allDamage.add(new Pair<>(oid, allDamageNumbers));
        }

        return ret;
    }

    public AttackInfo parseMesoExplosion(LittleEndianAccessor lea, AttackInfo ret) {

        if (ret.numAttackedAndDamage == 0) {
            lea.skip(10);

            int bullets = lea.readByte();
            for (int j = 0; j < bullets; j++) {
                int mesoid = lea.readInt();
                lea.skip(1);
                ret.allDamage.add(new Pair<Integer, List<Integer>>(Integer.valueOf(mesoid), null));
            }
            return ret;

        } else {
            lea.skip(6);
        }

        for (int i = 0; i < ret.numAttacked + 1; i++) {

            int oid = lea.readInt();

            if (i < ret.numAttacked) {
                lea.skip(12);
                int bullets = lea.readByte();

                List<Integer> allDamageNumbers = new ArrayList<Integer>();
                for (int j = 0; j < bullets; j++) {
                    int damage = lea.readInt();
                    // System.out.println("Damage: " + damage);
                    allDamageNumbers.add(Integer.valueOf(damage));
                }
                ret.allDamage.add(new Pair<Integer, List<Integer>>(Integer.valueOf(oid), allDamageNumbers));

            } else {

                int bullets = lea.readByte();
                for (int j = 0; j < bullets; j++) {
                    int mesoid = lea.readInt();
                    lea.skip(1);
                    ret.allDamage.add(new Pair<Integer, List<Integer>>(Integer.valueOf(mesoid), null));
                }
            }
        }

        return ret;
    }
}
