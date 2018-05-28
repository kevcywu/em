package net.channel;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import client.BuddyList;
import client.BuddylistEntry;
import client.MapleCharacter;
import client.BuddyList.BuddyAddResult;
import client.BuddyList.BuddyOperation;
import database.DatabaseConnection;
import net.ByteArrayMaplePacket;
import net.MaplePacket;
import net.channel.remote.ChannelWorldInterface;
import net.world.MapleParty;
import net.world.MaplePartyCharacter;
import net.world.PartyOperation;
import net.world.guild.MapleGuildSummary;
import net.world.remote.CheaterData;
import server.ShutdownServer;
import server.TimerManager;
import tools.CollectionUtil;
import net.packetcreator.MaplePacketCreator;

public class ChannelWorldInterfaceImpl extends UnicastRemoteObject implements ChannelWorldInterface {

    private static final long serialVersionUID = 7815256899088644192L;

    private ChannelServer server;

    public ChannelWorldInterfaceImpl() throws RemoteException {
        super(0, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory());
    }

    public ChannelWorldInterfaceImpl(ChannelServer server) throws RemoteException {
        super(0, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory());
        this.server = server;
    }

    @Override
    public void setChannelId(int id) throws RemoteException {
        server.setChannel(id);
    }

    @Override
    public int getChannelId() throws RemoteException {
        return server.getChannel();
    }

    @Override
    public String getIP() throws RemoteException {
        return server.getIP();
    }

    @Override
    public void broadcastMessage(String sender, byte[] message) throws RemoteException {
        MaplePacket packet = new ByteArrayMaplePacket(message);
        server.broadcastPacket(packet);
    }

    @Override
    public void whisper(String sender, String target, int channel, String message) throws RemoteException {
        if (isConnected(target)) {
            server.getPlayerStorage().getCharacterByName(target).getClient().announce(MaplePacketCreator.getWhisper(sender, channel, message));
        }
    }

    @Override
    public boolean isConnected(String charName) throws RemoteException {
        return server.getPlayerStorage().getCharacterByName(charName) != null;
    }

    @Override
    public void shutdown(int time) throws RemoteException {
        server.broadcastPacket(MaplePacketCreator.serverNotice(0, "The world will be shut down in " + (time / 60000) + " minutes, please log off safely"));
        TimerManager.getInstance().schedule(new ShutdownServer(server.getChannel()), time);
    }

    @Override
    public int getConnected() throws RemoteException {
        return server.getConnectedClients();
    }

    @Override
    public void loggedOff(String name, int characterId, int channel, int[] buddies) throws RemoteException {
        updateBuddies(characterId, channel, buddies, true);
    }

    @Override
    public void loggedOn(String name, int characterId, int channel, int buddies[]) throws RemoteException {
        updateBuddies(characterId, channel, buddies, false);
    }

    private void updateBuddies(int characterId, int channel, int[] buddies, boolean offline) {
        IPlayerStorage playerStorage = server.getPlayerStorage();
        for (int buddy : buddies) {
            MapleCharacter chr = playerStorage.getCharacterById(buddy);
            if (chr != null) {
                BuddylistEntry ble = chr.getBuddylist().get(characterId);
                if (ble != null && ble.isVisible()) {
                    int mcChannel;
                    if (offline) {
                        ble.setChannel(-1);
                        mcChannel = -1;
                    } else {
                        ble.setChannel(channel);
                        mcChannel = channel - 1;
                    }
                    chr.getBuddylist().put(ble);
                    chr.getClient().announce(MaplePacketCreator.updateBuddyChannel(ble.getCharacterId(), mcChannel));
                }
            }
        }
    }

