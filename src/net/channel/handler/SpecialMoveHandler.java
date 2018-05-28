package net.channel.handler;

import java.awt.Point;

import client.ISkill;
import client.MapleClient;
import client.SkillFactory;
import net.AbstractMaplePacketHandler;
import net.packetcreator.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class SpecialMoveHandler extends AbstractMaplePacketHandler {
    // private static Logger log = LoggerFactory.getLogger(SpecialMoveHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        //first 2 bytes always semi randomly change
        slea.readByte();
        slea.readByte();
        int unk = slea.readShort();
        int skillid = slea.readInt();
        // seems to be skilllevel for movement skills and -32748 for buffs
        Point pos = null;
        int __skillLevel = slea.readByte();
        if (slea.available() == 4) {
            pos = new Point(slea.readShort(), slea.readShort());
        }
        ISkill skill = SkillFactory.getSkill(skillid);
        int skillLevel = c.getPlayer().getSkillLevel(skill);

        if (skillLevel == 0) {
            return;
        } else {
            if (c.getPlayer().isAlive()) {
                if (skill.getId() != 2311002 || c.getPlayer().canDoor()) {
                    skill.getEffect(skillLevel).applyTo(c.getPlayer(), pos);
                } else {
//                    new ServernoticeMapleClientMessageCallback(5, c).dropMessage("Please wait 5 seconds before casting Mystic Door again");
                    c.getSession().write(MaplePacketCreator.enableActions());
                }
            } else {
                c.getSession().write(MaplePacketCreator.enableActions());
            }
        }
    }

}
