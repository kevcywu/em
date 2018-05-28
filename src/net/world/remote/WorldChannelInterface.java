package net.world.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Properties;

import net.channel.remote.ChannelWorldInterface;
import net.world.CharacterIdChannelPair;
import net.world.MapleParty;
import net.world.MaplePartyCharacter;
import net.world.PartyOperation;
import net.world.guild.MapleGuild;
import net.world.guild.MapleGuildCharacter;

public interface WorldChannelInterface extends Remote, WorldChannelCommonOperations {

    public Properties getDatabaseProperties() throws RemoteException;

    public Properties getGameProperties() throws RemoteException;

    public void serverReady() throws RemoteException;

    public String getIP(int channel) throws RemoteException;

    public int find(String charName) throws RemoteException;

    public int find(int characterId) throws RemoteException;

    public Map<Integer, Integer> getConnected() throws RemoteException;

    MapleParty createParty(MaplePartyCharacter chrfor) throws RemoteException;

    MapleParty getParty(int partyid) throws RemoteException;

    public void updateParty(int partyid, PartyOperation operation, MaplePartyCharacter target) throws RemoteException;

    public void partyChat(int partyid, String chattext, String namefrom) throws RemoteException;

    public boolean isAvailable() throws RemoteException;

    public ChannelWorldInterface getChannelInterface(int channel) throws RemoteException;

    public WorldLocation getLocation(String name) throws RemoteException;

    public CharacterIdChannelPair[] multiBuddyFind(int charIdFrom, int[] characterIds) throws RemoteException;

    public MapleGuild getGuild(int id, MapleGuildCharacter mgc) throws RemoteException;

    public void clearGuilds() throws RemoteException;

    public void setGuildMemberOnline(MapleGuildCharacter mgc, boolean bOnline, int channel) throws RemoteException;

    public int addGuildMember(MapleGuildCharacter mgc) throws RemoteException;

    public void leaveGuild(MapleGuildCharacter mgc) throws RemoteException;

    public void guildChat(int gid, String name, int cid, String msg) throws RemoteException;

    public void changeRank(int gid, int cid, int newRank) throws RemoteException;

    public void expelMember(MapleGuildCharacter initiator, String name, int cid) throws RemoteException;

    public void setGuildNotice(int gid, String notice) throws RemoteException;

    public void memberLevelJobUpdate(MapleGuildCharacter mgc) throws RemoteException;

    public void changeRankTitle(int gid, String[] ranks) throws RemoteException;

    public int createGuild(int leaderId, String name) throws RemoteException;

    public void setGuildEmblem(int gid, short bg, byte bgcolor, short logo, byte logocolor) throws RemoteException;

    public void disbandGuild(int gid) throws RemoteException;

    public boolean increaseGuildCapacity(int gid) throws RemoteException;

    public void gainGP(int gid, int amount) throws RemoteException;
}
