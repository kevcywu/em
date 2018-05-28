package net.channel.handler;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import scripting.npc.NPCConversationManager;
import scripting.npc.NPCScriptManager;
import tools.data.input.SeekableLittleEndianAccessor;

public class NPCMoreTalkHandler extends AbstractMaplePacketHandler {

    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        byte lastMsg = slea.readByte(); // 00 (last msg type I think)

        byte action = slea.readByte(); // 00 = end chat, 01 == follow
        //if (action == 1) {

        if (lastMsg == 3) {
            String returnText = slea.readMapleAsciiString();
            NPCConversationManager cm = NPCScriptManager.getInstance().getCM(c);
            cm.setGetText(returnText);
            NPCScriptManager.getInstance().action(c, action, lastMsg, (byte) -1);
        } else {
            byte selection = -1;
            if (slea.available() > 0) {
                selection = slea.readByte();
            }
            NPCScriptManager.getInstance().action(c, action, lastMsg, selection);
        }
        //}
        /*System.out.println("moretalk action: " + action);
	if (talkStatus == 0) {
	c.getSession().write(MaplePacketCreator.getNPCTalk(npc, (byte) 5, 
	"CHOOOOOSE! BOHAHAHAHAHAH ;))\r\n#L0##m60000##l\r\n#L1##m221000300##l"));
	talkStatus = 1;
	} else if (talkStatus == 1) {
	c.getSession().write(MaplePacketCreator.getNPCTalk(npc, (byte) 2, 
	"Here is your debug info: " + slea.readByte()));
	talkStatus = 0;
	}*/
    }
}
