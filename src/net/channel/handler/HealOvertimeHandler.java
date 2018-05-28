package net.channel.handler;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import tools.data.input.SeekableLittleEndianAccessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HealOvertimeHandler extends AbstractMaplePacketHandler {

    private static final Logger log = LoggerFactory.getLogger(HealOvertimeHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.readByte();
        slea.readShort();
        slea.readByte();
        int healHP = slea.readShort();
        if (healHP != 0) {
            c.getPlayer().addHP(healHP);
        }
        int healMP = slea.readShort();
        if (healMP != 0) {
            if (healMP > 250) {
                log.warn("[h4x] Player {} is regenerating too many MP: {} (Max MP: {})", new Object[]{c.getPlayer().getName(), healMP, c.getPlayer().getMaxMp()});
            }
            c.getPlayer().addMP(healMP);
        }
    }
}
