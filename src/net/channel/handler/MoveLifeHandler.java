package net.channel.handler;

import java.awt.Point;
import java.util.List;

import client.MapleClient;
import net.MaplePacket;
import server.life.MapleMonster;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.movement.LifeMovementFragment;
import net.packetcreator.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MoveLifeHandler extends AbstractMovementPacketHandler {

    private static final Logger log = LoggerFactory.getLogger(MoveLifeHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int objectid = slea.readInt();
        short moveid = slea.readShort();
        // or is the moveid an int?

        // when someone trys to move an item/npc he gets thrown out with a class cast exception mwaha
        MapleMapObject mmo = c.getPlayer().getMap().getMapObject(objectid);
        if (mmo == null || mmo.getType() != MapleMapObjectType.MONSTER) {
            /*if (mmo != null) {
				log.warn("[dc] Player {} is trying to move something which is not a monster. It is a {}.", new Object[] {
					c.getPlayer().getName(), c.getPlayer().getMap().getMapObject(objectid).getClass().getCanonicalName() });
			}*/
            return;
        }
        MapleMonster monster = (MapleMonster) mmo;

        List<LifeMovementFragment> res = null;
        int skillByte = slea.readByte();
        int skill = slea.readInt();
        slea.readShort();
        slea.readInt(); // whatever
        int start_x = slea.readShort(); // hmm.. startpos?
        int start_y = slea.readShort(); // hmm...
        Point startPos = new Point(start_x, start_y);

        res = parseMovement(slea);

        if (monster.getController() != c.getPlayer()) {
            if (monster.isAttackedBy(c.getPlayer())) { // aggro and controller change
                monster.switchController(c.getPlayer(), true);
            } else {
                // String sCon;
                // if (monster.getController() == null) {
                // sCon = "undefined";
                // } else {
                // sCon = monster.getController().getName();
                // }
                // log.warn("[dc] Player {} is trying to move a monster he does not control on map {}. The controller is
                // {}.", new Object[] { c.getPlayer().getName(), c.getPlayer().getMapId(), sCon});
                return;
            }
        } else {
            if (skill == 255 && monster.isControllerKnowsAboutAggro() && !monster.isMobile()) {
                monster.setControllerHasAggro(false);
                monster.setControllerKnowsAboutAggro(false);
            }
        }
        boolean aggro = monster.isControllerHasAggro();
        c.getSession().write(MaplePacketCreator.moveMonsterResponse(objectid, moveid, monster.getMp(), aggro));
        if (aggro) {
            monster.setControllerKnowsAboutAggro(true);
        }

        // if (!monster.isAlive())
        // return;
        if (res != null) {
            if (slea.available() != 9) {
                log.warn("slea.available != 9 (movement parsing error)");
                return;
            }
            MaplePacket packet = MaplePacketCreator.moveMonster(skillByte, skill, objectid, startPos, res);
            c.getPlayer().getMap().broadcastMessage(c.getPlayer(), packet, monster.getPosition());
            // MaplePacket packet = MaplePacketCreator.moveMonster(200, res);
            // c.getPlayer().getMap().broadcastMessage(null, packet);
            updatePosition(res, monster, -1);
            c.getPlayer().getMap().moveMonster(monster, monster.getPosition());
        }
    }
}
