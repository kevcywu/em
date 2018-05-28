package net.packetcreator;

import java.awt.Point;
import java.awt.Rectangle;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import client.BuddylistEntry;
import client.IEquip;
import client.IItem;
import client.ISkill;
import client.Item;
import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleInventory;
import client.MapleInventoryType;
import client.MapleKeyBinding;
import client.MapleQuestStatus;
import client.MapleStat;
import client.IEquip.ScrollResult;
import client.status.MonsterStatus;
import net.ByteArrayMaplePacket;
import net.LongValueHolder;
import net.MaplePacket;
import net.SendOpcode;
import net.channel.handler.SummonDamageHandler.SummonAttackEntry;
import net.world.MapleParty;
import net.world.MaplePartyCharacter;
import net.world.PartyOperation;
import net.world.guild.MapleGuild;
import net.world.guild.MapleGuildCharacter;
import net.world.guild.MapleGuildSummary;
import server.MapleItemInformationProvider;
import server.MaplePlayerShop;
import server.MaplePlayerShopItem;
import server.MapleShopItem;
import server.MapleTrade;
import server.life.MapleMonster;
import server.life.MapleNPC;
import server.maps.MapleMap;
import server.maps.MapleReactor;
import server.maps.SummonMovementType;
import server.movement.LifeMovementFragment;
import tools.BitTools;
import tools.HexTool;
import tools.KoreanDateUtil;
import tools.Pair;
import tools.StringUtil;
import tools.data.output.LittleEndianWriter;
import tools.data.output.MaplePacketLittleEndianWriter;

public class MaplePacketCreator {

    private static final Logger log = LoggerFactory.getLogger(MaplePacketCreator.class);

    private final static byte[] CHAR_INFO_MAGIC = new byte[]{(byte) 0xff, (byte) 0xc9, (byte) 0x9a, 0x3b};
    private final static byte[] ITEM_MAGIC = new byte[]{(byte) 0x80, 5};
    public static final List<Pair<MapleStat, Integer>> EMPTY_STATUPDATE = Collections.emptyList();

