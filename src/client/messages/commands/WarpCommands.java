package client.messages.commands;

import java.net.InetAddress;

import client.MapleCharacter;
import client.MapleClient;
import client.messages.Command;
import client.messages.CommandDefinition;
import client.messages.IllegalCommandSyntaxException;
import client.messages.MessageCallback;
import net.sf.MaplePacket;
import net.sf.channel.ChannelServer;
import net.sf.world.remote.WorldLocation;
import server.MaplePortal;
import server.MapleTrade;
import server.maps.MapleMap;
import tools.MaplePacketCreator;

public class WarpCommands implements Command {

    @Override
    public void execute(MapleClient c, MessageCallback mc, String[] splitted) throws Exception,
            IllegalCommandSyntaxException {
        ChannelServer cserv = c.getChannelServer();
        if (splitted[0].equals("!warp")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null) {
                if (splitted.length == 2) {
                    MapleMap target = victim.getMap();
                    c.getPlayer().changeMap(target, target.findClosestSpawnpoint(victim.getPosition()));
                } else {
                    int mapid = Integer.parseInt(splitted[2]);
                    MapleMap target = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(mapid);
                    victim.changeMap(target, target.getPortal(0));
                }
            } else {
                try {
                    victim = c.getPlayer();
                    WorldLocation loc = c.getChannelServer().getWorldInterface().getLocation(splitted[1]);
                    if (loc != null) {
                        mc.dropMessage("You will be cross-channel warped. This may take a few seconds.");
                        // WorldLocation loc = new WorldLocation(40000, 2);
                        MapleMap target = c.getChannelServer().getMapFactory().getMap(loc.map);
                        c.getPlayer().cancelAllBuffs();
                        String ip = c.getChannelServer().getIP(loc.channel);
                        c.getPlayer().getMap().removePlayer(c.getPlayer());
                        victim.setMap(target);
                        String[] socket = ip.split(":");
                        if (c.getPlayer().getTrade() != null) {
                            MapleTrade.cancelTrade(c.getPlayer());
                        }
                        c.getPlayer().saveToDB(true);
                        if (c.getPlayer().getCheatTracker() != null) {
                            c.getPlayer().getCheatTracker().dispose();
                        }
                        ChannelServer.getInstance(c.getChannel()).removePlayer(c.getPlayer());
                        c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION);
                        try {
                            MaplePacket packet = MaplePacketCreator.getChannelChange(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1]));
                            c.getSession().write(packet);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        int map = Integer.parseInt(splitted[1]);
                        MapleMap target = cserv.getMapFactory().getMap(map);
                        c.getPlayer().changeMap(target, target.getPortal(0));
                    }
                } catch (/* Remote */Exception e) {
                    mc.dropMessage("Something went wrong " + e.getMessage());
                }
            }
        } else if (splitted[0].equals("!warphere")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            victim.changeMap(c.getPlayer().getMap(), c.getPlayer().getMap().findClosestSpawnpoint(c.getPlayer().getPosition()));
        } else if (splitted[0].equals("!lolcastle")) {
            if (splitted.length != 2) {
                mc.dropMessage("Syntax: !lolcastle level (level = 1-5)");
            }
            MapleMap target = c.getChannelServer().getEventSM().getEventManager("lolcastle").getInstance("lolcastle"
                    + splitted[1]).getMapFactory().getMap(990000300, false, false);
            c.getPlayer().changeMap(target, target.getPortal(0));
        } else if (splitted[0].equals("!jail")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            int mapid = 200090300; // mulung ride
            if (splitted.length > 2 && splitted[1].equals("2")) {
                mapid = 980000404; // exit for CPQ; not used
                victim = cserv.getPlayerStorage().getCharacterByName(splitted[2]);
            }
            if (victim != null) {
                MapleMap target = cserv.getMapFactory().getMap(mapid);
                MaplePortal targetPortal = target.getPortal(0);
                victim.changeMap(target, targetPortal);
                mc.dropMessage(victim.getName() + " was jailed!");
            } else {
                mc.dropMessage(splitted[1] + " not found!");
            }
        } else if (splitted[0].equals("!map")) {
            int mapid = Integer.parseInt(splitted[1]);
            MapleMap target = cserv.getMapFactory().getMap(mapid);
            MaplePortal targetPortal = null;
            if (splitted.length > 2) {
                try {
                    targetPortal = target.getPortal(Integer.parseInt(splitted[2]));
                } catch (IndexOutOfBoundsException ioobe) {
                    // noop, assume the gm didn't know how many portals there are
                } catch (NumberFormatException nfe) {
                    // noop, assume that the gm is drunk
                }
            }
            if (targetPortal == null) {
                targetPortal = target.getPortal(0);
            }
            c.getPlayer().changeMap(target, targetPortal);
        }
    }

    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[]{
            new CommandDefinition("warp", "playername [targetid]", "Warps yourself to the player with the given name. When targetid is specified warps the player to the given mapid", 100),
            new CommandDefinition("warphere", "playername", "Warps the player with the given name to yourself", 100),
            new CommandDefinition("lolcastle", "[1-5]", "Warps you into Field of Judgement with the given level", 100),
            new CommandDefinition("jail", "[2] playername", "Warps the player to a map that he can't leave", 100),
            new CommandDefinition("map", "mapid", "Warps you to the given mapid (use /m instead)", 100),};
    }

}
