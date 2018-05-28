package net.channel.handler;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import net.AbstractMaplePacketHandler;
import server.maps.AnimatedMapleMapObject;
import server.movement.AbsoluteLifeMovement;
import server.movement.ChairMovement;
import server.movement.ChangeEquipSpecialAwesome;
import server.movement.LifeMovement;
import server.movement.LifeMovementFragment;
import server.movement.RelativeLifeMovement;
import server.movement.TeleportMovement;
import tools.data.input.LittleEndianAccessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMovementPacketHandler extends AbstractMaplePacketHandler {

    private static Logger log = LoggerFactory.getLogger(AbstractMovementPacketHandler.class);

    protected List<LifeMovementFragment> parseMovement(LittleEndianAccessor lea) {
        List<LifeMovementFragment> res = new ArrayList<LifeMovementFragment>();
        int numCommands = lea.readByte();
        for (int i = 0; i < numCommands; i++) {
            int command = lea.readByte();
            switch (command) {
                case 0: // normal move
                {
                    int xpos = lea.readShort();
                    int ypos = lea.readShort();
                    int xwobble = lea.readShort();
                    int ywobble = lea.readShort();
                    int unk = lea.readShort();
                    int newstate = lea.readByte();
                    int duration = lea.readShort();
                    AbsoluteLifeMovement alm = new AbsoluteLifeMovement(command, new Point(xpos, ypos), duration, newstate);
                    alm.setUnk(unk);
                    alm.setPixelsPerSecond(new Point(xwobble, ywobble));
                    // log.trace("Move to {},{} command {} wobble {},{} ? {} state {} duration {}", new Object[] { xpos,
                    // xpos, command, xwobble, ywobble, newstate, duration });
                    res.add(alm);
                    break;
                }
                case 1:
                case 2:
                case 6: // fj
                {
                    int xmod = lea.readShort();
                    int ymod = lea.readShort();
                    int newstate = lea.readByte();
                    int duration = lea.readShort();
                    RelativeLifeMovement rlm = new RelativeLifeMovement(command, new Point(xmod, ymod), duration, newstate);
                    res.add(rlm);
                    // log.trace("Relative move {},{} state {}, duration {}", new Object[] { xmod, ymod, newstate,
                    // duration });
                    break;
                }
                case 3:
                case 4: // tele... -.-
                case 7: // assaulter
                case 8: // assastinate
                case 9: // rush
                {
                    int xpos = lea.readShort();
                    int ypos = lea.readShort();
                    int xwobble = lea.readShort();
                    int ywobble = lea.readShort();
                    int newstate = lea.readByte();
                    TeleportMovement tm = new TeleportMovement(command, new Point(xpos, ypos), newstate);
                    tm.setPixelsPerSecond(new Point(xwobble, ywobble));
                    res.add(tm);
                    break;
                }
                case 10: // change equip ???
                {
                    res.add(new ChangeEquipSpecialAwesome(lea.readByte()));
                    break;
                }
                case 11: // chair
                {
                    int xpos = lea.readShort();
                    int ypos = lea.readShort();
                    int unk = lea.readShort();
                    int newstate = lea.readByte();
                    int duration = lea.readShort();
                    ChairMovement cm = new ChairMovement(command, new Point(xpos, ypos), duration, newstate);
                    cm.setUnk(unk);
                    res.add(cm);
                    break;
                }
                default: {
                    log.warn("Unhandeled movement command {} received", command);
                    return null;
                }
            }
        }
        if (numCommands != res.size()) {
            log.warn("numCommands ({}) does not match the number of deserialized movement commands ({})", numCommands, res.size());
        }
        return res;
    }

    protected void updatePosition(List<LifeMovementFragment> movement, AnimatedMapleMapObject target, int yoffset) {
        for (LifeMovementFragment move : movement) {
            if (move instanceof LifeMovement) {
                if (move instanceof AbsoluteLifeMovement) {
                    Point position = ((LifeMovement) move).getPosition();
                    position.y += yoffset;
                    target.setPosition(position);
                }
                target.setStance(((LifeMovement) move).getNewstate());
            }
        }
    }
}