    /**
     * Sends a hello packet.
     *
     * @param mapleVersion The maple client version.
     * @param sendIv the IV used by the server for sending
     * @param recvIv the IV used by the server for receiving
     */
    public static MaplePacket getHello(short mapleVersion, byte[] sendIv, byte[] recvIv) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(16);
        mplew.writeShort(0x0D);
        mplew.writeShort(mapleVersion);
        mplew.write(new byte[]{0, 0});
        mplew.write(recvIv);
        mplew.write(sendIv);
        mplew.write(8);
        return mplew.getPacket();
    }

    /**
     * Sends a ping packet.
     *
     * @return The packet.
     */
    public static MaplePacket getPing() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(16);
        mplew.writeShort(SendOpcode.PING.getValue());
        return mplew.getPacket();
    }

    /**
     * Gets a packet telling the client the IP of the channel server.
     *
     * @param inetAddr The InetAddress of the requested channel server.
     * @param port The port the channel is on.
     * @param clientId The ID of the client.
     * @return The server IP packet.
     */
    public static MaplePacket getServerIP(InetAddress inetAddr, int port, int clientId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SERVER_IP.getValue());
        mplew.writeShort(0);
        byte[] addr = inetAddr.getAddress();
        mplew.write(addr);
        mplew.writeShort(port);
        mplew.writeInt(clientId);
        mplew.write(new byte[]{0, 0, 0, 0, 0});
        return mplew.getPacket();
    }

    /**
     * Gets a packet telling the client the IP of the new channel.
     *
     * @param inetAddr The InetAddress of the requested channel server.
     * @param port The port the channel is on.
     * @return The server IP packet.
     */
    public static MaplePacket getChannelChange(InetAddress inetAddr, int port) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CHANGE_CHANNEL.getValue());
        mplew.write(1);
        byte[] addr = inetAddr.getAddress();
        mplew.write(addr);
        mplew.writeShort(port);
        return mplew.getPacket();
    }

    /**
     * Gets a packet with a list of characters.
     *
     * @param c The MapleClient to load characters of.
     * @param serverId The ID of the server requested.
     * @return The character list packet.
     */
    public static MaplePacket getCharList(MapleClient c, int serverId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CHARLIST.getValue());
        mplew.write(0);
        List<MapleCharacter> chars = c.loadCharacters(serverId);
        mplew.write((byte) chars.size());
        chars.forEach((chr) -> {
            addCharEntry(mplew, chr);
        });
        mplew.writeInt(6);
        return mplew.getPacket();
    }

    /**
     * Adds character stats to an existing MaplePacketLittleEndianWriter.
     *
     * @param mplew The MaplePacketLittleEndianWrite instance to write the stats
     * to.
     * @param chr The character to add the stats of.
     */
    private static void addCharStats(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.writeInt(chr.getId()); // character id
        mplew.writeAsciiString(chr.getName());
        for (int x = chr.getName().length(); x < 13; x++) { // fill to maximum name length
            mplew.write(0);
        }

        mplew.write(chr.getGender()); // gender (0 = male, 1 = female)
        mplew.write(chr.getSkinColor().getId()); // skin color

        mplew.writeInt(chr.getFace()); // face
        mplew.writeInt(chr.getHair()); // hair

        mplew.writeLong(0);
        mplew.writeLong(0);
        mplew.writeLong(0);

        mplew.write(chr.getLevel()); // level

        mplew.writeShort(chr.getJob().getId()); // job
        mplew.writeShort(chr.getStr()); // str
        mplew.writeShort(chr.getDex()); // dex
        mplew.writeShort(chr.getInt()); // int
        mplew.writeShort(chr.getLuk()); // luk

        mplew.writeShort(chr.getHp()); // hp (?)
        mplew.writeShort(chr.getMaxHp()); // maxhp
        mplew.writeShort(chr.getMp()); // mp (?)
        mplew.writeShort(chr.getMaxMp()); // maxmp

        mplew.writeShort(chr.getRemainingAp()); // remaining ap
        mplew.writeShort(chr.getRemainingSp()); // remaining sp
        mplew.writeInt(chr.getExp()); // current exp
        mplew.writeShort(chr.getFame()); // fame
        mplew.writeInt(0);
        mplew.writeInt(chr.getMapId()); // current map id
        mplew.write(chr.getInitialSpawnpoint()); // spawnpoint
        mplew.writeInt(0);
    }

    /**
     * Adds the aesthetic aspects of a character to an existing
     * MaplePacketLittleEndianWriter.
     *
     * @param mplew The MaplePacketLittleEndianWrite instance to write the stats
     * to.
     * @param chr The character to add the looks of.
     * @param mega Unknown
     */
    private static void addCharLook(MaplePacketLittleEndianWriter mplew, MapleCharacter chr, boolean mega) {
        mplew.write(chr.getGender());
        mplew.write(chr.getSkinColor().getId()); // skin color

        mplew.writeInt(chr.getFace()); // face
        mplew.write(mega ? 0 : 1);
        mplew.writeInt(chr.getHair()); // hair

        MapleInventory equip = chr.getInventory(MapleInventoryType.EQUIPPED);

        Map<Byte, Integer> myEquip = new LinkedHashMap<>();
        Map<Byte, Integer> maskedEquip = new LinkedHashMap<>();
        equip.list().forEach((item) -> {
            byte pos = (byte) (item.getPosition() * -1);
            if (pos < 100 && myEquip.get(pos) == null) {
                myEquip.put(pos, item.getItemId());
            } else if (pos > 100 && pos != 111) { // don't ask. o.o

                pos -= 100;
                if (myEquip.get(pos) != null) {
                    maskedEquip.put(pos, myEquip.get(pos));
                }
                myEquip.put(pos, item.getItemId());
            } else if (myEquip.get(pos) != null) {
                maskedEquip.put(pos, item.getItemId());
            }
        });
        for (Entry<Byte, Integer> entry : myEquip.entrySet()) {
            mplew.write(entry.getKey());
            mplew.writeInt(entry.getValue());
        }
        mplew.write(0xFF); // end of visible items masked itens

        for (Entry<Byte, Integer> entry : maskedEquip.entrySet()) {
            mplew.write(entry.getKey());
            mplew.writeInt(entry.getValue());
        }
        /*
		 * for (IItem item : equip.list()) { byte pos = (byte)(item.getPosition() * -1); if (pos > 100) {
		 * mplew.write(pos - 100); mplew.writeInt(item.getItemId()); } }
         */
        // ending markers
        mplew.write(0xFF);
        IItem cWeapon = equip.getItem((byte) -111);
        mplew.writeInt((cWeapon == null) ? 0 : cWeapon.getItemId());
        mplew.writeInt((chr.getPet() == null) ? 0 : chr.getPet().getItemId());
        mplew.writeInt(0);
        mplew.writeInt(0);
    }

    /**
     * Adds an entry for a character to an existing
     * MaplePacketLittleEndianWriter.
     *
     * @param mplew The MaplePacketLittleEndianWrite instance to write the stats
     * to.
     * @param chr The character to add.
     */
    protected static void addCharEntry(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        addCharStats(mplew, chr);
        addCharLook(mplew, chr, false);
        mplew.write(1); // world rank enabled (next 4 ints are not sent if disabled)
        mplew.writeInt(chr.getRank()); // world rank
        mplew.writeInt(chr.getRankMove()); // move (negative is downwards)
        mplew.writeInt(chr.getJobRank()); // job rank
        mplew.writeInt(chr.getJobRankMove()); // move (negative is downwards)

    }

    /**
     * Adds a quest info entry for a character to an existing
     * MaplePacketLittleEndianWriter.
     *
     * @param mplew The MaplePacketLittleEndianWrite instance to write the stats
     * to.
     * @param chr The character to add quest info about.
     */
    private static void addQuestInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        List<MapleQuestStatus> started = chr.getStartedQuests();
        mplew.writeShort(started.size());
        for (MapleQuestStatus q : started) {
            mplew.writeInt(q.getQuest().getId());
        }
        List<MapleQuestStatus> completed = chr.getCompletedQuests();
        mplew.writeShort(completed.size());
        for (MapleQuestStatus q : completed) {
            mplew.writeShort(q.getQuest().getId());
            // maybe start time? no effect.
            mplew.writeInt(KoreanDateUtil.getQuestTimestamp(q.getCompletionTime()));
            // completion time - don't ask about the time format
            mplew.writeInt(KoreanDateUtil.getQuestTimestamp(q.getCompletionTime()));
            // mplew.write(HexTool.getByteArrayFromHexString("80 DF BA C5"));
            // mplew.writeInt(KoreanDateUtil.getKoreanTimestamp(System.currentTimeMillis()));
            // mplew.writeInt((int) (System.currentTimeMillis() / 1000 / 60));
            // 10/19/2006 15:00 == 29815695
            // 10/23/2006 11:00 == 29816466
            // 08/05/2008 01:00 == 29947542
            // mplew.write(HexTool.getByteArrayFromHexString("80 DF BA C5 8B F3
            // C6 01"));
            // mplew.writeInt(29816466 - (int) ((365 * 6 + 10 * 30) * 24 *
            // 8.3804347826086956521739130434783));
            // mplew.writeInt(29816466 + (int) (5 * 24 *
            // 8.3804347826086956521739130434783));
            // mplew.writeInt((int) (134774 * 24 * 8.381905));
            // mplew.writeInt((int) (134774 * 24 * 8.381905));
            // mplew.writeLong(0);
            // mplew.write(HexTool.getByteArrayFromHexString("80 DF BA C5 96 F6
            // C8 01"));
        }
    }

    /**
     * Gets character info for a character.
     *
     * @param chr The character to get info about.
     * @return The character info packet.
     */
    public static MaplePacket getCharInfo(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.WARP_TO_MAP.getValue());
        mplew.writeInt(chr.getClient().getChannel() - 1);
        mplew.write(new byte[]{1, 1, 0, 0});
        mplew.writeInt(new Random().nextInt()); // seed the maplestory rng with a random number <3
        mplew.writeInt(new Random().nextInt());
        mplew.writeInt(new Random().nextInt());
        mplew.writeLong(-1);
        addCharStats(mplew, chr);
        mplew.write(chr.getBuddylist().getCapacity()); // buddylist capacity
        mplew.writeInt(chr.getMeso()); // mesos
        mplew.write(100); // equip slots
        mplew.write(100); // use slots
        mplew.write(100); // set-up slots
        mplew.write(100); // etc slots
        mplew.write(100); // cash slots
        MapleInventory iv = chr.getInventory(MapleInventoryType.EQUIPPED);
        Collection<IItem> equippedC = iv.list();
        List<Item> equipped = new ArrayList<>(equippedC.size());
        for (IItem item : equippedC) {
            equipped.add((Item) item);
        }
        Collections.sort(equipped);
        for (Item item : equipped) {
            addItemInfo(mplew, item);
        }
        mplew.writeShort(0); // start of equip inventory
        iv = chr.getInventory(MapleInventoryType.EQUIP);
        for (IItem item : iv.list()) {
            addItemInfo(mplew, item);
        }
        mplew.write(0); // start of use inventory
        // addItemInfo(mplew, new Item(2020028, (byte) 8, (short) 1));
        iv = chr.getInventory(MapleInventoryType.USE);
        for (IItem item : iv.list()) {
            addItemInfo(mplew, item);
        }
        mplew.write(0); // start of set-up inventory

        iv = chr.getInventory(MapleInventoryType.SETUP);
        for (IItem item : iv.list()) {
            addItemInfo(mplew, item);
        }
        mplew.write(0); // start of etc inventory

        iv = chr.getInventory(MapleInventoryType.ETC);
        for (IItem item : iv.list()) {
            addItemInfo(mplew, item);
        }
        mplew.write(0); // start of cash inventory

        iv = chr.getInventory(MapleInventoryType.CASH);
        for (IItem item : iv.list()) {
            addItemInfo(mplew, item);
        }
        mplew.write(0); // start of skills

        Map<ISkill, MapleCharacter.SkillEntry> skills = chr.getSkills();
        mplew.writeShort(skills.size());
        for (Entry<ISkill, MapleCharacter.SkillEntry> skill : skills.entrySet()) {
            mplew.writeInt(skill.getKey().getId());
            mplew.writeInt(skill.getValue().skillevel);
            if (skill.getKey().isFourthJob()) {
                mplew.writeInt(skill.getValue().masterlevel);
            }
        }

        mplew.writeShort(0);
        addQuestInfo(mplew, chr);

        mplew.writeLong(0);
        for (int x = 0; x < 15; x++) {
            mplew.write(CHAR_INFO_MAGIC);
        }
        mplew.writeInt(0);
        mplew.writeLong(getTime((long) System.currentTimeMillis()));
        return mplew.getPacket();
    }

    /**
     * Gets an empty stat update.
     *
     * @return The empy stat update packet.
     */
    public static MaplePacket enableActions() {
        return updatePlayerStats(EMPTY_STATUPDATE, true);
    }

    /**
     * Gets an update for specified stats.
     *
     * @param stats The stats to update.
     * @return The stat update packet.
     */
    public static MaplePacket updatePlayerStats(List<Pair<MapleStat, Integer>> stats) {
        return updatePlayerStats(stats, false);
    }

    /**
     * Gets an update for specified stats.
     *
     * @param stats The list of stats to update.
     * @param itemReaction Result of an item reaction(?)
     * @return The stat update packet.
     */
    public static MaplePacket updatePlayerStats(List<Pair<MapleStat, Integer>> stats, boolean itemReaction) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.UPDATE_STATS.getValue());
        mplew.write((itemReaction) ? 1 : 0);
        int updateMask = 0;
        for (Pair<MapleStat, Integer> statupdate : stats) {
            updateMask |= statupdate.getLeft().getValue();
        }
        List<Pair<MapleStat, Integer>> mystats = stats;
        if (mystats.size() > 1) {
            Collections.sort(mystats, new Comparator<Pair<MapleStat, Integer>>() {
                @Override
                public int compare(Pair<MapleStat, Integer> o1, Pair<MapleStat, Integer> o2) {
                    int val1 = o1.getLeft().getValue();
                    int val2 = o2.getLeft().getValue();
                    return (val1 < val2 ? -1 : (val1 == val2 ? 0 : 1));
                }
            });
        }
        mplew.writeInt(updateMask);
        for (Pair<MapleStat, Integer> statupdate : mystats) {
            if (statupdate.getLeft().getValue() >= 1) {
                if (statupdate.getLeft().getValue() == 0x1) {
                    mplew.writeShort(statupdate.getRight().shortValue());
                } else if (statupdate.getLeft().getValue() <= 0x4) {
                    mplew.writeInt(statupdate.getRight());
                } else if (statupdate.getLeft().getValue() < 0x20) {
                    mplew.write(statupdate.getRight().shortValue());
                } else if (statupdate.getLeft().getValue() < 0xFFFF) {
                    mplew.writeShort(statupdate.getRight().shortValue());
                } else {
                    mplew.writeInt(statupdate.getRight().intValue());
                }
            }
        }
        return mplew.getPacket();
    }

    /**
     * Gets a packet telling the client to change maps.
     *
     * @param to The <code>MapleMap</code> to warp to.
     * @param spawnPoint The spawn portal number to spawn at.
     * @param chr The character warping to <code>to</code>
     * @return The map change packet.
     */
    public static MaplePacket getWarpToMap(MapleMap to, int spawnPoint, MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.WARP_TO_MAP.getValue());
        mplew.writeInt(chr.getClient().getChannel() - 1);
        mplew.writeInt(2);
        mplew.writeInt(to.getId());
        mplew.write(spawnPoint);
        mplew.writeShort(chr.getHp()); // hp (???)
        mplew.write(0);
        long questMask = 0x1ffffffffffffffL;
        mplew.writeLong(questMask);
        return mplew.getPacket();
    }

    /**
     * Gets a packet to spawn a portal.
     *
     * @param townId The ID of the town the portal goes to.
     * @param targetId The ID of the target.
     * @param pos Where to put the portal.
     * @return The portal spawn packet.
     */
    public static MaplePacket spawnPortal(int townId, int targetId, Point pos) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SPAWN_PORTAL.getValue());
        mplew.writeInt(townId);
        mplew.writeInt(targetId);
        if (pos != null) {
            mplew.writeShort(pos.x);
            mplew.writeShort(pos.y);
        }
        // System.out.println("ssp: " +
        // HexTool.toString(mplew.getPacket().getBytes()));
        return mplew.getPacket();
    }

    /**
     * Gets a packet to spawn a door.
     *
     * @param oid The door's object ID.
     * @param pos The position of the door.
     * @param town
     * @return The remove door packet.
     */
    public static MaplePacket spawnDoor(int oid, Point pos, boolean town) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SPAWN_DOOR.getValue());
        // B9 00 00 47 1E 00 00
        mplew.write(town ? 1 : 0);
        mplew.writeInt(oid);
        mplew.writeShort(pos.x);
        mplew.writeShort(pos.y);
        // System.out.println("doorspawn: " +
        // HexTool.toString(mplew.getPacket().getBytes()));
        return mplew.getPacket();
    }

    /**
     * Gets a packet to remove a door.
     *
     * @param oid The door's ID.
     * @param town
     * @return The remove door packet.
     */
    public static MaplePacket removeDoor(int oid, boolean town) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (town) {
            mplew.writeShort(SendOpcode.SPAWN_PORTAL.getValue());
            mplew.writeInt(999999999);
            mplew.writeInt(999999999);
        } else {
            mplew.writeShort(SendOpcode.REMOVE_DOOR.getValue());
            mplew.write(/*town ? 1 : */0);
            mplew.writeInt(oid);
        }
        // System.out.println("doorremove: " +
        // HexTool.toString(mplew.getPacket().getBytes()));
        return mplew.getPacket();
    }

    /**
     * Gets a packet to spawn a special map object.
     *
     * @param chr The MapleCharacter who spawned the object.
     * @param skill The skill used.
     * @param skillLevel The level of the skill used.
     * @param pos Where the object was spawned.
     * @param movementType Movement type of the object.
     * @param animated Animated spawn?
     * @return The spawn packet for the map object.
     */
    public static MaplePacket spawnSpecialMapObject(MapleCharacter chr, int skill, int skillLevel, Point pos,
            SummonMovementType movementType, boolean animated) {
        // 72 00 29 1D 02 00 FD FE 30 00 19 7D FF BA 00 04 01 00 03 01 00

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SPAWN_SPECIAL_MAPOBJECT.getValue());

        mplew.writeInt(chr.getId());
        mplew.writeInt(skill);
        mplew.write(skillLevel);
        mplew.writeShort(pos.x);
        mplew.writeShort(pos.y);
        // mplew.writeInt(oid);
        mplew.write(0); // ?

        mplew.write(0); // ?

        mplew.write(0);

        mplew.write(movementType.getValue()); // 0 = don't move, 1 = follow
        // (4th mage summons?), 2/4 =
        // only tele follow, 3 = bird
        // follow

        mplew.write(1); // 0 and the summon can't attack - but puppets don't
        // attack with 1 either ^.-

        mplew.write(animated ? 0 : 1);

        // System.out.println(HexTool.toString(mplew.getPacket().getBytes()));
        // 72 00 B3 94 00 00 FD FE 30 00 19 FC 00 B4 00 00 00 00 03 01 00 -
        // fukos bird
        // 72 00 30 75 00 00 FD FE 30 00 00 FC 00 B4 00 00 00 00 03 01 00 -
        // faeks bird
        return mplew.getPacket();
    }

    /**
     * Gets a packet to remove a special map object.
     *
     * @param chr The MapleCharacter who removed the object.
     * @param skill The skill used to create the object.
     * @param animated Animated removal?
     * @return The packet removing the object.
     */
    public static MaplePacket removeSpecialMapObject(MapleCharacter chr, int skill, boolean animated) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.REMOVE_SPECIAL_MAPOBJECT.getValue());

        mplew.writeInt(chr.getId());
        mplew.writeInt(skill);

        mplew.write(animated ? 4 : 1); // ?

        // System.out.println(HexTool.toString(mplew.getPacket().getBytes()));
        return mplew.getPacket();
    }

    /**
     * Adds info about an item to an existing MaplePacketLittleEndianWriter.
     *
     * @param mplew The MaplePacketLittleEndianWriter to write to.
     * @param item The item to write info about.
     */
    protected static void addItemInfo(MaplePacketLittleEndianWriter mplew, IItem item) {
        addItemInfo(mplew, item, false, false);
    }

    /**
     * Adds expiration time info to an existing MaplePacketLittleEndianWriter.
     *
     * @param mplew The MaplePacketLittleEndianWriter to write to.
     * @param time The expiration time.
     * @param showexpirationtime Show the expiration time?
     */
    private static void addExpirationTime(MaplePacketLittleEndianWriter mplew, long time, boolean showexpirationtime) {
        mplew.writeInt(KoreanDateUtil.getItemTimestamp(time));
        mplew.write(showexpirationtime ? 1 : 2);
    }

    /**
     * Adds item info to existing MaplePacketLittleEndianWriter.
     *
     * @param mplew The MaplePacketLittleEndianWriter to write to.
     * @param item The item to add info about.
     * @param zeroPosition Is the position zero?
     * @param leaveOut Leave out the item if position is zero?
     */
    private static void addItemInfo(MaplePacketLittleEndianWriter mplew, IItem item, boolean zeroPosition, boolean leaveOut) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        byte pos = item.getPosition();
        boolean masking = false;
        if (zeroPosition) {
            if (!leaveOut) {
                mplew.write(0);
            }
        } else if (pos <= (byte) -1) {
            pos *= -1;
            if (pos > 100) {
                masking = true;
                mplew.write(0);
                mplew.write(pos - 100);
            } else {
                mplew.write(pos);
            }
        } else {
            mplew.write(item.getPosition());
        }

        mplew.write(item.getType());
        mplew.writeInt(item.getItemId());
        if (masking) {
            // 07.03.2008 06:49... o.o
            mplew.write(HexTool.getByteArrayFromHexString("01 41 B4 38 00 00 00 00 00 80 20 6F"));
        } else {
            mplew.writeShort(0);
            mplew.write(ITEM_MAGIC);
        }
        // TODO: Item.getExpirationTime
        addExpirationTime(mplew, 0, false);

        if (item.getType() == IItem.EQUIP) {
            IEquip equip = (IEquip) item;
            mplew.write(equip.getUpgradeSlots());
            mplew.write(equip.getLevel());

            mplew.writeShort(equip.getStr()); // str
            mplew.writeShort(equip.getDex()); // dex
            mplew.writeShort(equip.getInt()); // int
            mplew.writeShort(equip.getLuk()); // luk
            mplew.writeShort(equip.getHp()); // hp
            mplew.writeShort(equip.getMp()); // mp

            mplew.writeShort(equip.getWatk()); // watk
            mplew.writeShort(equip.getMatk()); // matk
            mplew.writeShort(equip.getWdef()); // wdef
            mplew.writeShort(equip.getMdef()); // mdef

            mplew.writeShort(equip.getAcc()); // accuracy
            mplew.writeShort(equip.getAvoid()); // avoid
            mplew.writeShort(equip.getHands()); // hands
            mplew.writeShort(equip.getSpeed()); // speed
            mplew.writeShort(equip.getJump()); // jump

            mplew.writeMapleAsciiString(equip.getOwner());
            mplew.write(0); // 0 normal; 1 locked
            if (!masking) {
                mplew.write(0);
                mplew.writeInt(0); // values of these don't seem to matter at all
                mplew.writeInt(0);
            }
        } else {
            mplew.writeShort(item.getQuantity());
            mplew.writeMapleAsciiString(item.getOwner());
            mplew.writeShort(0); // this seems to end the item entry
            // but only if its not a THROWING STAR :))9 O.O!

            if (ii.isThrowingStar(item.getItemId())) {
                // mplew.write(HexTool.getByteArrayFromHexString("A8 3A 00 00 41
                // 00 00 20"));
                mplew.write(HexTool.getByteArrayFromHexString("A1 6D 05 01 00 00 00 7D"));
            }
        }
    }

    /**
     * Gets a server message packet.
     *
     * @param message The message to convey.
     * @return The server message packet.
     */
    public static MaplePacket serverMessage(String message) {
        return serverMessage(4, 0, message, true);
    }

    /**
     * Gets a server notice packet.
     *
     * Possible values for <code>type</code>:<br>
     * 0: [Notice]<br>
     * 1: Popup<br>
     * 2: Light blue background and lolwhut<br>
     * 4: Scrolling message at top<br>
     * 5: Pink Text<br>
     * 6: Lightblue Text
     *
     * @param type The type of the notice.
     * @param message The message to convey.
     * @return The server notice packet.
     */
    public static MaplePacket serverNotice(int type, String message) {
        return serverMessage(type, 0, message, false);
    }

    /**
     * Gets a server notice packet.
     *
     * Possible values for <code>type</code>:<br>
     * 0: [Notice]<br>
     * 1: Popup<br>
     * 2: Light blue background and lolwhut<br>
     * 4: Scrolling message at top<br>
     * 5: Pink Text<br>
     * 6: Lightblue Text
     *
     * @param type The type of the notice.
     * @param channel The channel this notice was sent on.
     * @param message The message to convey.
     * @return The server notice packet.
     */
    public static MaplePacket serverNotice(int type, int channel, String message) {
        return serverMessage(type, channel, message, false);
    }

    /**
     * Gets a server message packet.
     *
     * Possible values for <code>type</code>:<br>
     * 0: [Notice]<br>
     * 1: Popup<br>
     * 2: Light blue background and lolwhut<br>
     * 4: Scrolling message at top<br>
     * 5: Pink Text<br>
     * 6: Lightblue Text
     *
     * @param type The type of the notice.
     * @param channel The channel this notice was sent on.
     * @param message The message to convey.
     * @param servermessage Is this a scrolling ticker?
     * @return The server notice packet.
     */
    private static MaplePacket serverMessage(int type, int channel, String message, boolean servermessage) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SERVERMESSAGE.getValue());
        mplew.write(type);
        if (servermessage) {
            mplew.write(1);
        }
        mplew.writeMapleAsciiString(message);
        if (type == 3) {
            mplew.write(channel - 1); // channel
            mplew.write(0); // 0 = graues ohr, 1 = lulz?
        }
        return mplew.getPacket();
    }

    /**
     * Gets an avatar megaphone packet.
     *
     * @param chr The character using the avatar megaphone.
     * @param channel The channel the character is on.
     * @param itemId The ID of the avatar-mega.
     * @param message The message that is sent.
     * @return The avatar mega packet.
     */
    public static MaplePacket getAvatarMega(MapleCharacter chr, int channel, int itemId, List<String> message) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendOpcode.AVATAR_MEGA.getValue());
        mplew.writeInt(itemId);
        mplew.writeMapleAsciiString(chr.getName());
        for (String s : message) {
            mplew.writeMapleAsciiString(s);
        }
        mplew.writeInt(channel - 1); // channel

        mplew.write(0);
        addCharLook(mplew, chr, true);

        return mplew.getPacket();
    }

    /**
     * Gets a NPC spawn packet.
     *
     * @param life The NPC to spawn.
     * @param requestController Does the NPC want a controller?
     * @return The NPC spawn packet.
     */
    public static MaplePacket spawnNPC(MapleNPC life, boolean requestController) {
        // B1 00 01 04 00 00 00 34 08 00 00 99 FF 35 00 01 0B 00 67 FF CB FF
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (requestController) {
            mplew.writeShort(SendOpcode.SPAWN_NPC_REQUEST_CONTROLLER.getValue());
            mplew.write(1);
        } else {
            mplew.writeShort(SendOpcode.SPAWN_NPC.getValue());
        }
        mplew.writeInt(life.getObjectId());
        mplew.writeInt(life.getId());
        mplew.writeShort(life.getPosition().x);
        mplew.writeShort(life.getCy());
        mplew.write(1); // type ?
        mplew.writeShort(life.getFh());
        mplew.writeShort(life.getRx0());
        mplew.writeShort(life.getRx1());
        mplew.write(1); // v62 ?
        return mplew.getPacket();
    }

    /**
     * Gets a spawn monster packet.
     *
     * @param life The monster to spawn.
     * @param newSpawn Is it a new spawn?
     * @return The spawn monster packet.
     */
    public static MaplePacket spawnMonster(MapleMonster life, boolean newSpawn) {
        return spawnMonsterInternal(life, false, newSpawn, false, 0);
    }

    /**
     * Gets a spawn monster packet.
     *
     * @param life The monster to spawn.
     * @param newSpawn Is it a new spawn?
     * @param effect The spawn effect.
     * @return The spawn monster packet.
     */
    public static MaplePacket spawnMonster(MapleMonster life, boolean newSpawn, int effect) {
        return spawnMonsterInternal(life, false, newSpawn, false, effect);
    }

    /**
     * Gets a control monster packet.
     *
     * @param life The monster to give control to.
     * @param newSpawn Is it a new spawn?
     * @param aggro Aggressive monster?
     * @return The monster control packet.
     */
    public static MaplePacket controlMonster(MapleMonster life, boolean newSpawn, boolean aggro) {
        return spawnMonsterInternal(life, true, newSpawn, aggro, 0);
    }

    /**
     * Internal function to handler monster spawning and controlling.
     *
     * @param life The mob to perform operations with.
     * @param requestController Requesting control of mob?
     * @param newSpawn New spawn (fade in?)
     * @param aggro Aggressive mob?
     * @param effect The spawn effect to use.
     * @return The spawn/control packet.
     */
    private static MaplePacket spawnMonsterInternal(MapleMonster life, boolean requestController, boolean newSpawn, boolean aggro, int effect) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (requestController) {
            mplew.writeShort(SendOpcode.SPAWN_MONSTER_CONTROL.getValue());
            mplew.write((aggro) ? 2 : 1);
        } else {
            mplew.writeShort(SendOpcode.SPAWN_MONSTER.getValue());
        }
        mplew.writeInt(life.getObjectId());
        mplew.write(5); // ????!? either 5 or 1?

        mplew.writeInt(life.getId());
        mplew.write(new byte[]{0, 0, 0, 8});
        mplew.writeInt(0); // if nonnull client crashes (?)
        mplew.writeShort(life.getPosition().x);
        mplew.writeShort(life.getPosition().y);
        mplew.write(life.getStance()); // or 5? o.O"
        mplew.writeShort(0); // ??
        mplew.writeShort(life.getFh()); // seems to be left and right restriction...
        if (effect > 0) {
            mplew.write(effect);
            mplew.write(0x00);
            mplew.writeShort(0x00); // ?
        }
        mplew.writeShort((newSpawn) ? -2 : -1);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    /**
     * Gets a stop control monster packet.
     *
     * @param oid The ObjectID of the monster to stop controlling.
     * @return The stop control monster packet.
     */
    public static MaplePacket stopControllingMonster(int oid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SPAWN_MONSTER_CONTROL.getValue());
        mplew.write(0);
        mplew.writeInt(oid);
        return mplew.getPacket();
    }

    /**
     * Gets a response to a move monster packet.
     *
     * @param objectid The ObjectID of the monster being moved.
     * @param moveid The movement ID.
     * @param currentMp The current MP of the monster.
     * @param useSkills Can the monster use skills?
     * @return The move response packet.
     */
    public static MaplePacket moveMonsterResponse(int objectid, short moveid, int currentMp, boolean useSkills) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.MOVE_MONSTER_RESPONSE.getValue());
        mplew.writeInt(objectid);
        mplew.writeShort(moveid);
        mplew.write(useSkills ? 1 : 0);
        mplew.writeShort(currentMp);
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    /**
     * Gets a general chat packet.
     *
     * @param cidfrom The character ID who sent the chat.
     * @param text The text of the chat.
     * @return The general chat packet.
     */
    public static MaplePacket getChatText(int cidfrom, String text) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CHATTEXT.getValue());
        mplew.writeInt(cidfrom);
        mplew.write(0); // gms have this set to != 0, gives them white background text
        mplew.writeMapleAsciiString(text);
        mplew.write(0); // v62
        return mplew.getPacket();
    }

    /**
     * For testing only! Gets a packet from a hexadecimal string.
     *
     * @param hex The hexadecimal packet to create.
     * @return The MaplePacket representing the hex string.
     */
    public static MaplePacket getPacketFromHexString(String hex) {
        byte[] b = HexTool.getByteArrayFromHexString(hex);
        return new ByteArrayMaplePacket(b);
    }

    /**
     * Gets a packet telling the client to show an EXP increase.
     *
     * @param gain The amount of EXP gained.
     * @param inChat In the chat box?
     * @param white White text or yellow?
     * @return The exp gained packet.
     */
    public static MaplePacket getShowExpGain(int gain, boolean inChat, boolean white) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        // 20 00 03 01 0A 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00
        mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(3); // 3 = exp, 4 = fame, 5 = mesos, 6 = guildpoints

        mplew.write(white ? 1 : 0);
        mplew.writeInt(gain);
        mplew.write(inChat ? 1 : 0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    /**
     * Gets a packet telling the client to show a meso gain.
     *
     * @param gain How many mesos gained.
     * @return The meso gain packet.
     */
    public static MaplePacket getShowMesoGain(int gain) {
        return getShowMesoGain(gain, false);
    }

    /**
     * Gets a packet telling the client to show a meso gain.
     *
     * @param gain How many mesos gained.
     * @param inChat Show in the chat window?
     * @return The meso gain packet.
     */
    public static MaplePacket getShowMesoGain(int gain, boolean inChat) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
        if (!inChat) {
            mplew.write(0);
            mplew.write(1);
        } else {
            mplew.write(5);
        }
        mplew.writeInt(gain);
        mplew.writeShort(0); // inet cafe meso gain ?.o

        return mplew.getPacket();
    }

    /**
     * Gets a packet telling the client to show a item gain.
     *
     * @param itemId The ID of the item gained.
     * @param quantity How many items gained.
     * @return The item gain packet.
     */
    public static MaplePacket getShowItemGain(int itemId, short quantity) {
        return getShowItemGain(itemId, quantity, false);
    }

    /**
     * Gets a packet telling the client to show an item gain.
     *
     * @param itemId The ID of the item gained.
     * @param quantity The number of items gained.
     * @param inChat Show in the chat window?
     * @return The item gain packet.
     */
    public static MaplePacket getShowItemGain(int itemId, short quantity, boolean inChat) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (inChat) {
            mplew.writeShort(SendOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
            mplew.write(3);
            mplew.write(1);
            mplew.writeInt(itemId);
            mplew.writeInt(quantity);
        } else {
            mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
            mplew.writeShort(0);
            mplew.writeInt(itemId);
            mplew.writeInt(quantity);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        return mplew.getPacket();
    }

    /**
     * Gets a packet telling the client that a monster was killed.
     *
     * @param oid The objectID of the killed monster.
     * @param animation Show killed animation?
     * @return The kill monster packet.
     */
    public static MaplePacket killMonster(int oid, boolean animation) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.KILL_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.write((animation) ? 1 : 0);
        mplew.write(0);
        return mplew.getPacket();
    }

    /**
     * Gets a packet telling the client to show mesos coming out of a map
     * object.
     *
     * @param amount The amount of mesos.
     * @param itemoid The ObjectID of the dropped mesos.
     * @param dropperoid The OID of the dropper.
     * @param ownerid The ID of the drop owner.
     * @param dropfrom Where to drop from.
     * @param dropto Where the drop lands.
     * @param mod ?
     * @return The drop mesos packet.
     */
    public static MaplePacket dropMesoFromMapObject(int amount, int itemoid, int dropperoid, int ownerid,
            Point dropfrom, Point dropto, byte mod) {
        return dropItemFromMapObjectInternal(amount, itemoid, dropperoid, ownerid, dropfrom, dropto, mod, true);
    }

    /**
     * Gets a packet telling the client to show an item coming out of a map
     * object.
     *
     * @param itemid The ID of the dropped item.
     * @param itemoid The ObjectID of the dropped item.
     * @param dropperoid The OID of the dropper.
     * @param ownerid The ID of the drop owner.
     * @param dropfrom Where to drop from.
     * @param dropto Where the drop lands.
     * @param mod ?
     * @return The drop mesos packet.
     */
    public static MaplePacket dropItemFromMapObject(int itemid, int itemoid, int dropperoid, int ownerid,
            Point dropfrom, Point dropto, byte mod) {
        return dropItemFromMapObjectInternal(itemid, itemoid, dropperoid, ownerid, dropfrom, dropto, mod, false);
    }

    /**
     * Internal function to get a packet to tell the client to drop an item onto
     * the map.
     *
     * @param itemid The ID of the item to drop.
     * @param itemoid The ObjectID of the dropped item.
     * @param dropperoid The OID of the dropper.
     * @param ownerid The ID of the drop owner.
     * @param dropfrom Where to drop from.
     * @param dropto Where the drop lands.
     * @param mod ?
     * @param mesos Is the drop mesos?
     * @return The item drop packet.
     */
    public static MaplePacket dropItemFromMapObjectInternal(int itemid, int itemoid, int dropperoid, int ownerid, Point dropfrom, Point dropto, byte mod, boolean mesos) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.DROP_ITEM_FROM_MAPOBJECT.getValue());
        mplew.write(mod);// 1 with animation, 2 without o.o
        mplew.writeInt(itemoid);
        mplew.write(mesos ? 1 : 0); // 1 = mesos, 0 =item
        mplew.writeInt(itemid);
        mplew.writeInt(ownerid); // owner charid
        mplew.write(0);
        mplew.writeShort(dropto.x);
        mplew.writeShort(dropto.y);
        if (mod != 2) {
            mplew.writeInt(ownerid);
            mplew.writeShort(dropfrom.x);
            mplew.writeShort(dropfrom.y);
        } else {
            mplew.writeInt(dropperoid);
        }
        mplew.write(0);
        if (mod != 2) {
            mplew.write(0);
            mplew.write(1);
        }
        if (!mesos) {
            mplew.write(ITEM_MAGIC);
            // TODO getTheExpirationTimeFromSomewhere o.o
            addExpirationTime(mplew, System.currentTimeMillis(), false);
            // mplew.write(1);
            mplew.write(1);
        }

        System.out.println("Drop! objectid: " + itemoid);
        return mplew.getPacket();
    }

    /* (non-javadoc)
	 * TODO: make MapleCharacter a mapobject, remove the need for passing oid
	 * here.
     */
    /**
     * Gets a packet spawning a player as a mapobject to other clients.
     *
     * @param chr The character to spawn to other clients.
     * @return The spawn player packet.
     */
    public static MaplePacket spawnPlayerMapobject(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SPAWN_PLAYER.getValue());
        mplew.writeInt(chr.getId());
        mplew.writeMapleAsciiString(chr.getName());
        if (chr.getGuildId() <= 0) {
            mplew.writeMapleAsciiString("");
            mplew.write(new byte[6]);
        } else {
            MapleGuildSummary gs = chr.getClient().getChannelServer().getGuildSummary(
                    chr.getGuildId());
            if (gs != null) {
                mplew.writeMapleAsciiString(gs.getName());
                mplew.writeShort(gs.getLogoBG());
                mplew.write(gs.getLogoBGColor());
                mplew.writeShort(gs.getLogo());
                mplew.write(gs.getLogoColor());
            } else {
                mplew.writeMapleAsciiString("");
                mplew.write(new byte[6]);
            }
        }
        mplew.writeInt(0);
        mplew.writeInt(1);
        mplew.write(0);
        mplew.writeShort(0);
        mplew.write(0xF8);
        long buffmask = 0;
        Integer buffvalue = null;

        if (chr.getBuffedValue(MapleBuffStat.DARKSIGHT) != null && !chr.isHidden()) {
            buffmask |= MapleBuffStat.DARKSIGHT.getValue();
        }
        if (chr.getBuffedValue(MapleBuffStat.COMBO) != null) {
            buffmask |= MapleBuffStat.COMBO.getValue();
            buffvalue = Integer.valueOf(chr.getBuffedValue(MapleBuffStat.COMBO).intValue());
        }
        if (chr.getBuffedValue(MapleBuffStat.MONSTER_RIDING) != null) {
            buffmask |= MapleBuffStat.MONSTER_RIDING.getValue();
        }
        if (chr.getBuffedValue(MapleBuffStat.SHADOWPARTNER) != null) {
            buffmask |= MapleBuffStat.SHADOWPARTNER.getValue();
        }
        mplew.writeLong(buffmask);
