package net.channel.handler;

import java.awt.Point;

import client.ISkill;
import client.MapleClient;
import client.SkillFactory;
import net.AbstractMaplePacketHandler;
import server.AutobanManager;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class SpecialMoveHandler extends AbstractMaplePacketHandler {
    // private static Logger log = LoggerFactory.getLogger(SpecialMoveHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        //first 2 bytes always semi randomly change
        slea.readByte();
        slea.readByte();
        @SuppressWarnings("unused")
        int unk = slea.readShort();
        @SuppressWarnings("unused")
        int skillid = slea.readInt();
        // seems to be skilllevel for movement skills and -32748 for buffs
        Point pos = null;
        @SuppressWarnings("unused")
        int __skillLevel = slea.readByte();
        if (slea.available() == 4) {
            pos = new Point(slea.readShort(), slea.readShort());
        }
        ISkill skill = SkillFactory.getSkill(skillid);
        int skillLevel = c.getPlayer().getSkillLevel(skill);

        if (skillLevel == 0) {
            AutobanManager.getInstance().addPoints(c.getPlayer().getClient(), 1000, 0, "Using a move skill he doesn't have (" + skill.getId() + ")");
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
