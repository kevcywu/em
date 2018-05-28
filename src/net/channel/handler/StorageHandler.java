package net.channel.handler;

import client.IItem;
import client.MapleClient;
import client.MapleInventoryType;
import net.AbstractMaplePacketHandler;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleStorage;
import net.packetcreator.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class StorageHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        byte mode = slea.readByte();
        final MapleStorage storage = c.getPlayer().getStorage();
        if (mode == 4) { // take out
            byte type = slea.readByte();
            byte slot = slea.readByte();
            slot = storage.getSlot(MapleInventoryType.getByType(type), slot);
            IItem item = storage.takeOut(slot);
            if (item != null) {
                if (MapleInventoryManipulator.checkSpace(c, item.getItemId(), item.getQuantity(), item.getOwner())) {
                    StringBuilder logInfo = new StringBuilder("Taken out from storage by ");
                    logInfo.append(c.getPlayer().getName());
                    MapleInventoryManipulator.addFromDrop(c, item, logInfo.toString());
                } else {
                    storage.store(item);
                    c.getSession().write(MaplePacketCreator.serverNotice(1, "Your inventory is full"));
                }
                storage.sendTakenOut(c, ii.getInventoryType(item.getItemId()));
            } else {
                return;
// AutobanManager.getInstance().autoban(c, "Trying to take out item from storage which does not exist.");
            }
        } else if (mode == 5) { // store
            byte slot = (byte) slea.readShort();
            int itemId = slea.readInt();
            short quantity = slea.readShort();
            if (quantity < 1) {
//                AutobanManager.getInstance().autoban(c, "Trying to store " + quantity + " of " + itemId);
                return;
            }
            if (storage.isFull()) {
                c.getSession().write(MaplePacketCreator.getStorageFull());
                return;
            }
            if (c.getPlayer().getMeso() < 100) {
                c.getSession().write(MaplePacketCreator.serverNotice(1, "You don't have enough mesos to store the item"));
            } else {
                MapleInventoryType type = ii.getInventoryType(itemId);
                IItem item = c.getPlayer().getInventory(type).getItem(slot).copy();
                if (item.getItemId() == itemId && (item.getQuantity() >= quantity || ii.isThrowingStar(itemId))) {
                    if (ii.isThrowingStar(itemId)) {
                        quantity = item.getQuantity();
                    }
                    StringBuilder logMsg = new StringBuilder("Stored by ");
                    logMsg.append(c.getPlayer().getName());
                    item.log(logMsg.toString(), false);
                    c.getPlayer().gainMeso(-100, true, true, true);
                    MapleInventoryManipulator.removeFromSlot(c, type, slot, quantity, false);
                    item.setQuantity(quantity);
                    storage.store(item);
                } else {
//                    AutobanManager.getInstance().addPoints(c, 1000, 0, "Trying to store non-matching itemid (" + itemId + "/" + item.getItemId() + ") or quantity not in posession (" + quantity + "/" + item.getQuantity() + ")");
                }
            }
            storage.sendStored(c, ii.getInventoryType(itemId));
        } else if (mode == 6) { // meso
            int meso = slea.readInt();
            int storageMesos = storage.getMeso();
            int playerMesos = c.getPlayer().getMeso();
            if ((meso > 0 && storageMesos >= meso) || (meso < 0 && playerMesos >= -meso)) {
                if (meso < 0 && (storageMesos - meso) < 0) { // storing with overflow
                    meso = -(Integer.MAX_VALUE - storageMesos);
                    if ((-meso) > playerMesos) { // should never happen just a failsafe
                        throw new RuntimeException("everything sucks");
                    }
                } else if (meso > 0 && (playerMesos + meso) < 0) { // taking out with overflow
                    meso = (Integer.MAX_VALUE - playerMesos);
                    if ((meso) > storageMesos) { // should never happen just a failsafe
                        throw new RuntimeException("everything sucks");
                    }
                }
                storage.setMeso(storageMesos - meso);
                c.getPlayer().gainMeso(meso, true, true, true);
            } else {
//                AutobanManager.getInstance().addPoints(c, 1000, 0, "Trying to store or take out unavailable amount of mesos (" + meso + "/" + storage.getMeso() + "/" + c.getPlayer().getMeso() + ")");
            }
            storage.sendMeso(c);
        } else if (mode == 7) { // close
            storage.close();
        }
    }

}