    @Override
    public void updateParty(MapleParty party, PartyOperation operation, MaplePartyCharacter target) throws RemoteException {
        for (MaplePartyCharacter partychar : party.getMembers()) {
            if (partychar.getChannel() == server.getChannel()) {
                MapleCharacter chr = server.getPlayerStorage().getCharacterByName(partychar.getName());
                if (chr != null) {
                    chr.setParty((operation == PartyOperation.DISBAND) ? null : party);
                    chr.getClient().announce(MaplePacketCreator.updateParty(chr.getClient().getChannel(), party, operation, target));
                }
            }
        }
        switch (operation) {
            case LEAVE:
            case EXPEL:
                if (target.getChannel() == server.getChannel()) {
                    MapleCharacter chr = server.getPlayerStorage().getCharacterByName(target.getName());
                    if (chr != null) {
                        chr.getClient().announce(MaplePacketCreator.updateParty(chr.getClient().getChannel(), party, operation, target));
                        chr.setParty(null);
                    }
                }
        }
    }

    @Override
    public void partyChat(MapleParty party, String chattext, String namefrom) throws RemoteException {
        for (MaplePartyCharacter partychar : party.getMembers()) {
            if (partychar.getChannel() == server.getChannel() && !(partychar.getName().equals(namefrom))) {
                MapleCharacter chr = server.getPlayerStorage().getCharacterByName(partychar.getName());
                if (chr != null) {
                    chr.getClient().announce(MaplePacketCreator.multiChat(namefrom, chattext, 1));
                }
            }
        }
    }

    @Override
    public boolean isAvailable() throws RemoteException {
        return true;
    }

    @Override
    public int getLocation(String name) throws RemoteException {
        MapleCharacter chr = server.getPlayerStorage().getCharacterByName(name);
        return (chr != null) ? server.getPlayerStorage().getCharacterByName(name).getMapId() : -1;
    }

    @Override
    public BuddyAddResult requestBuddyAdd(String addName, int channelFrom, int cidFrom, String nameFrom) {
        MapleCharacter addChar = server.getPlayerStorage().getCharacterByName(addName);
        if (addChar != null) {
            BuddyList buddylist = addChar.getBuddylist();
            if (buddylist.isFull()) {
                return BuddyAddResult.BUDDYLIST_FULL;
            }
            if (!buddylist.contains(cidFrom)) {
                buddylist.addBuddyRequest(addChar.getClient(), cidFrom, nameFrom, channelFrom);
            } else {
                if (buddylist.containsVisible(cidFrom)) {
                    return BuddyAddResult.ALREADY_ON_LIST;
                }
            }
        }
        return BuddyAddResult.OK;
    }

    @Override
    public boolean isConnected(int characterId) throws RemoteException {
        return server.getPlayerStorage().getCharacterById(characterId) != null;
    }

    @Override
    public void buddyChanged(int cid, int cidFrom, String name, int channel, BuddyOperation operation) {
        MapleCharacter addChar = server.getPlayerStorage().getCharacterById(cid);
        if (addChar != null) {
            BuddyList buddylist = addChar.getBuddylist();
            switch (operation) {
                case ADDED:
                    if (buddylist.contains(cidFrom)) {
                        buddylist.put(new BuddylistEntry(name, cidFrom, channel, true));
                        addChar.getClient().announce(MaplePacketCreator.updateBuddyChannel(cidFrom, channel - 1));
                    }
                    break;
                case DELETED:
                    if (buddylist.contains(cidFrom)) {
                        buddylist.put(new BuddylistEntry(name, cidFrom, -1, buddylist.get(cidFrom).isVisible()));
                        addChar.getClient().announce(MaplePacketCreator.updateBuddyChannel(cidFrom, -1));
                    }
                    break;
            }
        }
    }

    @Override
    public void buddyChat(int[] recipientCharacterIds, int cidFrom, String nameFrom, String chattext) throws RemoteException {
        IPlayerStorage playerStorage = server.getPlayerStorage();
        for (int characterId : recipientCharacterIds) {
            MapleCharacter chr = playerStorage.getCharacterById(characterId);
            if (chr != null) {
                if (chr.getBuddylist().containsVisible(cidFrom)) {
                    chr.getClient().announce(MaplePacketCreator.multiChat(nameFrom, chattext, 0));
                }
            }
        }
    }

