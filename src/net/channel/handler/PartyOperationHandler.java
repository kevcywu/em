package net.channel.handler;

import java.rmi.RemoteException;

import client.MapleCharacter;
import client.MapleClient;
import net.AbstractMaplePacketHandler;
import net.channel.ChannelServer;
import net.world.MapleParty;
import net.world.MaplePartyCharacter;
import net.world.PartyOperation;
import net.world.remote.WorldChannelInterface;
import net.packetcreator.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class PartyOperationHandler extends AbstractMaplePacketHandler {
    // private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PartyOperationHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int operation = slea.readByte();
        MapleCharacter player = c.getPlayer();
        WorldChannelInterface wci = ChannelServer.getInstance(c.getChannel()).getWorldInterface();
        MapleParty party = player.getParty();
        MaplePartyCharacter partyplayer = new MaplePartyCharacter(player);

        switch (operation) {
            case 1: { // create
                if (c.getPlayer().getParty() == null) {
                    try {
                        party = wci.createParty(partyplayer);
                        player.setParty(party);
                    } catch (RemoteException e) {
                        c.getChannelServer().reconnectWorld();
                    }
                    c.getSession().write(MaplePacketCreator.partyCreated());
                } else {
                    c.getSession().write(MaplePacketCreator.serverNotice(5, "You can't create a party as you are already in one"));
                }
                break;
            }
            case 2: { // leave
                if (party != null) { //are we in a party? o.O"
                    try {
                        if (partyplayer.equals(party.getLeader())) { // disband
                            wci.updateParty(party.getId(), PartyOperation.DISBAND, partyplayer);
                            if (player.getEventInstance() != null) {
                                player.getEventInstance().disbandParty();
                            }
                        } else {
                            wci.updateParty(party.getId(), PartyOperation.LEAVE, partyplayer);
                            if (player.getEventInstance() != null) {
                                player.getEventInstance().leftParty(player);
                            }
                        }
                    } catch (RemoteException e) {
                        c.getChannelServer().reconnectWorld();
                    }
                    player.setParty(null);
                }
                break;
            }
            case 3: { // accept invitation
                int partyid = slea.readInt();
                if (c.getPlayer().getParty() == null) {
                    try {
                        party = wci.getParty(partyid);
                        if (party != null) {
                            if (party.getMembers().size() < 6) {
                                wci.updateParty(party.getId(), PartyOperation.JOIN, partyplayer);
                                player.receivePartyMemberHP();
                                player.updatePartyMemberHP();
                            } else {
                                c.getSession().write(MaplePacketCreator.partyStatusMessage(17));
                            }
                        } else {
                            c.getSession().write(MaplePacketCreator.serverNotice(5, "The party you are trying to join does not exist"));
                        }
                    } catch (RemoteException e) {
                        c.getChannelServer().reconnectWorld();
                    }
                } else {
                    c.getSession().write(MaplePacketCreator.serverNotice(5, "You can't join the party as you are already in one"));
                }
                break;
            }
            case 4: { // invite
                //TODO store pending invitations and check against them
                String name = slea.readMapleAsciiString();
                MapleCharacter invited = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
                if (invited != null) {
                    if (invited.getParty() == null) {
                        if (party.getMembers().size() < 6) {
                            invited.getClient().getSession().write(MaplePacketCreator.partyInvite(player));
                        }
                    } else {
                        c.getSession().write(MaplePacketCreator.partyStatusMessage(16));
                    }
                } else {
                    c.getSession().write(MaplePacketCreator.partyStatusMessage(18));
                }
                break;
            }
            case 5: { // expel
                int cid = slea.readInt();
                if (partyplayer.equals(party.getLeader())) {
                    MaplePartyCharacter expelled = party.getMemberById(cid);
                    if (expelled != null) {
                        try {
                            wci.updateParty(party.getId(), PartyOperation.EXPEL, expelled);
                            if (player.getEventInstance() != null) {
                                /*if leader wants to boot someone, then the whole party gets expelled
								TODO: Find an easier way to get the character behind a MaplePartyCharacter
								possibly remove just the expellee.*/
                                player.getEventInstance().disbandParty();
                            }

                        } catch (RemoteException e) {
                            c.getChannelServer().reconnectWorld();
                        }
                    }
                }
                break;
            }
        }
    }
}
