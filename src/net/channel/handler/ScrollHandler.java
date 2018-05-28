package net.channel.handler;

import client.IEquip;
import client.IItem;
import client.InventoryException;
import client.Item;
import client.MapleClient;
import client.MapleInventory;
import client.MapleInventoryType;
import client.IEquip.ScrollResult;
import net.AbstractMaplePacketHandler;
import server.MapleItemInformationProvider;
import net.packetcreator.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScrollHandler extends AbstractMaplePacketHandler {

    private static final Logger log = LoggerFactory.getLogger(ScrollHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.readInt(); // whatever...
        byte slot = (byte) slea.readShort();
        byte dst = (byte) slea.readShort();
        byte ws = (byte) slea.readShort();
        boolean whiteScroll = false; // white scroll being used?
        boolean legendarySpirit = false; // legendary spirit skill

        if ((ws & 2) == 2) {
            whiteScroll = true;
        }

        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        IEquip toScroll;
        if (dst < 0) {
            toScroll = (IEquip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(dst);
        } else {
            // legendary spirit
            legendarySpirit = true;
            toScroll = (IEquip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(dst);
        }
        byte oldLevel = toScroll.getLevel();
        if (((IEquip) toScroll).getUpgradeSlots() < 1) {
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            return;
        }
        MapleInventory useInventory = c.getPlayer().getInventory(MapleInventoryType.USE);
        IItem scroll = useInventory.getItem(slot);
        IItem wscroll = null;

        if (whiteScroll) {
            wscroll = useInventory.findById(2340000);
            if (wscroll == null || wscroll.getItemId() != 2340000) {
                whiteScroll = false;
                log.info("[h4x] Player {} is trying to scroll with non existant white scroll", new Object[]{c.getPlayer().getName()});
            }
        }

        if (!ii.canScroll(scroll.getItemId(), toScroll.getItemId())) {
            log.info("[h4x] Player {} is trying to scroll {} with {} which should not work", new Object[]{
                c.getPlayer().getName(), toScroll.getItemId(), scroll.getItemId()});
            return;
        }
        if (scroll.getQuantity() <= 0) {
            throw new InventoryException("<= 0 quantity when scrolling");
        }
        IEquip scrolled = (IEquip) ii.scrollEquipWithId(toScroll, scroll.getItemId(), whiteScroll);
        ScrollResult scrollSuccess = IEquip.ScrollResult.FAIL; // fail
        if (scrolled == null) {
            scrollSuccess = IEquip.ScrollResult.CURSE;
        } else if (scrolled.getLevel() > oldLevel) {
            scrollSuccess = IEquip.ScrollResult.SUCCESS;
        }
        useInventory.removeItem(scroll.getPosition(), (short) 1, false);
        if (whiteScroll) {
            useInventory.removeItem(wscroll.getPosition(), (short) 1, false);
            if (wscroll.getQuantity() < 1) {
                c.getSession().write(MaplePacketCreator.clearInventoryItem(MapleInventoryType.USE, wscroll.getPosition(), false));
            } else {
                c.getSession().write(MaplePacketCreator.updateInventorySlot(MapleInventoryType.USE, (Item) wscroll));
            }
        }
        if (scrollSuccess == IEquip.ScrollResult.CURSE) {
            c.getSession().write(MaplePacketCreator.scrolledItem(scroll, toScroll, true));

            if (dst < 0) {
                c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).removeItem(toScroll.getPosition());
            } else {
                c.getPlayer().getInventory(MapleInventoryType.EQUIP).removeItem(toScroll.getPosition());
            }
        } else {
            c.getSession().write(MaplePacketCreator.scrolledItem(scroll, scrolled, false));
        }
        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getScrollEffect(c.getPlayer().getId(), scrollSuccess, legendarySpirit));

        // equipped item was scrolled and changed
        if (dst < 0 && (scrollSuccess == IEquip.ScrollResult.SUCCESS || scrollSuccess == IEquip.ScrollResult.CURSE)) {
            c.getPlayer().equipChanged();
        }
    }
}
