package client.messages.commands;

import java.awt.Point;

import client.MapleCharacter;
import client.MapleClient;
import client.anticheat.CheatingOffense;
import client.messages.Command;
import client.messages.CommandDefinition;
import client.messages.IllegalCommandSyntaxException;
import client.messages.MessageCallback;
import server.MaplePortal;
import server.TimerManager;
import server.maps.MapleDoor;
import server.quest.MapleQuest;
import tools.HexTool;
import tools.MaplePacketCreator;
import tools.data.output.MaplePacketLittleEndianWriter;

public class DebugCommands implements Command {

    @Override
    public void execute(MapleClient c, MessageCallback mc, String[] splitted) throws Exception,
            IllegalCommandSyntaxException {
        MapleCharacter player = c.getPlayer();
        if (splitted[0].equals("!resetquest")) {
            MapleQuest.getInstance(Integer.parseInt(splitted[1])).forfeit(c.getPlayer());
        } else if (splitted[0].equals("!nearestPortal")) {
            final MaplePortal portal = player.getMap().findClosestSpawnpoint(player.getPosition());
            mc.dropMessage(portal.getName() + " id: " + portal.getId() + " script: " + portal.getScriptName());
        } else if (splitted[0].equals("!spawndebug")) {
            c.getPlayer().getMap().spawnDebug(mc);
        } else if (splitted[0].equals("!door")) {
            Point doorPos = new Point(player.getPosition());
            doorPos.y -= 270;
            MapleDoor door = new MapleDoor(c.getPlayer(), doorPos);
            door.getTarget().addMapObject(door);
            // c.getSession().write(MaplePacketCreator.spawnDoor(/*c.getPlayer().getId()*/ 0x1E47, door.getPosition(),
            // false));
            /* c.getSession().write(MaplePacketCreator.saveSpawnPosition(door.getPosition())); */
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(HexTool.getByteArrayFromHexString("B9 00 00 47 1E 00 00 0A 04 76 FF"));
            c.getSession().write(mplew.getPacket());
            mplew = new MaplePacketLittleEndianWriter();
            mplew.write(HexTool.getByteArrayFromHexString("36 00 00 EF 1C 0D 4C 3E 1D 0D 0A 04 76 FF"));
            c.getSession().write(mplew.getPacket());
            c.getSession().write(MaplePacketCreator.enableActions());
            door = new MapleDoor(door);
            door.getTown().addMapObject(door);
        } else if (splitted[0].equals("!timerdebug")) {
            TimerManager.getInstance().dropDebugInfo(mc);
        } else if (splitted[0].equals("!threads")) {
            Thread[] threads = new Thread[Thread.activeCount()];
            Thread.enumerate(threads);
            String filter = "";
            if (splitted.length > 1) {
                filter = splitted[1];
            }
            for (int i = 0; i < threads.length; i++) {
                String tstring = threads[i].toString();
                if (tstring.toLowerCase().indexOf(filter.toLowerCase()) > -1) {
                    mc.dropMessage(i + ": " + tstring);
                }
            }
        } else if (splitted[0].equals("!showtrace")) {
            if (splitted.length < 2) {
                throw new IllegalCommandSyntaxException(2);
            }
            Thread[] threads = new Thread[Thread.activeCount()];
            Thread.enumerate(threads);
            Thread t = threads[Integer.parseInt(splitted[1])];
            mc.dropMessage(t.toString() + ":");
            for (StackTraceElement elem : t.getStackTrace()) {
                mc.dropMessage(elem.toString());
            }
        } else if (splitted[0].equals("!fakerelog")) {
            c.getSession().write(MaplePacketCreator.getCharInfo(player));
            player.getMap().removePlayer(player);
            player.getMap().addPlayer(player);
        } else if (splitted[0].equals("!toggleoffense")) {
            try {
                CheatingOffense co = CheatingOffense.valueOf(splitted[1]);
                co.setEnabled(!co.isEnabled());
            } catch (IllegalArgumentException iae) {
                mc.dropMessage("Offense " + splitted[1] + " not found");
            }
        } else if (splitted[0].equals("!tdrops")) {
            player.getMap().toggleDrops();
        }
    }

    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[]{
            new CommandDefinition("resetquest", "", "", 1000),
            new CommandDefinition("nearestPortal", "", "", 1000),
            new CommandDefinition("spawndebug", "", "", 1000),
            new CommandDefinition("timerdebug", "", "", 1000),
            new CommandDefinition("threads", "", "", 1000),
            new CommandDefinition("showtrace", "", "", 1000),
            new CommandDefinition("toggleoffense", "", "", 1000),
            new CommandDefinition("fakerelog", "", "", 1000),
            new CommandDefinition("tdrops", "", "", 100),};
    }

}
