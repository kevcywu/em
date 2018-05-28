package client.commands;

import client.IItem;
import client.Item;
import client.MapleClient;
import client.MapleInventoryType;
import client.MapleJob;
import java.awt.Point;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import net.packetcreator.MaplePacketCreator;
import server.MapleItemInformationProvider;
import server.MaplePortal;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;

public class CommandProcessor {

    static final HashMap<String, Integer> COMMON_MAP = new HashMap<>();

    static {
        COMMON_MAP.put("gmmap", 180000000);
        COMMON_MAP.put("southperry", 60000);
        COMMON_MAP.put("amherst", 1010000);
        COMMON_MAP.put("henesys", 100000000);
        COMMON_MAP.put("ellinia", 101000000);
        COMMON_MAP.put("perion", 102000000);
        COMMON_MAP.put("kerning", 103000000);
        COMMON_MAP.put("lith", 104000000);
        COMMON_MAP.put("sleepywood", 105040300);
        COMMON_MAP.put("florina", 110000000);
        COMMON_MAP.put("orbis", 200000000);
        COMMON_MAP.put("happy", 209000000);
        COMMON_MAP.put("elnath", 211000000);
        COMMON_MAP.put("ludi", 220000000);
        COMMON_MAP.put("aqua", 230000000);
        COMMON_MAP.put("leafre", 240000000);
        COMMON_MAP.put("mulung", 250000000);
        COMMON_MAP.put("herb", 251000000);
        COMMON_MAP.put("omega", 221000000);
        COMMON_MAP.put("korean", 222000000);
        COMMON_MAP.put("nlc", 600000000);
        COMMON_MAP.put("excavation", 990000000);
        COMMON_MAP.put("pianus", 230040420);
        COMMON_MAP.put("horntail", 240060200);
        COMMON_MAP.put("mushmom", 100000005);
        COMMON_MAP.put("griffey", 240020101);
        COMMON_MAP.put("manon", 240020401);
        COMMON_MAP.put("horseman", 682000001);
        COMMON_MAP.put("balrog", 105090900);
        COMMON_MAP.put("zakum", 211042300);
        COMMON_MAP.put("papu", 220080001);
        COMMON_MAP.put("showa", 801000000);
        COMMON_MAP.put("guild", 200000301);
        COMMON_MAP.put("shrine", 800000000);
        COMMON_MAP.put("skelegon", 240040511);
        COMMON_MAP.put("hpq", 100000200);
        COMMON_MAP.put("ht", 240050400);
        COMMON_MAP.put("fm", 910000000);
    }

//A; B; C; D; E; F; G; H; I; J; K; L; M; N; O; P; Q; R; S; T; U; V; W; X; Y; Z; 
    public static void executeCommand(MapleClient c, String[] sp) {
        switch (sp[0]) {
            case "!cleardrops":
                clearDropsCommand(c);
                break;
            case "!dispose":
                disposeCommand(c);
                break;
            case "!drop":
                dropCommand(c, sp);
                break;
            case "!goto":
                gotoCommand(c, sp);
                break;
            case "!job":
                jobCommand(c, sp);
                break;
            case "!killall":
                killAllCommand(c, sp);
                break;
            case "!pos":
                posCommand(c);
                break;
            case "!spawn":
                spawnCommand(c, sp);
                break;
            default:

        }
    }

