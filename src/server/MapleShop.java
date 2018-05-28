package server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import client.IItem;
import client.Item;
import client.MapleClient;
import client.MapleInventoryType;
import database.DatabaseConnection;
import net.PacketProcessor;
import net.packetcreator.MaplePacketCreator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapleShop {

    private static final Set<Integer> rechargeableItems = new LinkedHashSet<Integer>();
    private int id;
    private int npcId;
    private List<MapleShopItem> items;

    private static final Logger log = LoggerFactory.getLogger(PacketProcessor.class);

    static {
        for (int i = 2070000; i <= 2070016; i++) {
            rechargeableItems.add(i);
        }
        rechargeableItems.remove(2070014); // doesn't exist
    }

    /**
     * Creates a new instance of MapleShop
     */
    private MapleShop(int id, int npcId) {
        this.id = id;
        this.npcId = npcId;
        items = new LinkedList<>();
    }

    public void addItem(MapleShopItem item) {
        items.add(item);
    }

    public void sendShop(MapleClient c) {
        c.getPlayer().setShop(this);
        c.announce(MaplePacketCreator.getNPCShop(getNpcId(), items));
    }

    public void buy(MapleClient c, int itemId, short quantity) {
        if (quantity <= 0) {
            return;
        }
        MapleShopItem item = findById(itemId);
        if (item != null && item.getPrice() > 0) {
            if (c.getPlayer().getMeso() > item.getPrice() * quantity) {
                if (MapleInventoryManipulator.checkSpace(c, itemId, quantity, "")) {
                    StringBuilder logInfo = new StringBuilder(c.getPlayer().getName());
                    logInfo.append(" bought ");
                    logInfo.append(quantity);
                    logInfo.append(" for ");
                    logInfo.append(item.getPrice() * quantity);
                    logInfo.append(" from shop ");
                    logInfo.append(id);
                    MapleInventoryManipulator.addById(c, itemId, quantity, logInfo.toString());
                    c.getPlayer().gainMeso(-(item.getPrice() * quantity), false);
                } else {
                    c.announce(MaplePacketCreator.serverNotice(1, "Your Inventory is full"));
                }
                c.announce(MaplePacketCreator.confirmShopTransaction((byte) 0));
            }
        }
    }

    public void sell(MapleClient c, MapleInventoryType type, byte slot, short quantity) {
        if (quantity == 0xFFFF || quantity == 0) {
            quantity = 1;
        }
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        IItem item = c.getPlayer().getInventory(type).getItem(slot);
        if (ii.isThrowingStar(item.getItemId())) {
            quantity = item.getQuantity();
        }
        if (quantity < 0) {
            return;
        }
        short iQuant = item.getQuantity();
        if (iQuant == 0xFFFF) {
            iQuant = 1;
        }
        if (quantity <= iQuant && iQuant > 0) {
            MapleInventoryManipulator.removeFromSlot(c, type, slot, quantity, false);
            double price;
            if (ii.isThrowingStar(item.getItemId())) {
                price = ii.getWholePrice(item.getItemId()) / (double) ii.getSlotMax(item.getItemId());
            } else {
                price = ii.getPrice(item.getItemId());
            }
            int recvMesos = (int) Math.max(Math.ceil(price * quantity), 0);
            if (price != -1 && recvMesos > 0) {
                c.getPlayer().gainMeso(recvMesos, true);
            }
            c.announce(MaplePacketCreator.confirmShopTransaction((byte) 0x8));
        }
    }

    public void recharge(MapleClient c, byte slot) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        IItem item = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
        if (item == null || !ii.isThrowingStar(item.getItemId())) {
            if (item != null && !ii.isThrowingStar(item.getItemId())) {
                log.warn(c.getPlayer().getName() + " is trying to recharge " + item.getItemId());
            }
            return;
        }
        short slotMax = ii.getSlotMax(item.getItemId());
        if (item.getQuantity() < 0) {
            log.warn(c.getPlayer().getName() + " is trying to recharge " + item.getItemId() + " with quantity " + item.getQuantity());
        }
        if (item.getQuantity() < slotMax) {
            // calc price ;_;
            int price = (int) Math.round(ii.getPrice(item.getItemId()) * (slotMax - item.getQuantity()));
            if (c.getPlayer().getMeso() >= price) {
                item.setQuantity(slotMax);
//                c.announce(MaplePacketCreator.updateInventorySlot(MapleInventoryType.USE, (Item) item));
                c.getPlayer().gainMeso(-price, true, true, true);
                c.announce(MaplePacketCreator.confirmShopTransaction((byte) 0x8));
            }
        }
    }

    protected MapleShopItem findById(int itemId) {
        for (MapleShopItem item : items) {
            if (item.getItemId() == itemId) {
                return item;
            }
        }
        return null;
    }

    public static MapleShop createFromDB(int id, boolean isShopId) {
        MapleShop ret = null;
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        int shopId;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            if (isShopId) {
                ps = con.prepareStatement("SELECT * FROM shops WHERE shopid = ?");
            } else {
                ps = con.prepareStatement("SELECT * FROM shops WHERE npcid = ?");
            }

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                shopId = rs.getInt("shopid");
                ret = new MapleShop(shopId, rs.getInt("npcid"));
                rs.close();
                ps.close();
            } else {
                rs.close();
                ps.close();
                return null;
            }
            ps = con.prepareStatement("SELECT * FROM "
                    + "shopitems WHERE shopid = ? ORDER BY position ASC");
            ps.setInt(1, shopId);
            rs = ps.executeQuery();
            List<Integer> recharges = new ArrayList<Integer>(rechargeableItems);
            while (rs.next()) {
                if (ii.isThrowingStar(rs.getInt("itemid"))) {
                    MapleShopItem starItem = new MapleShopItem((short) 1, rs.getInt("itemid"), rs.getInt("price"));
                    ret.addItem(starItem);
                    if (rechargeableItems.contains(starItem.getItemId())) {
                        recharges.remove(Integer.valueOf(starItem.getItemId()));
                    }
                } else {
                    ret.addItem(new MapleShopItem((short) 1000, rs.getInt("itemid"), rs.getInt("price")));
                }
            }
            for (Integer recharge : recharges) {
                ret.addItem(new MapleShopItem((short) 1000, recharge.intValue(), 0));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            log.error("Could not load shop", e);
        }

        return ret;
    }

    public int getNpcId() {
        return npcId;
    }

    public int getId() {
        return id;
    }

}
