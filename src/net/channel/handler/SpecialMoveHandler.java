/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
                       Matthias Butz <matze@odinms.de>
                       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License version 3
    as published by the Free Software Foundation. You may not use, modify
    or distribute this program under any other version of the
    GNU Affero General Public License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package net.channel.handler;

import java.awt.Point;

import client.ISkill;
import client.MapleClient;
import client.SkillFactory;
import client.messages.ServernoticeMapleClientMessageCallback;
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
					new ServernoticeMapleClientMessageCallback(5, c).dropMessage("Please wait 5 seconds before casting Mystic Door again");
					c.getSession().write(MaplePacketCreator.enableActions());
				}
			} else {
				c.getSession().write(MaplePacketCreator.enableActions());
			}
		}
	}

}
