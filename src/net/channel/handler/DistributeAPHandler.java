package net.channel.handler;

import java.util.ArrayList;
import java.util.List;

import client.MapleClient;
import client.MapleStat;
import net.AbstractMaplePacketHandler;
import net.packetcreator.MaplePacketCreator;
import tools.Pair;
import tools.data.input.SeekableLittleEndianAccessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DistributeAPHandler extends AbstractMaplePacketHandler {

    private static final Logger log = LoggerFactory.getLogger(DistributeAPHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        List<Pair<MapleStat, Integer>> statupdate = new ArrayList<>(2);
        c.announce(MaplePacketCreator.updatePlayerStats(statupdate, true));
        slea.readInt(); // whatever
        int update = slea.readInt();
        if (c.getPlayer().getRemainingAp() > 0) {
            switch (update) {
                case 64: // str
                    c.getPlayer().setStr(c.getPlayer().getStr() + 1);
                    statupdate.add(new Pair<>(MapleStat.STR, c.getPlayer().getStr()));
                    break;
                case 128: // dex
                    c.getPlayer().setDex(c.getPlayer().getDex() + 1);
                    statupdate.add(new Pair<MapleStat, Integer>(MapleStat.DEX, c.getPlayer().getDex()));
                    break;
                case 256: // int
                    c.getPlayer().setInt(c.getPlayer().getInt() + 1);
                    statupdate.add(new Pair<MapleStat, Integer>(MapleStat.INT, c.getPlayer().getInt()));
                    break;
                case 512: // luk
                    c.getPlayer().setLuk(c.getPlayer().getLuk() + 1);
                    statupdate.add(new Pair<MapleStat, Integer>(MapleStat.LUK, c.getPlayer().getLuk()));
                    break;
                // case 2048: // hp
                // c.getPlayer().setHpApUsed(c.getPlayer().getHpApUsed()+1);
                // break;
                // case 8192: // mp
                // c.getPlayer().setMpApUsed(c.getPlayer().getMpApUsed()+1);
                // break;
                default: // TODO: implement hp and mp adding
                    c.announce(MaplePacketCreator.enableActions());
                    return;
            }
            c.getPlayer().setRemainingAp(c.getPlayer().getRemainingAp() - 1);
            statupdate.add(new Pair<>(MapleStat.AVAILABLEAP, c.getPlayer().getRemainingAp()));
            c.announce(MaplePacketCreator.updatePlayerStats(statupdate, true));
        } else {
            //AutobanManager.getInstance().addPoints(c, 334, 120000, "Trying to distribute AP to " + update + " that are not availables");
            log.info("[h4x] Player {} is distributing ap to {} without having any", c.getPlayer().getName(), Integer.valueOf(update));
        }
    }
}
