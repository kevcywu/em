package net.channel.handler;

import client.MapleClient;
import client.messages.CommandProcessor;
import net.AbstractMaplePacketHandler;
import tools.MaplePacketCreator;
import tools.StringUtil;
import tools.data.input.SeekableLittleEndianAccessor;

public class GeneralchatHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        String text = slea.readMapleAsciiString();

        if (!CommandProcessor.getInstance().processCommand(c, text)) {
            if (StringUtil.countCharacters(text, '@') > 4 || StringUtil.countCharacters(text, '%') > 4
                    || StringUtil.countCharacters(text, '+') > 6 || StringUtil.countCharacters(text, '$') > 6
                    || StringUtil.countCharacters(text, '&') > 6 || StringUtil.countCharacters(text, '~') > 6
                    || StringUtil.countCharacters(text, 'W') > 6) {
                text = "DISREGARD THAT I SUCK COCK";
            }
            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getChatText(c.getPlayer().getId(), text));
        }
    }
}
