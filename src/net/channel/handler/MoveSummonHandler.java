package net.channel.handler;

import java.awt.Point;
import java.util.List;

import client.MapleCharacter;
import client.MapleClient;
import server.maps.MapleSummon;
import server.movement.LifeMovementFragment;
import net.packetcreator.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.data.input.StreamUtil;

public class MoveSummonHandler extends AbstractMovementPacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int summonSkill = slea.readInt();
        Point startPos = StreamUtil.readShortPoint(slea);
        List<LifeMovementFragment> res = parseMovement(slea);

        MapleCharacter player = c.getPlayer();
        MapleSummon summon = player.getSummons().get(summonSkill);
        if (summon != null) {
            updatePosition(res, summon, 0);
            // player = ((MapleCharacter) c.getPlayer().getMap().getMapObject(30000));
            player.getMap().broadcastMessage(player, MaplePacketCreator.moveSummon(player.getId(), summonSkill, startPos, res), summon.getPosition());
        }
    }
}