    @Override
    public int[] multiBuddyFind(int charIdFrom, int[] characterIds) throws RemoteException {
        List<Integer> ret = new ArrayList<>(characterIds.length);
        IPlayerStorage playerStorage = server.getPlayerStorage();
        for (int characterId : characterIds) {
            MapleCharacter chr = playerStorage.getCharacterById(characterId);
            if (chr != null) {
                if (chr.getBuddylist().containsVisible(charIdFrom)) {
                    ret.add(characterId);
                }
            }
        }
        int[] retArr = new int[ret.size()];
        int pos = 0;
        for (Integer i : ret) {
            retArr[pos++] = i;
        }
        return retArr;
    }

    @Override
    public void sendPacket(List<Integer> targetIds, MaplePacket packet, int exception) throws RemoteException {
        MapleCharacter c;
        for (int i : targetIds) {
            if (i == exception) {
                continue;
            }
            c = server.getPlayerStorage().getCharacterById(i);
            if (c != null) {
                c.getClient().announce(packet);
            }
        }
    }

    @Override
    public void setGuildAndRank(List<Integer> cids, int guildid, int rank, int exception) throws RemoteException {
        for (int cid : cids) {
            if (cid != exception) {
                setGuildAndRank(cid, guildid, rank);
            }
        }
    }

    @Override
    public void setGuildAndRank(int cid, int guildid, int rank) throws RemoteException {
        MapleCharacter mc = server.getPlayerStorage().getCharacterById(cid);
        if (mc == null) {
            // System.out.println("ERROR: cannot find player in given channel");
            return;
        }

        boolean bDifferentGuild;
        if (guildid == -1 && rank == -1) { //just need a respawn
            bDifferentGuild = true;
        } else {
            bDifferentGuild = guildid != mc.getGuildId();
            mc.setGuildId(guildid);
            mc.setGuildRank(rank);
            mc.saveGuildStatus();
        }

        if (bDifferentGuild) {
            mc.getMap().broadcastMessage(mc, MaplePacketCreator.removePlayerFromMap(cid), false);
            mc.getMap().broadcastMessage(mc, MaplePacketCreator.spawnPlayerMapobject(mc), false);
        }
    }

    @Override
    public void setOfflineGuildStatus(int guildid, byte guildrank, int cid) throws RemoteException {
        Logger log = LoggerFactory.getLogger(this.getClass());
        try {
            java.sql.Connection con = DatabaseConnection.getConnection();
            java.sql.PreparedStatement ps = con.prepareStatement("UPDATE characters SET guildid = ?, guildrank = ? WHERE id = ?");
            ps.setInt(1, guildid);
            ps.setInt(2, guildrank);
            ps.setInt(3, cid);
            ps.execute();
            ps.close();
        } catch (SQLException se) {
            log.error("SQLException: " + se.getLocalizedMessage(), se);
        }
    }

    @Override
    public void reloadGuildCharacters() throws RemoteException {
        for (MapleCharacter mc : server.getPlayerStorage().getAllCharacters()) {
            if (mc.getGuildId() > 0) {
                //multiple world ops, but this method is ONLY used 
                //in !clearguilds gm command, so it shouldn't be a problem
                server.getWorldInterface().setGuildMemberOnline(mc.getMGC(), true, server.getChannel());
                server.getWorldInterface().memberLevelJobUpdate(mc.getMGC());
            }
        }
        ChannelServer.getInstance(this.getChannelId()).reloadGuildSummary();
    }

    @Override
    public void changeEmblem(int gid, List<Integer> affectedPlayers, MapleGuildSummary mgs) throws RemoteException {
        ChannelServer.getInstance(this.getChannelId()).updateGuildSummary(gid, mgs);
        this.sendPacket(affectedPlayers, MaplePacketCreator.guildEmblemChange(gid, mgs.getLogoBG(), mgs.getLogoBGColor(), mgs.getLogo(), mgs.getLogoColor()), -1);
        this.setGuildAndRank(affectedPlayers, -1, -1, -1);	//respawn player
    }

}