    private static void clearDropsCommand(MapleClient c) {
        MapleMap map = c.getPlayer().getMap();
        double range = Double.POSITIVE_INFINITY;
        List<MapleMapObject> items = map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(MapleMapObjectType.ITEM));
        for (MapleMapObject itemmo : items) {
            map.removeMapObject(itemmo);
            map.broadcastMessage(MaplePacketCreator.removeItemFromMap(itemmo.getObjectId(), 0, c.getPlayer().getId()));
        }
        c.getPlayer().dropMessage("You have destroyed " + items.size() + " items on the ground.");
    }

    private static void disposeCommand(MapleClient c) {
        c.announce(MaplePacketCreator.enableActions());
    }

    private static void dropCommand(MapleClient c, String[] sp) {
        if (sp.length < 2) {
            c.getPlayer().dropMessage("Syntax: !drop <itemid> <quality>");
        } else {
            try {
                int itemId = Integer.parseInt(sp[1]);
                short quantity = (sp.length > 2) ? Short.parseShort(sp[2]) : 1;
                IItem toDrop;
                if (MapleItemInformationProvider.getInstance().getInventoryType(itemId) == MapleInventoryType.EQUIP) {
                    toDrop = MapleItemInformationProvider.getInstance().getEquipById(itemId);
                } else {
                    toDrop = new Item(itemId, (byte) 0, quantity);
                }
                c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), toDrop, c.getPlayer().getPosition(), true, true);
            } catch (NumberFormatException e) {
                c.getPlayer().dropMessage("<itemid> or <quality> is invalid!");
            }
        }
    }

    private static void gotoCommand(MapleClient c, String[] sp) {
        if (sp.length < 2) {
            c.getPlayer().dropMessage("Syntax: !goto <mapid>");
        } else {
            try {
                int mapid = (COMMON_MAP.containsKey(sp[1])) ? COMMON_MAP.get(sp[1]) : Integer.parseInt(sp[1]);
                MapleMap target = c.getChannelServer().getMapFactory().getMap(mapid);
                if (target == null) {
                    c.getPlayer().dropMessage("<mapid> does not exist!");
                } else {
                    MaplePortal targetPortal = null;
                    if (sp.length == 3) {
                        try {
                            targetPortal = target.getPortal(Integer.parseInt(sp[2]));
                        } catch (IndexOutOfBoundsException | NumberFormatException ioobe) {
                            // noop, assume the gm didn't know how many portals there are
                        }
                        // noop, assume that the gm is drunk
                    }
                    if (targetPortal == null) {
                        targetPortal = target.getPortal(0);
                    }
                    c.getPlayer().changeMap(target, targetPortal);
                }
            } catch (NumberFormatException e) {
                c.getPlayer().dropMessage("<mapid> is invalid!");
            }
        }
    }

    private static void jobCommand(MapleClient c, String[] sp) {
        if (sp.length < 2) {
            c.getPlayer().dropMessage("Syntax: !job <jobid>");
        } else {
            try {
                MapleJob job = MapleJob.getById(Integer.parseInt(sp[1]));
                if (job != null) {
                    c.getPlayer().changeJob(job);
                } else {
                    c.getPlayer().dropMessage("<jobid> does not exist!");
                }
            } catch (NumberFormatException e) {
                c.getPlayer().dropMessage("<jobid> is invalid!");
            }
        }
    }

    private static void killAllCommand(MapleClient c, String[] sp) {
        MapleMap map = c.getPlayer().getMap();
        double range = Double.POSITIVE_INFINITY;
        List<MapleMapObject> monsters = map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(MapleMapObjectType.MONSTER));
        for (MapleMapObject monstermo : monsters) {
            MapleMonster monster = (MapleMonster) monstermo;
            map.killMonster(monster, c.getPlayer(), false);
        }
        c.getPlayer().dropMessage("Killed " + monsters.size() + " monsters <3");
    }

    private static void posCommand(MapleClient c) {
        Point pos = c.getPlayer().getPosition();
        c.getPlayer().dropMessage("Current map: " + c.getPlayer().getMapId() + "<x,y> = <" + pos.getX() + "," + pos.getY() + ">");
    }

    private static void spawnCommand(MapleClient c, String[] sp) {
        if (sp.length < 2) {
            c.getPlayer().dropMessage("Syntax: !spawn <mobid> <quality>");
        } else {
            try {
                int mid = Integer.parseInt(sp[1]);
                int num = (sp.length > 2) ? Integer.parseInt(sp[2]) : 1;
                if (MapleLifeFactory.getMonster(mid) == null) {
                    c.getPlayer().dropMessage("<mobid> is invalid!");
                } else {
                    for (int i = 0; i < num; i++) {
                        MapleMonster mob = MapleLifeFactory.getMonster(mid);
                        c.getPlayer().getMap().spawnMonsterOnGroudBelow(mob, c.getPlayer().getPosition());
                    }
                }
            } catch (NumberFormatException e) {
                c.getPlayer().dropMessage("<modid> or <quality> is invalid!");
            }
        }
    }

}