//
        int CHAR_MAGIC_SPAWN = new Random().nextInt();
        mplew.writeInt(0);
        mplew.writeShort(0);
        mplew.writeInt(CHAR_MAGIC_SPAWN);

        mplew.writeLong(0);
        mplew.writeShort(0);
        mplew.writeInt(CHAR_MAGIC_SPAWN);

        mplew.writeLong(0);
        mplew.writeShort(0);
        mplew.writeInt(CHAR_MAGIC_SPAWN);

        mplew.writeLong(0);
        mplew.writeShort(0);
        mplew.writeInt(CHAR_MAGIC_SPAWN);

        mplew.writeLong(0);
        mplew.writeInt(CHAR_MAGIC_SPAWN);

        mplew.writeLong(0);
        mplew.writeInt(0);
        mplew.writeShort(0);
        mplew.writeInt(CHAR_MAGIC_SPAWN);

        mplew.writeInt(0);
        mplew.write(new byte[]{40, 01});
//
        if (buffvalue != null) {
            mplew.write(buffvalue.byteValue());
        }
        addCharLook(mplew, chr, false);
        mplew.writeInt(0);
        mplew.writeInt(chr.getItemEffect());
        mplew.writeInt(chr.getChair());
        mplew.writeShort(chr.getPosition().x);
        mplew.writeShort(chr.getPosition().y);
        mplew.write(chr.getStance());
        // 04 34 00 00
        // mplew.writeInt(1); // dunno p00 (?)
        if (chr.getPet() != null) {
            mplew.writeInt(0x01000000);
            mplew.writeInt(chr.getPet().getItemId());
            mplew.writeMapleAsciiString(chr.getPet().getName());
            // 38 EVTL. Y
            // mplew.write(HexTool.getByteArrayFromHexString("72 FB 38 00 00 00
            // 00 00 09 03 04 00 18 34 00 00 00 01 00
            // 00 00"));
            mplew.write(HexTool.getByteArrayFromHexString("00 00 00 00 00 00 00 00"));
            mplew.writeShort(0);
            mplew.writeShort(chr.getPosition().y);
            mplew.write(HexTool.getByteArrayFromHexString("00 00 00 00 00 00 00 00 00"));
        } else {
            mplew.writeInt(0);
            mplew.writeInt(1);
        }

        mplew.writeLong(0);
        if (chr.getPlayerShop() != null && chr.getPlayerShop().isOwner(chr)) {
            addAnnounceBox(mplew, chr.getPlayerShop());
        } else {
            mplew.write(0);
        }
        mplew.write(new byte[5]);
        mplew.write(-1);
        return mplew.getPacket();
    }

    /**
     * Adds a announcement box to an existing MaplePacketLittleEndianWriter.
     *
     * @param mplew The MaplePacketLittleEndianWriter to add an announcement box
     * to.
     * @param shop The shop to announce.
     */
    private static void addAnnounceBox(MaplePacketLittleEndianWriter mplew, MaplePlayerShop shop) {
        // 00: no game
        // 01: omok game
        // 02: card game
        // 04: shop
        mplew.write(4);
        mplew.writeInt(shop.getObjectId()); // gameid/shopid

        mplew.writeMapleAsciiString(shop.getDescription()); // desc
        // 00: public
        // 01: private

        mplew.write(0);
        // 00: red 4x3
        // 01: green 5x4
        // 02: blue 6x5
        // omok:
        // 00: normal
        mplew.write(0);
        // first slot: 1/2/3/4
        // second slot: 1/2/3/4
        mplew.write(1);
        mplew.write(4);
        // 0: open
        // 1: in progress
        mplew.write(0);
    }

    public static MaplePacket facialExpression(MapleCharacter from, int expression) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.FACIAL_EXPRESSION.getValue());
        // mplew.writeShort(0x85); // 47 83
        mplew.writeInt(from.getId());
        mplew.writeInt(expression);
        return mplew.getPacket();
    }

    private static void serializeMovementList(LittleEndianWriter lew, List<LifeMovementFragment> moves) {
        lew.write(moves.size());
        for (LifeMovementFragment move : moves) {
            move.serialize(lew);
        }
    }

    public static MaplePacket movePlayer(int cid, List<LifeMovementFragment> moves) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.MOVE_PLAYER.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(0);
        serializeMovementList(mplew, moves);
        return mplew.getPacket();
    }

    public static MaplePacket moveSummon(int cid, int summonSkill, Point startPos, List<LifeMovementFragment> moves) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.MOVE_SUMMON.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(summonSkill);
        mplew.writeShort(startPos.x);
        mplew.writeShort(startPos.y);
        serializeMovementList(mplew, moves);
        return mplew.getPacket();
    }

    public static MaplePacket moveMonster(int useskill, int skill, int oid, Point startPos, List<LifeMovementFragment> moves) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.MOVE_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.write(useskill);
        mplew.writeInt(skill);
        mplew.write(0);
        mplew.writeShort(startPos.x);
        mplew.writeShort(startPos.y);
        serializeMovementList(mplew, moves);
        return mplew.getPacket();
    }

    public static MaplePacket summonAttack(int cid, int summonSkillId, int newStance, List<SummonAttackEntry> allDamage) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendOpcode.SUMMON_ATTACK.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(summonSkillId);
        mplew.write(newStance);
        mplew.write(allDamage.size());
        for (SummonAttackEntry attackEntry : allDamage) {
            mplew.writeInt(attackEntry.getMonsterOid()); // oid

            mplew.write(6); // who knows

            mplew.writeInt(attackEntry.getDamage()); // damage

        }

        return mplew.getPacket();
    }

    public static MaplePacket closeRangeAttack(int cid, int skill, int stance, int numAttackedAndDamage,
            List<Pair<Integer, List<Integer>>> damage) {
        // 7D 00 #30 75 00 00# 12 00 06 02 0A 00 00 00 00 01 00 00 00 00 97 02
        // 00 00 97 02 00 00
        // 7D 00 #30 75 00 00# 11 00 06 02 0A 00 00 00 00 20 00 00 00 49 06 00
        // 00
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendOpcode.CLOSE_RANGE_ATTACK.getValue());
        // mplew.writeShort(0x7F); // 47 7D
        if (skill == 4211006) // meso explosion
        {
            addMesoExplosion(mplew, cid, skill, stance, numAttackedAndDamage, 0, damage);
        } else {
            addAttackBody(mplew, cid, skill, stance, numAttackedAndDamage, 0, damage);
        }
        return mplew.getPacket();
    }

    public static MaplePacket rangedAttack(int cid, int skill, int stance, int numAttackedAndDamage, int projectile,
            List<Pair<Integer, List<Integer>>> damage) {
        // 7E 00 30 75 00 00 01 00 97 04 0A CB 72 1F 00
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendOpcode.RANGED_ATTACK.getValue());
        // mplew.writeShort(0x80); // 47 7E
        addAttackBody(mplew, cid, skill, stance, numAttackedAndDamage, projectile, damage);

        return mplew.getPacket();
    }

    public static MaplePacket magicAttack(int cid, int skill, int stance, int numAttackedAndDamage,
            List<Pair<Integer, List<Integer>>> damage) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendOpcode.MAGIC_ATTACK.getValue());
        // mplew.writeShort(0x81);
        addAttackBody(mplew, cid, skill, stance, numAttackedAndDamage, 0, damage);

        return mplew.getPacket();
    }

    private static void addAttackBody(LittleEndianWriter lew, int cid, int skill, int stance, int numAttackedAndDamage,
            int projectile, List<Pair<Integer, List<Integer>>> damage) {
        lew.writeInt(cid);
        lew.write(numAttackedAndDamage);
        if (skill > 0) {
            lew.write(0xFF); // too low and some skills don't work (?)

            lew.writeInt(skill);
        } else {
            lew.write(0);
        }
        lew.write(stance);
        lew.write(HexTool.getByteArrayFromHexString("02 0A"));
        lew.writeInt(projectile);

        for (Pair<Integer, List<Integer>> oned : damage) {
            if (oned.getRight() != null) {
                lew.writeInt(oned.getLeft().intValue());
                lew.write(0xFF);
                for (Integer eachd : oned.getRight()) {
                    // highest bit set = crit
                    lew.writeInt(eachd.intValue());
                }
            }
        }
    }

    private static void addMesoExplosion(LittleEndianWriter lew, int cid, int skill, int stance,
            int numAttackedAndDamage, int projectile,
            List<Pair<Integer, List<Integer>>> damage) {
        // 7A 00 6B F4 0C 00 22 1E 3E 41 40 00 38 04 0A 00 00 00 00 44 B0 04 00
        // 06 02 E6 00 00 00 D0 00 00 00 F2 46 0E 00 06 02 D3 00 00 00 3B 01 00
        // 00
        // 7A 00 6B F4 0C 00 00 1E 3E 41 40 00 38 04 0A 00 00 00 00
        lew.writeInt(cid);
        lew.write(numAttackedAndDamage);
        lew.write(0x1E);
        lew.writeInt(skill);
        lew.write(stance);
        lew.write(HexTool.getByteArrayFromHexString("04 0A"));
        lew.writeInt(projectile);

        for (Pair<Integer, List<Integer>> oned : damage) {
            if (oned.getRight() != null) {
                lew.writeInt(oned.getLeft().intValue());
                lew.write(0xFF);
                lew.write(oned.getRight().size());
                for (Integer eachd : oned.getRight()) {
                    lew.writeInt(eachd.intValue());
                }
            }
        }

    }

    public static MaplePacket getNPCShop(int sid, List<MapleShopItem> items) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.OPEN_NPC_SHOP.getValue());
        mplew.writeInt(sid);
        mplew.writeShort(items.size()); // item count
        for (MapleShopItem item : items) {
            mplew.writeInt(item.getItemId());
            mplew.writeInt(item.getPrice());
            if (!ii.isThrowingStar(item.getItemId())) {
                mplew.writeShort(1); // stacksize o.o
                mplew.writeShort(item.getBuyable());
            } else {
                mplew.writeShort(0);
                mplew.writeInt(0);
                // o.O getPrice sometimes returns the unitPrice not the price
                mplew.writeShort(BitTools.doubleToShortBits(ii.getPrice(item.getItemId())));
                mplew.writeShort(ii.getSlotMax(item.getItemId()));
            }
        }
        return mplew.getPacket();
    }

    /**
     * code (8 = sell, 0 = buy, 0x20 = due to an error the trade did not happen
     * o.o)
     *
     * @param code
     * @return
     */
    public static MaplePacket confirmShopTransaction(byte code) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CONFIRM_SHOP_TRANSACTION.getValue());
        // mplew.writeShort(0xE6); // 47 E4
        mplew.write(code); // recharge == 8?
        return mplew.getPacket();
    }

    /*
	 * 19 reference 00 01 00 = new while adding 01 01 00 = add from drop 00 01 01 = update count 00 01 03 = clear slot
	 * 01 01 02 = move to empty slot 01 02 03 = move and merge 01 02 01 = move and merge with rest
     */
    public static MaplePacket addInventorySlot(MapleInventoryType type, IItem item) {
        return addInventorySlot(type, item, false);
    }

    public static MaplePacket addInventorySlot(MapleInventoryType type, IItem item, boolean fromDrop) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write((fromDrop) ? 1 : 0);
        mplew.write(HexTool.getByteArrayFromHexString("01 00")); // add mode
        mplew.write(type.getType()); // iv type
        mplew.write(item.getPosition()); // slot id
        addItemInfo(mplew, item, true, false);
        return mplew.getPacket();
    }

    public static MaplePacket updateInventorySlot(MapleInventoryType type, IItem item) {
        return updateInventorySlot(type, item, false);
    }

    public static MaplePacket updateInventorySlot(MapleInventoryType type, IItem item, boolean fromDrop) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write((fromDrop) ? 1 : 0);
        mplew.write(HexTool.getByteArrayFromHexString("01 01")); // update
        // mode
        mplew.write(type.getType()); // iv type
        mplew.write(item.getPosition()); // slot id
        mplew.write(0); // ?
        mplew.writeShort(item.getQuantity());
        return mplew.getPacket();
    }

    public static MaplePacket moveInventoryItem(MapleInventoryType type, byte src, byte dst) {
        return moveInventoryItem(type, src, dst, (byte) -1);
    }

    public static MaplePacket moveInventoryItem(MapleInventoryType type, byte src, byte dst, byte equipIndicator) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("01 01 02"));
        mplew.write(type.getType());
        mplew.writeShort(src);
        mplew.writeShort(dst);
        if (equipIndicator != -1) {
            mplew.write(equipIndicator);
        }
        return mplew.getPacket();
    }

    public static MaplePacket moveAndMergeInventoryItem(MapleInventoryType type, byte src, byte dst, short total) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("01 02 03"));
        mplew.write(type.getType());
        mplew.writeShort(src);
        mplew.write(1); // merge mode?

        mplew.write(type.getType());
        mplew.writeShort(dst);
        mplew.writeShort(total);
        return mplew.getPacket();
    }

    public static MaplePacket moveAndMergeWithRestInventoryItem(MapleInventoryType type, byte src, byte dst,
            short srcQ, short dstQ) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("01 02 01"));
        mplew.write(type.getType());
        mplew.writeShort(src);
        mplew.writeShort(srcQ);
        mplew.write(HexTool.getByteArrayFromHexString("01"));
        mplew.write(type.getType());
        mplew.writeShort(dst);
        mplew.writeShort(dstQ);
        return mplew.getPacket();
    }

    public static MaplePacket clearInventoryItem(MapleInventoryType type, byte slot, boolean fromDrop) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(fromDrop ? 1 : 0);
        mplew.write(HexTool.getByteArrayFromHexString("01 03"));
        mplew.write(type.getType());
        mplew.writeShort(slot);
        return mplew.getPacket();
    }

    public static MaplePacket scrolledItem(IItem scroll, IItem item, boolean destroyed) {
        // 18 00 01 02 03 02 08 00 03 01 F7 FF 01
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(1); // fromdrop always true
        mplew.write((destroyed) ? 2 : 3);
        mplew.write((scroll.getQuantity() > 0) ? 1 : 3);
        mplew.write(MapleInventoryType.USE.getType());
        mplew.writeShort(scroll.getPosition());
        if (scroll.getQuantity() > 0) {
            mplew.writeShort(scroll.getQuantity());
        }
        mplew.write(3);
        if (!destroyed) {
            mplew.write(MapleInventoryType.EQUIP.getType());
            mplew.writeShort(item.getPosition());
            mplew.write(0);
        }
        mplew.write(MapleInventoryType.EQUIP.getType());
        mplew.writeShort(item.getPosition());
        if (!destroyed) {
            addItemInfo(mplew, item, true, true);
        }
        mplew.write(1);
        return mplew.getPacket();
    }

    public static MaplePacket getScrollEffect(int chr, ScrollResult scrollSuccess, boolean legendarySpirit) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_SCROLL_EFFECT.getValue());
        mplew.writeInt(chr);
        switch (scrollSuccess) {
            case SUCCESS:
                mplew.writeShort(1);
                if (legendarySpirit) {
                    mplew.writeShort(1);
                } else {
                    mplew.writeShort(0);
                }
                break;
            case FAIL:
                mplew.writeShort(0);
                if (legendarySpirit) {
                    mplew.writeShort(1);
                } else {
                    mplew.writeShort(0);
                }
                break;
            case CURSE:
                mplew.write(0);
                mplew.write(1);
                if (legendarySpirit) {
                    mplew.writeShort(1);
                } else {
                    mplew.writeShort(0);
                }
                break;
            default:
                throw new IllegalArgumentException("effect in illegal range");
        }

        return mplew.getPacket();
    }

    public static MaplePacket removePlayerFromMap(int cid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.REMOVE_PLAYER_FROM_MAP.getValue());
        // mplew.writeShort(0x65); // 47 63
        mplew.writeInt(cid);
        return mplew.getPacket();
    }

    /**
     * animation: 0 - expire<br/> 1 - without animation<br/> 2 - pickup<br/>
     * 4 - explode<br/> cid is ignored for 0 and 1
     *
     * @param oid
     * @param animation
     * @param cid
     * @return
     */
    public static MaplePacket removeItemFromMap(int oid, int animation, int cid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.REMOVE_ITEM_FROM_MAP.getValue());
        mplew.write(animation); // expire
        mplew.writeInt(oid);
        if (animation >= 2) {
            mplew.writeInt(cid);
        }
        return mplew.getPacket();
    }

    public static MaplePacket updateCharLook(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.UPDATE_CHAR_LOOK.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(1);
        addCharLook(mplew, chr, false);
        mplew.writeShort(0);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket dropInventoryItem(MapleInventoryType type, short src) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.MODIFY_INVENTORY_ITEM.getValue());
        // mplew.writeShort(0x19);
        mplew.write(HexTool.getByteArrayFromHexString("01 01 03"));
        mplew.write(type.getType());
        mplew.writeShort(src);
        if (src < 0) {
            mplew.write(1);
        }
        return mplew.getPacket();
    }

    public static MaplePacket dropInventoryItemUpdate(MapleInventoryType type, IItem item) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("01 01 01"));
        mplew.write(type.getType());
        mplew.writeShort(item.getPosition());
        mplew.writeShort(item.getQuantity());
        return mplew.getPacket();
    }

    public static MaplePacket damagePlayer(int skill, int monsteridfrom, int cid, int damage) {
        // 82 00 30 C0 23 00 FF 00 00 00 00 B4 34 03 00 01 00 00 00 00 00 00
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.DAMAGE_PLAYER.getValue());
        // mplew.writeShort(0x84); // 47 82
        mplew.writeInt(cid);
        mplew.write(skill);
        mplew.writeInt(0);
        mplew.writeInt(monsteridfrom);
        mplew.write(1);
        mplew.write(0);
        mplew.write(0); // > 0 = heros will effect

        mplew.writeInt(damage);

        return mplew.getPacket();
    }

    public static MaplePacket charNameResponse(String charname, boolean nameUsed) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CHAR_NAME_RESPONSE.getValue());
        mplew.writeMapleAsciiString(charname);
        mplew.write(nameUsed ? 1 : 0);
        return mplew.getPacket();
    }

    public static MaplePacket addNewCharEntry(MapleCharacter chr, boolean worked) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.ADD_NEW_CHAR_ENTRY.getValue());
        mplew.write(worked ? 0 : 1);
        addCharEntry(mplew, chr);
        return mplew.getPacket();
    }

    /**
     *
     * @param c
     * @param quest
     * @return
     */
    public static MaplePacket startQuest(MapleCharacter c, short quest) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(1);
        mplew.writeShort(quest);
        mplew.writeShort(1);
        mplew.write(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static MaplePacket charInfo(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CHAR_INFO.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(chr.getLevel());
        mplew.writeShort(chr.getJob().getId());
        mplew.writeShort(chr.getFame());
        mplew.write(0); // heart red or gray

        if (chr.getGuildId() <= 0) {
            mplew.writeMapleAsciiString(""); // guild
        } else {
            MapleGuildSummary gs = null;
            gs = chr.getClient().getChannelServer().getGuildSummary(chr.getGuildId());
            if (gs != null) {
                mplew.writeMapleAsciiString(gs.getName());
            } else {
                mplew.writeMapleAsciiString(""); // guild
            }
        }

        if (false) { // got pet
            mplew.write(1);
            mplew.writeInt(5000036); // petid
            mplew.writeMapleAsciiString("Der TOOOOD");
            mplew.write(1); // pet level
            mplew.writeShort(1337); // pet closeness
            mplew.write(200); // pet fullness
            mplew.write(0x30); // ??
            mplew.write(new byte[5]); // probably pet equip and/or mount
            // and/or wishlist
        }
        // no pet
        mplew.write(new byte[3]);
        return mplew.getPacket();
    }

    /**
     *
     * @param c
     * @param quest
     * @return
     */
    public static MaplePacket forfeitQuest(MapleCharacter c, short quest) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(1);
        mplew.writeShort(quest);
        mplew.writeShort(0);
        mplew.write(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    /**
     *
     * @param c
     * @param quest
     * @return
     */
    public static MaplePacket completeQuest(MapleCharacter c, short quest) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(1);
        mplew.writeShort(quest);
        mplew.write(HexTool.getByteArrayFromHexString("02 A0 67 B9 DA 69 3A C8 01"));
        mplew.writeInt(0);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    /**
     *
     * @param c
     * @param quest
     * @param npc
     * @param progress
     * @return
     */
    // frz note, 0.52 transition: this is only used when starting a quest and
    // seems to have no effect, is it needed?
    public static MaplePacket updateQuestInfo(MapleCharacter c, short quest, int npc, byte progress) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.UPDATE_QUEST_INFO.getValue());
        mplew.write(progress);
        mplew.writeShort(quest);
        mplew.writeInt(npc);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    private static <E extends LongValueHolder> long getLongMask(List<Pair<E, Integer>> statups) {
        long mask = 0;
        for (Pair<E, Integer> statup : statups) {
            mask |= statup.getLeft().getValue();
        }
        return mask;
    }

    private static <E extends LongValueHolder> long getLongMaskFromList(List<E> statups) {
        long mask = 0;
        for (E statup : statups) {
            mask |= statup.getValue();
        }
        return mask;
    }

    /**
     * It is important that statups is in the correct order (see decleration
     * order in MapleBuffStat) since this method doesn't do automagical
     * reordering.
     *
     * @param buffid
     * @param bufflength
     * @param statups
     * @return
     */
    public static MaplePacket giveBuff(int buffid, int bufflength, List<Pair<MapleBuffStat, Integer>> statups) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GIVE_BUFF.getValue());

        // darksight
        // 1C 00 80 04 00 00 00 00 00 00 F4 FF EB 0C 3D 00 C8 00 01 00 EB 0C 3D
        // 00 C8 00 00 00 01
        // fire charge
        // 1C 00 04 00 40 00 00 00 00 00 26 00 7B 7A 12 00 90 01 01 00 7B 7A 12
        // 00 90 01 58 02
        // ice charge
        // 1C 00 04 00 40 00 00 00 00 00 07 00 7D 7A 12 00 26 00 01 00 7D 7A 12
        // 00 26 00 58 02
        // thunder charge
        // 1C 00 04 00 40 00 00 00 00 00 0B 00 7F 7A 12 00 18 00 01 00 7F 7A 12
        // 00 18 00 58 02
        // incincible 0.49
        // 1B 00 00 80 00 00 00 00 00 00 0F 00 4B 1C 23 00 F8 24 01 00 00 00
        // mguard 0.49
        // 1B 00 00 02 00 00 00 00 00 00 50 00 6A 88 1E 00 C0 27 09 00 00 00
        // bless 0.49
        // 1B 00 3A 00 00 00 00 00 00 00 14 00 4C 1C 23 00 3F 0D 03 00 14 00 4C
        // 1C 23 00 3F 0D 03 00 14 00 4C 1C 23 00 3F 0D 03 00 14 00 4C 1C 23 00
        // 3F 0D 03 00 00 00
        // combo
        // 1B 00 00 00 20 00 00 00 00 00 01 00 DA F3 10 00 C0 D4 01 00 58 02
        // 1B 00 00 00 20 00 00 00 00 00 02 00 DA F3 10 00 57 B7 01 00 00 00
        // 1B 00 00 00 20 00 00 00 00 00 03 00 DA F3 10 00 51 A7 01 00 00 00
        long mask = getLongMask(statups);

        mplew.writeLong(mask);
        for (Pair<MapleBuffStat, Integer> statup : statups) {
            mplew.writeShort(statup.getRight().shortValue());
            mplew.writeInt(buffid);
            mplew.writeInt(bufflength);
        }

        mplew.writeShort(0); // ??? wk charges have 600 here o.o

        mplew.write(0); // combo 600, too

        return mplew.getPacket();
    }

    public static MaplePacket giveForeignBuff(int cid, List<Pair<MapleBuffStat, Integer>> statups) {
        // 8A 00 24 46 32 00 80 04 00 00 00 00 00 00 F4 00 00
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GIVE_FOREIGN_BUFF.getValue());

        mplew.writeInt(cid);
        long mask = getLongMask(statups);
        mplew.writeLong(mask);
        // TODO write the values somehow? only one byte per value?
        // seems to work
        for (Pair<MapleBuffStat, Integer> statup : statups) {
            mplew.writeShort(statup.getRight().byteValue());
        }
        mplew.writeShort(0); // same as give_buff

        return mplew.getPacket();
    }

    public static MaplePacket cancelForeignBuff(int cid, List<MapleBuffStat> statups) {
        // 8A 00 24 46 32 00 80 04 00 00 00 00 00 00 F4 00 00
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CANCEL_FOREIGN_BUFF.getValue());

        mplew.writeInt(cid);
        mplew.writeLong(getLongMaskFromList(statups));

        return mplew.getPacket();
    }

    public static MaplePacket cancelBuff(List<MapleBuffStat> statups) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CANCEL_BUFF.getValue());
        mplew.writeLong(getLongMaskFromList(statups));
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket getPlayerShopChat(MapleCharacter c, String chat, boolean owner) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("06 08"));
        mplew.write(owner ? 0 : 1);
        mplew.writeMapleAsciiString(c.getName() + " : " + chat);
        return mplew.getPacket();
    }

    public static MaplePacket getPlayerShopNewVisitor(MapleCharacter c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("04 02"));
        addCharLook(mplew, c, false);
        mplew.writeMapleAsciiString(c.getName());
        return mplew.getPacket();
    }

    public static MaplePacket getTradePartnerAdd(MapleCharacter c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("04 01"));// 00 04 88 4E
        // 00"));

        addCharLook(mplew, c, false);
        mplew.writeMapleAsciiString(c.getName());
        return mplew.getPacket();
    }

    public static MaplePacket getTradeInvite(MapleCharacter c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("02 03"));
        mplew.writeMapleAsciiString(c.getName());
        mplew.write(HexTool.getByteArrayFromHexString("B7 50 00 00"));
        return mplew.getPacket();
    }

    public static MaplePacket getTradeMesoSet(byte number, int meso) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0xE);
        mplew.write(number);
        mplew.writeInt(meso);
        return mplew.getPacket();
    }

    public static MaplePacket getTradeItemAdd(byte number, IItem item) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0xD);
        mplew.write(number);
        // mplew.write(1);
        addItemInfo(mplew, item);
        return mplew.getPacket();
    }

    public static MaplePacket getPlayerShopItemUpdate(MaplePlayerShop shop) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x16);
        mplew.write(shop.getItems().size());
        for (MaplePlayerShopItem item : shop.getItems()) {
            mplew.writeShort(item.getBundles());
            mplew.writeShort(item.getItem().getQuantity());
            mplew.writeInt(item.getPrice());
            addItemInfo(mplew, item.getItem(), true, true);
        }
        return mplew.getPacket();
    }

    /**
     *
     * @param c
     * @param shop
     * @param owner
     * @return
     */
    public static MaplePacket getPlayerShop(MapleClient c, MaplePlayerShop shop, boolean owner) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("05 04 04"));
        mplew.write(owner ? 0 : 1);
        mplew.write(0);
        addCharLook(mplew, shop.getOwner(), false);
        mplew.writeMapleAsciiString(shop.getOwner().getName());

        MapleCharacter[] visitors = shop.getVisitors();
        for (int i = 0; i < visitors.length; i++) {
            if (visitors[i] != null) {
                mplew.write(i + 1);
                addCharLook(mplew, visitors[i], false);
                mplew.writeMapleAsciiString(visitors[i].getName());
            }
        }
        mplew.write(0xFF);
        mplew.writeMapleAsciiString(shop.getDescription());
        List<MaplePlayerShopItem> items = shop.getItems();
        mplew.write(0x10);
        mplew.write(items.size());
        for (MaplePlayerShopItem item : items) {
            mplew.writeShort(item.getBundles());
            mplew.writeShort(item.getItem().getQuantity());
            mplew.writeInt(item.getPrice());
            addItemInfo(mplew, item.getItem(), true, true);
        }
        // mplew.write(HexTool.getByteArrayFromHexString("01 60 BF 0F 00 00 00
        // 80 05 BB 46 E6 17 02 05 00 00 00 00 00 00
        // 00 00 00 1D 00 16 00 00 00 00 00 00 00 02 00 00 00 00 00 00 00 00 00
        // 00 00 00 00 00 00 1B 7F 00 00 0D 00 00
        // 40 01 00 01 00 FF 34 0C 00 01 E6 D0 10 00 00 00 80 05 BB 46 E6 17 02
        // 04 01 00 00 00 00 00 00 00 00 0A 00 00
        // 00 00 00 00 00 00 00 00 00 00 00 0F 00 00 00 00 00 00 00 00 00 00 00
        // 63 CF 07 01 00 00 00 7C 01 00 01 00 5F
        // AE 0A 00 01 79 16 15 00 00 00 80 05 BB 46 E6 17 02 07 00 00 00 00 00
        // 00 00 00 00 66 00 00 00 21 00 2F 00 00
        // 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 A4 82 7A 01 00 00
        // 00 7C 01 00 01 00 5F AE 0A 00 01 79 16
        // 15 00 00 00 80 05 BB 46 E6 17 02 07 00 00 00 00 00 00 00 00 00 66 00
        // 00 00 23 00 2C 00 00 00 00 00 00 00 00
        // 00 00 00 00 00 00 00 00 00 00 00 FE AD 88 01 00 00 00 7C 01 00 01 00
        // DF 67 35 00 01 E5 D0 10 00 00 00 80 05
        // BB 46 E6 17 02 01 03 00 00 00 00 07 00 00 00 00 00 00 00 00 00 00 00
        // 00 00 00 00 00 00 0A 00 00 00 00 00 00
        // 00 00 00 00 00 CE D4 F1 00 00 00 00 7C 01 00 01 00 7F 1A 06 00 01 4C
        // BF 0F 00 00 00 80 05 BB 46 E6 17 02 05
        // 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 1D 00 00 00
        // 00 00 00 00 00 00 00 00 00 00 00 00 38
        // CE AF 00 00 00 00 7C 01 00 01 00 BF 27 09 00 01 07 76 16 00 00 00 80
        // 05 BB 46 E6 17 02 00 07 00 00 00 00 00
        // 00 00 00 00 00 00 00 17 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
        // 00 00 00 00 00 00 7C 02 00 00 1E 00 00
        // 48 01 00 01 00 5F E3 16 00 01 11 05 14 00 00 00 80 05 BB 46 E6 17 02
        // 07 00 00 00 00 00 00 00 00 00 00 00 00
        // 00 21 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
        // 1C 8A 00 00 39 00 00 10 01 00 01 00 7F
        // 84 1E 00 01 05 DE 13 00 00 00 80 05 BB 46 E6 17 02 07 00 00 00 00 00
        // 00 00 00 00 00 00 00 00 00 00 00 00 00
        // 00 00 00 00 00 00 00 00 00 0C 00 00 00 00 00 00 00 00 E5 07 01 00 00
        // 00 7C 2B 00 01 00 AF B3 00 00 02 FC 0C
        // 3D 00 00 00 80 05 BB 46 E6 17 02 2B 00 00 00 00 00 00 00 01 00 0F 27
        // 00 00 02 D1 ED 2D 00 00 00 80 05 BB 46
        // E6 17 02 01 00 00 00 00 00 0A 00 01 00 9F 0F 00 00 02 84 84 1E 00 00
        // 00 80 05 BB 46 E6 17 02 0A 00 00 00 00
        // 00 01 00 01 00 FF 08 3D 00 01 02 05 14 00 00 00 80 05 BB 46 E6 17 02
        // 07 00 00 00 00 00 00 00 00 00 00 00 00
        // 00 25 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
        // 78 36 00 00 1D 00 00 14 01 00 01 00 9F
        // 25 26 00 01 2B 2C 14 00 00 00 80 05 BB 46 E6 17 02 07 00 00 00 00 00
        // 00 00 00 00 00 00 00 00 34 00 00 00 06
        // 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 E8 76 00 00 1F 00
        // 00 24 01 00 01 00 BF 0E 16 02 01 D9 D0
        // 10 00 00 00 80 05 BB 46 E6 17 02 00 04 00 00 00 00 00 00 07 00 00 00
        // 00 00 02 00 00 00 06 00 08 00 00 00 00
        // 00 00 00 00 00 00 00 00 00 00 00 23 02 00 00 1C 00 00 1C 5A 00 01 00
        // 0F 27 00 00 02 B8 14 3D 00 00 00 80 05
        // BB 46 E6 17 02 5A 00 00 00 00 00"));
        /*
		 * 10 10 01 00 01 00 3F 42 0F 00 01 60 BF 0F 00 00 00 80 05 BB /* ||||||||||| OMG ITS THE PRICE ||||| PROBABLY
		 * THA QUANTITY ||||||||||| itemid
		 * 
         */
        // mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static MaplePacket getTradeStart(MapleClient c, MapleTrade trade, byte number) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("05 03 02"));
        mplew.write(number);
        if (number == 1) {
            mplew.write(0);
            addCharLook(mplew, trade.getPartner().getChr(), false);
            mplew.writeMapleAsciiString(trade.getPartner().getChr().getName());
        }
        mplew.write(number);
        /*if (number == 1) {
		mplew.write(0);
		mplew.writeInt(c.getPlayer().getId());
		}*/
        addCharLook(mplew, c.getPlayer(), false);
        mplew.writeMapleAsciiString(c.getPlayer().getName());
        mplew.write(0xFF);
        return mplew.getPacket();
    }

    public static MaplePacket getTradeConfirmation() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0xF);
        return mplew.getPacket();
    }

    public static MaplePacket getTradeCompletion(byte number) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0xA);
        mplew.write(number);
        mplew.write(6);
        return mplew.getPacket();
    }

    public static MaplePacket getTradeCancel(byte number) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0xA);
        mplew.write(number);
        mplew.write(2);
        return mplew.getPacket();
    }

    public static MaplePacket updateCharBox(MapleCharacter c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.UPDATE_CHAR_BOX.getValue());
        mplew.writeInt(c.getId());
        if (c.getPlayerShop() != null) {
            addAnnounceBox(mplew, c.getPlayerShop());
        } else {
            mplew.write(0);
        }
        return mplew.getPacket();
    }

//    public static MaplePacket getNPCTalk(int npc, byte msgType, String talk, String endBytes) {
//        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
//        mplew.writeShort(SendOpcode.NPC_TALK.getValue());
//        mplew.write(4); // ?
//        mplew.writeInt(npc);
//        mplew.write(msgType);
//        mplew.writeMapleAsciiString(talk);
//        mplew.write(HexTool.getByteArrayFromHexString(endBytes));
//        return mplew.getPacket();
//    }
//
//    public static MaplePacket getNPCTalkStyle(int npc, String talk, int styles[]) {
//        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
//        mplew.writeShort(SendOpcode.NPC_TALK.getValue());
//        mplew.write(4); // ?
//        mplew.writeInt(npc);
//        mplew.write(7);
//        mplew.writeMapleAsciiString(talk);
//        mplew.write(styles.length);
//        for (int i = 0; i < styles.length; i++) {
//            mplew.writeInt(styles[i]);
//        }
//        return mplew.getPacket();
//    }
//
//    public static MaplePacket getNPCTalkNum(int npc, String talk, int def, int min, int max) {
//        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
//        mplew.writeShort(SendOpcode.NPC_TALK.getValue());
//        mplew.write(4); // ?
//        mplew.writeInt(npc);
//        mplew.write(4);
//        mplew.writeMapleAsciiString(talk);
//        mplew.writeInt(def);
//        mplew.writeInt(min);
//        mplew.writeInt(max);
//        mplew.writeInt(0);
//        return mplew.getPacket();
//    }
//
//    public static MaplePacket getNPCTalkText(int npc, String talk) {
//        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
//        mplew.writeShort(SendOpcode.NPC_TALK.getValue());
//        mplew.write(4); // ?
//        mplew.writeInt(npc);
//        mplew.write(3);
//        mplew.writeMapleAsciiString(talk);
//        mplew.writeInt(0);
//        mplew.writeInt(0);
//        return mplew.getPacket();
//    }
    public static MaplePacket showLevelup(int cid) {
        return showForeignEffect(cid, 0);
    }

    public static MaplePacket showJobChange(int cid) {
        return showForeignEffect(cid, 8);
    }

    public static MaplePacket showForeignEffect(int cid, int effect) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(cid); // ?
        mplew.write(effect);
        return mplew.getPacket();
    }

    public static MaplePacket showBuffeffect(int cid, int skillid, int effectid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(cid); // ?

        mplew.write(effectid);
        mplew.writeInt(skillid);
        mplew.write(1); // probably buff level but we don't know it and it
        // doesn't really matter

        return mplew.getPacket();
    }

    public static MaplePacket showOwnBuffEffect(int skillid, int effectid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(effectid);
        mplew.writeInt(skillid);
        mplew.write(1); // probably buff level but we don't know it and it
        // doesn't really matter

        return mplew.getPacket();
    }

    public static MaplePacket updateSkill(int skillid, int level, int masterlevel) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.UPDATE_SKILLS.getValue());
        mplew.write(1);
        mplew.writeShort(1);
        mplew.writeInt(skillid);
        mplew.writeInt(level);
        mplew.writeInt(masterlevel);
        mplew.write(1);
        return mplew.getPacket();
    }

    public static MaplePacket updateQuestMobKills(MapleQuestStatus status) {
        // 21 00 01 FB 03 01 03 00 30 30 31
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(1);
        mplew.writeShort(status.getQuest().getId());
        mplew.write(1);
        String killStr = "";
        for (int kills : status.getMobKills().values()) {
            killStr += StringUtil.getLeftPaddedStr(String.valueOf(kills), '0', 3);
        }
        mplew.writeMapleAsciiString(killStr);
        mplew.writeInt(0);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static MaplePacket getShowQuestCompletion(int id) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_QUEST_COMPLETION.getValue());
        mplew.writeShort(id);
        return mplew.getPacket();
    }

    public static MaplePacket getKeymap(Map<Integer, MapleKeyBinding> keybindings) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.KEYMAP.getValue());
        mplew.write(0);
        for (int x = 0; x < 90; x++) {
            MapleKeyBinding binding = keybindings.get(x);
            if (binding != null) {
                mplew.write(binding.getType());
                mplew.writeInt(binding.getAction());
            } else {
                mplew.write(0);
                mplew.writeInt(0);
            }
        }

        return mplew.getPacket();
    }

    public static MaplePacket getWhisper(String sender, int channel, String text) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.WHISPER.getValue());
        mplew.write(0x12);
        mplew.writeMapleAsciiString(sender);
        mplew.writeShort(channel - 1); // I guess this is the channel

        mplew.writeMapleAsciiString(text);
        return mplew.getPacket();
    }

    /**
     *
     * @param target name of the target character
     * @param reply error code: 0x0 = cannot find char, 0x1 = success
     * @return the MaplePacket
     */
    public static MaplePacket getWhisperReply(String target, byte reply) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.WHISPER.getValue());
        mplew.write(0x0A); // whisper?

        mplew.writeMapleAsciiString(target);
        mplew.write(reply);
        // System.out.println(HexTool.toString(mplew.getPacket().getBytes()));
        return mplew.getPacket();
    }

    public static MaplePacket getFindReplyWithMap(String target, int mapid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.WHISPER.getValue());
        mplew.write(9);
        mplew.writeMapleAsciiString(target);
        mplew.write(1);
        mplew.writeInt(mapid);
        // ?? official doesn't send zeros here but whatever
        mplew.write(new byte[8]);
        return mplew.getPacket();
    }

    public static MaplePacket getFindReply(String target, int channel) {
        // Received UNKNOWN (1205941596.79689): (25)
        // 54 00 09 07 00 64 61 76 74 73 61 69 01 86 7F 3D 36 D5 02 00 00 22 00
        // 00 00
        // T....davtsai..=6...."...
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.WHISPER.getValue());
        mplew.write(9);
        mplew.writeMapleAsciiString(target);
        mplew.write(3);
        mplew.writeInt(channel - 1);
        return mplew.getPacket();
    }

    public static MaplePacket getInventoryFull() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(1);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket getShowInventoryFull() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(0);
        mplew.write(0xFF);
        mplew.writeInt(0);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static MaplePacket getStorage(int npcId, byte slots, Collection<IItem> items, int meso) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.OPEN_STORAGE.getValue());
        mplew.write(0x13);
        mplew.writeInt(npcId);
        mplew.write(slots);
        mplew.writeShort(0x7E);
        mplew.writeInt(meso);
        mplew.write(HexTool.getByteArrayFromHexString("00 00 00"));
        mplew.write((byte) items.size());
        for (IItem item : items) {
            addItemInfo(mplew, item, true, true);
        }
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket getStorageFull() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.OPEN_STORAGE.getValue());
        mplew.write(0xE);
        return mplew.getPacket();
    }

    public static MaplePacket mesoStorage(byte slots, int meso) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.OPEN_STORAGE.getValue());
        mplew.write(0x10);
        mplew.write(slots);
        mplew.writeShort(2);
        mplew.writeInt(meso);
        return mplew.getPacket();
    }

    public static MaplePacket storeStorage(byte slots, MapleInventoryType type, Collection<IItem> items) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.OPEN_STORAGE.getValue());
        mplew.write(0xB);
        mplew.write(slots);
        mplew.writeShort(type.getBitfieldEncoding());
        mplew.write(items.size());
        for (IItem item : items) {
            addItemInfo(mplew, item, true, true);
            // mplew.write(0);
        }

        return mplew.getPacket();
    }

    /*public static MaplePacket takeOutStorage(byte slots, byte slot) {
	MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
	mplew.writeShort(SendOpcode.OPEN_STORAGE.getValue());
	mplew.write(8);
	mplew.write(slots);
	mplew.write(4 * slot);
	mplew.writeShort(0);
	return mplew.getPacket();
	}*/
    public static MaplePacket takeOutStorage(byte slots, MapleInventoryType type, Collection<IItem> items) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.OPEN_STORAGE.getValue());
        mplew.write(0x8);
        mplew.write(slots);
        mplew.writeShort(type.getBitfieldEncoding());
        mplew.write(items.size());
        for (IItem item : items) {
            addItemInfo(mplew, item, true, true);
            // mplew.write(0);
        }

        return mplew.getPacket();
    }

    /**
     *
     * @param oid
     * @param remhp in %
     * @return
     */
    public static MaplePacket showMonsterHP(int oid, int remhppercentage) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_MONSTER_HP.getValue());
        mplew.writeInt(oid);
        mplew.write(remhppercentage);
        return mplew.getPacket();
    }

    public static MaplePacket showBossHP(int oid, int currHP, int maxHP, byte tagColor, byte tagBgColor) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        //53 00 05 21 B3 81 00 46 F2 5E 01 C0 F3 5E 01 04 01
        //00 81 B3 21 = 8500001 = Pap monster ID
        //01 5E F3 C0 = 23,000,000 = Pap max HP
        //04, 01 - boss bar color/background color as provided in WZ
        mplew.writeShort(SendOpcode.BOSS_ENV.getValue());
        mplew.write(5);
        mplew.writeInt(oid);
        mplew.writeInt(currHP);
        mplew.writeInt(maxHP);
        mplew.write(tagColor);
        mplew.write(tagBgColor);

        return mplew.getPacket();
    }

    public static MaplePacket giveFameResponse(int mode, String charname, int newfame) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.FAME_RESPONSE.getValue());

        mplew.write(0);
        mplew.writeMapleAsciiString(charname);
        mplew.write(mode);
        mplew.writeShort(newfame);
        mplew.writeShort(0);

        return mplew.getPacket();
    }

    /**
     * status can be: <br>
     * 0: ok, use giveFameResponse<br>
     * 1: the username is incorrectly entered<br>
     * 2: users under level 15 are unable to toggle with fame.<br>
     * 3: can't raise or drop fame anymore today.<br>
     * 4: can't raise or drop fame for this character for this month
     * anymore.<br>
     * 5: received fame, use receiveFame()<br>
     * 6: level of fame neither has been raised nor dropped due to an unexpected
     * error
     *
     * @param status
     * @param mode
     * @param charname
     * @param newfame
     * @return
     */
    public static MaplePacket giveFameErrorResponse(int status) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.FAME_RESPONSE.getValue());
        mplew.write(status);

        return mplew.getPacket();
    }

    public static MaplePacket receiveFame(int mode, String charnameFrom) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.FAME_RESPONSE.getValue());
        mplew.write(5);
        mplew.writeMapleAsciiString(charnameFrom);
        mplew.write(mode);

        return mplew.getPacket();
    }

    public static MaplePacket partyCreated() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PARTY_OPERATION.getValue());
        mplew.write(8);
        mplew.writeShort(0x8b);
        mplew.writeShort(2);
        mplew.write(CHAR_INFO_MAGIC);
        mplew.write(CHAR_INFO_MAGIC);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static MaplePacket partyInvite(MapleCharacter from) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PARTY_OPERATION.getValue());
        mplew.write(4);
        mplew.writeInt(from.getParty().getId());
        mplew.writeMapleAsciiString(from.getName());

        return mplew.getPacket();
    }

    /**
     * 10: a beginner can't create a party<br>
     * 11/14/19: your request for a party didn't work due to an unexpected
     * error<br>
     * 13: you have yet to join a party<br>
     * 16: already have joined a party<br>
     * 17: the party you are trying to join is already at full capacity<br>
     * 18: unable to find the requested character in this channel<br>
     *
     * @param message
     * @return
     */
    public static MaplePacket partyStatusMessage(int message) {
        // 32 00 08 DA 14 00 00 FF C9 9A 3B FF C9 9A 3B 22 03 6E 67
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PARTY_OPERATION.getValue());
        mplew.write(message);

        return mplew.getPacket();
    }

    /**
     * 22: has denied the invitation<br>
     *
     * @param message
     * @param charname
     * @return
     */
    public static MaplePacket partyStatusMessage(int message, String charname) {
        // 32 00 08 DA 14 00 00 FF C9 9A 3B FF C9 9A 3B 22 03 6E 67
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PARTY_OPERATION.getValue());
        mplew.write(message);
        mplew.writeMapleAsciiString(charname);

        return mplew.getPacket();
    }

    private static void addPartyStatus(int forchannel, MapleParty party, LittleEndianWriter lew, boolean leaving) {
        List<MaplePartyCharacter> partymembers = new ArrayList<MaplePartyCharacter>(party.getMembers());
        while (partymembers.size() < 6) {
            partymembers.add(new MaplePartyCharacter());
        }
        for (MaplePartyCharacter partychar : partymembers) {
            lew.writeInt(partychar.getId());
        }
        for (MaplePartyCharacter partychar : partymembers) {
            lew.writeAsciiString(StringUtil.getRightPaddedStr(partychar.getName(), '\0', 13));
        }
        for (MaplePartyCharacter partychar : partymembers) {
            lew.writeInt(partychar.getJobId());
        }
        for (MaplePartyCharacter partychar : partymembers) {
            lew.writeInt(partychar.getLevel());
        }
        for (MaplePartyCharacter partychar : partymembers) {
            if (partychar.isOnline()) {
                lew.writeInt(partychar.getChannel() - 1);
            } else {
                lew.writeInt(-2);
            }
        }
        lew.writeInt(party.getLeader().getId());
        for (MaplePartyCharacter partychar : partymembers) {
            if (partychar.getChannel() == forchannel) {
                lew.writeInt(partychar.getMapid());
            } else {
                lew.writeInt(-2);
            }
        }
        for (MaplePartyCharacter partychar : partymembers) {
            if (partychar.getChannel() == forchannel && !leaving) {
                lew.writeInt(partychar.getDoorTown());
                lew.writeInt(partychar.getDoorTarget());
                lew.writeInt(partychar.getDoorPosition().x);
                lew.writeInt(partychar.getDoorPosition().y);
            } else {
                lew.writeInt(999999999);
                lew.writeInt(999999999);
                lew.writeInt(-1);
                lew.writeInt(-1);
            }
        }
    }

    public static MaplePacket updateParty(int forChannel, MapleParty party, PartyOperation op, MaplePartyCharacter target) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendOpcode.PARTY_OPERATION.getValue());
        switch (op) {
            case DISBAND:
            case EXPEL:
            case LEAVE:
                mplew.write(0xC);
                mplew.writeShort(0x8b);
                mplew.writeShort(2);
                mplew.writeInt(target.getId());

                if (op == PartyOperation.DISBAND) {
                    mplew.write(0);
                    mplew.writeInt(party.getId());
                } else {
                    mplew.write(1);
                    if (op == PartyOperation.EXPEL) {
                        mplew.write(1);
                    } else {
                        mplew.write(0);
                    }
                    mplew.writeMapleAsciiString(target.getName());
                    addPartyStatus(forChannel, party, mplew, false);
                    // addLeavePartyTail(mplew);
                }

                break;
            case JOIN:
                mplew.write(0xF);
                mplew.writeShort(0x8b);
                mplew.writeShort(2);
                mplew.writeMapleAsciiString(target.getName());
                addPartyStatus(forChannel, party, mplew, false);
                // addJoinPartyTail(mplew);
                break;
            case SILENT_UPDATE:
            case LOG_ONOFF:
                if (op == PartyOperation.LOG_ONOFF) {
                    mplew.write(0x1F); // actually this is silent too

                } else {
                    mplew.write(0x7);
                }
                mplew.write(0xdd);
                mplew.write(0x14);
                mplew.writeShort(0);
                addPartyStatus(forChannel, party, mplew, false);
                // addJoinPartyTail(mplew);
                // addDoorPartyTail(mplew);
                break;

        }
        // System.out.println("partyupdate: " +
        // HexTool.toString(mplew.getPacket().getBytes()));
        return mplew.getPacket();
    }

    public static MaplePacket partyPortal(int townId, int targetId, Point position) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendOpcode.PARTY_OPERATION.getValue());
        mplew.writeShort(0x22);
        mplew.writeInt(townId);
        mplew.writeInt(targetId);
        mplew.writeShort(position.x);
        mplew.writeShort(position.y);
        // System.out.println("partyportal: " +
        // HexTool.toString(mplew.getPacket().getBytes()));
        return mplew.getPacket();
    }

    // 87 00 30 75 00 00# 00 02 00 00 00 03 00 00
    public static MaplePacket updatePartyMemberHP(int cid, int curhp, int maxhp) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendOpcode.UPDATE_PARTYMEMBER_HP.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(curhp);
        mplew.writeInt(maxhp);
        return mplew.getPacket();
    }

    /**
     * mode: 0 buddychat; 1 partychat; 2 guildchat
     *
     * @param name
     * @param chattext
     * @param mode
     * @return
     */
    public static MaplePacket multiChat(String name, String chattext, int mode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.MULTICHAT.getValue());
        mplew.write(mode);
        mplew.writeMapleAsciiString(name);
        mplew.writeMapleAsciiString(chattext);
        return mplew.getPacket();
    }

    public static MaplePacket applyMonsterStatus(int oid, Map<MonsterStatus, Integer> stats, int skill, boolean monsterSkill, int delay) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.APPLY_MONSTER_STATUS.getValue());
        mplew.writeInt(oid);
        int mask = 0;
        for (MonsterStatus stat : stats.keySet()) {
            mask |= stat.getValue();
        }
        mplew.writeInt(mask);
        for (Integer val : stats.values()) {
            mplew.writeShort(val);
            if (monsterSkill) {
                mplew.writeShort(skill);
                mplew.writeShort(1);
            } else {
                mplew.writeInt(skill);
            }
            mplew.writeShort(0); // as this looks similar to giveBuff this
            // might actually be the buffTime but it's
            // not displayed anywhere
        }
        mplew.writeShort(delay); // delay in ms
        mplew.write(1); // ?
        return mplew.getPacket();
    }

    public static MaplePacket cancelMonsterStatus(int oid, Map<MonsterStatus, Integer> stats) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CANCEL_MONSTER_STATUS.getValue());

        mplew.writeInt(oid);
        int mask = 0;
        for (MonsterStatus stat : stats.keySet()) {
            mask |= stat.getValue();
        }

        mplew.writeInt(mask);
        mplew.write(1);

        return mplew.getPacket();
    }

    public static MaplePacket getClock(int time) { // time in seconds

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CLOCK.getValue());
        mplew.write(2); // clock type. if you send 3 here you have to send
        // another byte (which does not matter at all) before
        // the timestamp

        mplew.writeInt(time);
        return mplew.getPacket();
    }

    public static MaplePacket getClockTime(int hour, int min, int sec) { // Current Time

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CLOCK.getValue());
        mplew.write(1); //Clock-Type
        mplew.write(hour);
        mplew.write(min);
        mplew.write(sec);
        return mplew.getPacket();
    }

    public static MaplePacket spawnMist(int oid, int ownerCid, int skillId, Rectangle mistPosition) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendOpcode.SPAWN_MIST.getValue());
        mplew.writeInt(oid); // maybe this should actually be the "mistid" -
        // seems to always be 1 with only one mist in
        // the map...

        mplew.write(0);
        mplew.writeInt(ownerCid); // probably only intresting for smokescreen

        mplew.writeInt(skillId);
        mplew.write(1); // who knows

        mplew.writeShort(7); // ???

        mplew.writeInt(mistPosition.x); // left position

        mplew.writeInt(mistPosition.y); // bottom position

        mplew.writeInt(mistPosition.x + mistPosition.width); // left position

        mplew.writeInt(mistPosition.y + mistPosition.height); // upper
        // position

        mplew.write(0);

        return mplew.getPacket();
    }

    public static MaplePacket removeMist(int oid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendOpcode.REMOVE_MIST.getValue());
        mplew.writeInt(oid);

        return mplew.getPacket();
    }

    public static MaplePacket damageSummon(int cid, int summonSkillId, int damage, int unkByte, int monsterIdFrom) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendOpcode.DAMAGE_SUMMON.getValue());
        // 77 00 29 1D 02 00 FA FE 30 00 00 10 00 00 00 BF 70 8F 00 00
        mplew.writeInt(cid);
        mplew.writeInt(summonSkillId);
        mplew.write(unkByte);
        mplew.writeInt(damage);
        mplew.writeInt(monsterIdFrom);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static MaplePacket damageMonster(int oid, int damage) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendOpcode.DAMAGE_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.write(0);
        mplew.writeInt(damage);

        return mplew.getPacket();
    }

    public static MaplePacket updateBuddylist(Collection<BuddylistEntry> buddylist) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.BUDDYLIST.getValue());
        mplew.write(7);
        mplew.write(buddylist.size());
        for (BuddylistEntry buddy : buddylist) {
            if (buddy.isVisible()) {
                mplew.writeInt(buddy.getCharacterId()); // cid
                mplew.writeAsciiString(StringUtil.getRightPaddedStr(buddy.getName(), '\0', 13));
                mplew.write(0);
                mplew.writeInt(buddy.getChannel() - 1);
            }
        }
        for (int x = 0; x < buddylist.size(); x++) {
            mplew.writeInt(0);
        }
        return mplew.getPacket();
    }

    public static MaplePacket requestBuddylistAdd(int cidFrom, String nameFrom) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendOpcode.BUDDYLIST.getValue());
        mplew.write(9);
        mplew.writeInt(cidFrom);
        mplew.writeMapleAsciiString(nameFrom);
        mplew.writeInt(cidFrom);
        mplew.writeAsciiString(StringUtil.getRightPaddedStr(nameFrom, '\0', 13));
        mplew.write(1);
        mplew.write(31);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static MaplePacket updateBuddyChannel(int characterid, int channel) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendOpcode.BUDDYLIST.getValue());
        // 2B 00 14 30 C0 23 00 00 11 00 00 00
        mplew.write(0x14);
        mplew.writeInt(characterid);
        mplew.write(0);
        mplew.writeInt(channel);

        // 2B 00 14 30 C0 23 00 00 0D 00 00 00
        // 2B 00 14 30 75 00 00 00 11 00 00 00
        return mplew.getPacket();
    }

    public static MaplePacket itemEffect(int characterid, int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendOpcode.SHOW_ITEM_EFFECT.getValue());

        mplew.writeInt(characterid);
        mplew.writeInt(itemid);

        return mplew.getPacket();
    }

    public static MaplePacket updateBuddyCapacity(int capacity) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendOpcode.BUDDYLIST.getValue());
        mplew.write(0x15);
        mplew.write(capacity);

        return mplew.getPacket();
    }

    public static MaplePacket showChair(int characterid, int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendOpcode.SHOW_CHAIR.getValue());

        mplew.writeInt(characterid);
        mplew.writeInt(itemid);

        return mplew.getPacket();
    }

    public static MaplePacket cancelChair() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendOpcode.CANCEL_CHAIR.getValue());

        mplew.write(0);

        return mplew.getPacket();
    }

    // is there a way to spawn reactors non-animated?
    public static MaplePacket spawnReactor(MapleReactor reactor) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        Point pos = reactor.getPosition();
        mplew.writeShort(SendOpcode.REACTOR_SPAWN.getValue());
        mplew.writeInt(reactor.getObjectId());
        mplew.writeInt(reactor.getId());
        mplew.write(reactor.getState());
        mplew.writeShort(pos.x);
        mplew.writeShort(pos.y);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static MaplePacket triggerReactor(MapleReactor reactor, int stance) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        Point pos = reactor.getPosition();

        mplew.writeShort(SendOpcode.REACTOR_HIT.getValue());
        mplew.writeInt(reactor.getObjectId());
        mplew.write(reactor.getState());
        mplew.writeShort(pos.x);
        mplew.writeShort(pos.y);
        mplew.writeShort(stance);
        mplew.write(0);

        //frame delay, set to 5 since there doesn't appear to be a fixed formula for it
        mplew.write(5);

        return mplew.getPacket();
    }

    public static MaplePacket destroyReactor(MapleReactor reactor) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        Point pos = reactor.getPosition();

        mplew.writeShort(SendOpcode.REACTOR_DESTROY.getValue());
        mplew.writeInt(reactor.getObjectId());
        mplew.write(reactor.getState());
        mplew.writeShort(pos.x);
        mplew.writeShort(pos.y);

        return mplew.getPacket();
    }

    public static MaplePacket musicChange(String song) {
        return environmentChange(song, 6);
    }

    public static MaplePacket showEffect(String effect) {
        return environmentChange(effect, 3);
    }

    public static MaplePacket playSound(String sound) {
        return environmentChange(sound, 4);
    }

    public static MaplePacket environmentChange(String env, int mode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendOpcode.BOSS_ENV.getValue());
        mplew.write(mode);
        mplew.writeMapleAsciiString(env);

        return mplew.getPacket();
    }

    public static MaplePacket startMapEffect(String msg, int itemid, boolean active) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendOpcode.MAP_EFFECT.getValue());
        mplew.write(active ? 0 : 1);

        mplew.writeInt(itemid);
        if (active) {
            mplew.writeMapleAsciiString(msg);
        }
        return mplew.getPacket();
    }

    public static MaplePacket removeMapEffect() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendOpcode.MAP_EFFECT.getValue());
        mplew.write(0);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static MaplePacket showGuildInfo(MapleCharacter c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x1A); //signature for showing guild info
        if (c == null) { //show empty guild (used for leaving, expelled)
            mplew.write(0);
            return mplew.getPacket();
        }
        MapleGuildCharacter initiator = c.getMGC();
        MapleGuild g = c.getClient().getChannelServer().getGuild(initiator);
        if (g == null) { //failed to read from DB - don't show a guild
            mplew.write(0);
            log.warn(MapleClient.getLogMessage(c, "Couldn't load a guild"));
            return mplew.getPacket();
        } else {
            //MapleGuild holds the absolute correct value of guild rank after it is initiated
            MapleGuildCharacter mgc = g.getMGC(c.getId());
            c.setGuildRank(mgc.getGuildRank());
        }
        mplew.write(1); //bInGuild
        mplew.writeInt(c.getGuildId()); //not entirely sure about this one
        mplew.writeMapleAsciiString(g.getName());
        for (int i = 1; i <= 5; i++) {
            mplew.writeMapleAsciiString(g.getRankTitle(i));
        }
        Collection<MapleGuildCharacter> members = g.getMembers();
        mplew.write(members.size());
        //then it is the size of all the members
        for (MapleGuildCharacter mgc : members) { //and each of their character ids o_O
            mplew.writeInt(mgc.getId());
        }

        for (MapleGuildCharacter mgc : members) {
            mplew.writeAsciiString(StringUtil.getRightPaddedStr(mgc.getName(), '\0', 13));
            mplew.writeInt(mgc.getJobId());
            mplew.writeInt(mgc.getLevel());
            mplew.writeInt(mgc.getGuildRank());
            mplew.writeInt(mgc.isOnline() ? 1 : 0);
            mplew.writeInt(g.getSignature());
        }

        mplew.writeInt(g.getCapacity());
        mplew.writeShort(g.getLogoBG());
        mplew.write(g.getLogoBGColor());
        mplew.writeShort(g.getLogo());
        mplew.write(g.getLogoColor());
        mplew.writeMapleAsciiString(g.getNotice());
        mplew.writeInt(g.getGP());
        return mplew.getPacket();
    }

    public static MaplePacket guildMemberOnline(int gid, int cid, boolean bOnline) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x3d);
        mplew.writeInt(gid);
        mplew.writeInt(cid);
        mplew.write(bOnline ? 1 : 0);

        return mplew.getPacket();
    }

    public static MaplePacket guildInvite(int gid, String charName) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x05);
        mplew.writeInt(gid);
        mplew.writeMapleAsciiString(charName);

        return mplew.getPacket();
    }

    public static MaplePacket genericGuildMessage(byte code) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(code);

        return mplew.getPacket();
    }

    public static MaplePacket newGuildMember(MapleGuildCharacter mgc) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x27);

        mplew.writeInt(mgc.getGuildId());
        mplew.writeInt(mgc.getId());
        mplew.writeAsciiString(StringUtil.getRightPaddedStr(mgc.getName(), '\0', 13));
        mplew.writeInt(mgc.getJobId());
        mplew.writeInt(mgc.getLevel());
        mplew.writeInt(mgc.getGuildRank()); //should be always 5 but whatevs
        mplew.writeInt(mgc.isOnline() ? 1 : 0); //should always be 1 too
        mplew.writeInt(1); //? could be guild signature, but doesn't seem to matter

        return mplew.getPacket();
    }

    //someone leaving, mode == 0x2c for leaving, 0x2f for expelled
    public static MaplePacket memberLeft(MapleGuildCharacter mgc, boolean bExpelled) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(bExpelled ? 0x2f : 0x2c);

        mplew.writeInt(mgc.getGuildId());
        mplew.writeInt(mgc.getId());
        mplew.writeMapleAsciiString(mgc.getName());

        return mplew.getPacket();
    }

    //rank change
    public static MaplePacket changeRank(MapleGuildCharacter mgc) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x40);
        mplew.writeInt(mgc.getGuildId());
        mplew.writeInt(mgc.getId());
        mplew.write(mgc.getGuildRank());

        return mplew.getPacket();
    }

    public static MaplePacket guildNotice(int gid, String notice) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x44);
        mplew.writeInt(gid);
        mplew.writeMapleAsciiString(notice);
        return mplew.getPacket();
    }

    public static MaplePacket guildMemberLevelJobUpdate(MapleGuildCharacter mgc) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x3C);
        mplew.writeInt(mgc.getGuildId());
        mplew.writeInt(mgc.getId());
        mplew.writeInt(mgc.getLevel());
        mplew.writeInt(mgc.getJobId());
        return mplew.getPacket();
    }

    public static MaplePacket rankTitleChange(int gid, String[] ranks) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x3e);
        mplew.writeInt(gid);
        for (int i = 0; i < 5; i++) {
            mplew.writeMapleAsciiString(ranks[i]);
        }
        return mplew.getPacket();
    }

    public static MaplePacket guildDisband(int gid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x32);
        mplew.writeInt(gid);
        mplew.write(1);
        return mplew.getPacket();
    }

    public static MaplePacket guildEmblemChange(int gid, short bg, byte bgcolor, short logo, byte logocolor) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x42);
        mplew.writeInt(gid);
        mplew.writeShort(bg);
        mplew.write(bgcolor);
        mplew.writeShort(logo);
        mplew.write(logocolor);
        return mplew.getPacket();
    }

    public static MaplePacket guildCapacityChange(int gid, int capacity) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x3a);
        mplew.writeInt(gid);
        mplew.write(capacity);
        return mplew.getPacket();
    }

    //credit goes to Simon for this function, copied (with slight variation) out of his tempban
    //convert to EDT
    private final static long FT_UT_OFFSET = 116444592000000000L;

    private static long getKoreanTimestamp(long realTimestamp) {
        long time = (realTimestamp / 1000 / 60); //convert to minutes
        return ((time * 600000000) + FT_UT_OFFSET);
    }

    public static long getTime(long realTimestamp) {
        long time = (realTimestamp / 1000); // convert to seconds
        return ((time * 10000000) + FT_UT_OFFSET);
    }

    public static void addThread(MaplePacketLittleEndianWriter mplew, ResultSet rs) throws SQLException {
        mplew.writeInt(rs.getInt("localthreadid"));
        mplew.writeInt(rs.getInt("postercid"));
        mplew.writeMapleAsciiString(rs.getString("name"));
        mplew.writeLong(MaplePacketCreator.getKoreanTimestamp(rs.getLong("timestamp")));
        mplew.writeInt(rs.getInt("icon"));
        mplew.writeInt(rs.getInt("replycount"));
    }

    public static MaplePacket BBSThreadList(ResultSet rs, int start) throws SQLException {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendOpcode.BBS_OPERATION.getValue());
        mplew.write(0x06);

        if (!rs.last()) //no result at all
        {
            mplew.write(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            return mplew.getPacket();
        }

        int threadCount = rs.getRow();
        if (rs.getInt("localthreadid") == 0) //has a notice
        {
            mplew.write(1);
            addThread(mplew, rs);
            threadCount--; //one thread didn't count (because it's a notice)
        } else {
            mplew.write(0);
        }

        if (!rs.absolute(start + 1)) //seek to the thread before where we start
        {
            rs.first(); //uh, we're trying to start at a place past possible
            start = 0;
            // System.out.println("Attempting to start past threadCount");
        }

        mplew.writeInt(threadCount);
        mplew.writeInt(Math.min(10, threadCount - start));

        for (int i = 0; i < Math.min(10, threadCount - start); i++) {
            addThread(mplew, rs);
            rs.next();
        }

        return mplew.getPacket();
    }

    public static MaplePacket showThread(int localthreadid, ResultSet threadRS, ResultSet repliesRS)
            throws SQLException,
            RuntimeException {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendOpcode.BBS_OPERATION.getValue());
        mplew.write(0x07);

        mplew.writeInt(localthreadid);
        mplew.writeInt(threadRS.getInt("postercid"));
        mplew.writeLong(getKoreanTimestamp(threadRS.getLong("timestamp")));
        mplew.writeMapleAsciiString(threadRS.getString("name"));
        mplew.writeMapleAsciiString(threadRS.getString("startpost"));
        mplew.writeInt(threadRS.getInt("icon"));

        if (repliesRS != null) {
            int replyCount = threadRS.getInt("replycount");
            mplew.writeInt(replyCount);

            int i;
            for (i = 0; i < replyCount && repliesRS.next(); i++) {
                mplew.writeInt(repliesRS.getInt("replyid"));
                mplew.writeInt(repliesRS.getInt("postercid"));
                mplew.writeLong(getKoreanTimestamp(repliesRS.getLong("timestamp")));
                mplew.writeMapleAsciiString(repliesRS.getString("content"));
            }

            if (i != replyCount || repliesRS.next()) {
                //in the unlikely event that we lost count of replyid
                throw new RuntimeException(String.valueOf(threadRS.getInt("threadid")));
                //we need to fix the database and stop the packet sending
                //or else it'll probably error 38 whoever tries to read it

                //there is ONE case not checked, and that's when the thread 
                //has a replycount of 0 and there is one or more replies to the
                //thread in bbs_replies 
            }
        } else {
            mplew.writeInt(0); //0 replies
        }
        return mplew.getPacket();
    }

    public static MaplePacket showGuildRanks(int npcid, ResultSet rs) throws SQLException {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x49);
        mplew.writeInt(npcid);
        if (!rs.last()) //no guilds o.o
        {
            mplew.writeInt(0);
            return mplew.getPacket();
        }

        mplew.writeInt(rs.getRow());		//number of entries

        rs.beforeFirst();
        while (rs.next()) {
            mplew.writeMapleAsciiString(rs.getString("name"));
            mplew.writeInt(rs.getInt("GP"));
            mplew.writeInt(rs.getInt("logo"));
            mplew.writeInt(rs.getInt("logoColor"));
            mplew.writeInt(rs.getInt("logoBG"));
            mplew.writeInt(rs.getInt("logoBGColor"));
        }

        return mplew.getPacket();
    }

    public static MaplePacket updateGP(int gid, int GP) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x48);
        mplew.writeInt(gid);
        mplew.writeInt(GP);
        return mplew.getPacket();
    }

    public static MaplePacket skillEffect(MapleCharacter from, int skillId, byte flags) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SKILL_EFFECT.getValue());
        mplew.writeInt(from.getId());
        mplew.writeInt(skillId);
        mplew.write(0x01); // unknown at this point
        mplew.write(flags);
        mplew.write(0x04); // unknown at this point
        return mplew.getPacket();
    }

    public static MaplePacket skillCancel(MapleCharacter from, int skillId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CANCEL_SKILL_EFFECT.getValue());
        mplew.writeInt(from.getId());
        mplew.writeInt(skillId);
        return mplew.getPacket();
    }
}
