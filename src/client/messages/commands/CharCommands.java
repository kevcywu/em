package client.messages.commands;

import static client.messages.CommandProcessor.getOptionalIntArg;
import client.IItem;
import client.Item;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleInventoryType;
import client.MapleJob;
import client.MapleStat;
import client.SkillFactory;
import client.messages.Command;
import client.messages.CommandDefinition;
import client.messages.IllegalCommandSyntaxException;
import client.messages.MessageCallback;
import client.messages.ServernoticeMapleClientMessageCallback;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleShop;
import server.MapleShopFactory;

public class CharCommands implements Command {

    @Override
    public void execute(MapleClient c, MessageCallback mc, String[] splitted) throws Exception,
            IllegalCommandSyntaxException {
        MapleCharacter player = c.getPlayer();
        if (splitted[0].equals("!lowhp")) {
            player.setHp(1);
            player.setMp(500);
            player.updateSingleStat(MapleStat.HP, 1);
            player.updateSingleStat(MapleStat.MP, 500);
        } else if (splitted[0].equals("!fullhp")) {
            player.setHp(player.getMaxHp());
            player.updateSingleStat(MapleStat.HP, player.getMaxHp());
        } else if (splitted[0].equals("!skill")) {
            int skill = Integer.parseInt(splitted[1]);
            int level = getOptionalIntArg(splitted, 2, 1);
            int masterlevel = getOptionalIntArg(splitted, 3, 1);
            c.getPlayer().changeSkillLevel(SkillFactory.getSkill(skill), level, masterlevel);
        } else if (splitted[0].equals("!sp")) {
            player.setRemainingSp(getOptionalIntArg(splitted, 1, 1));
            player.updateSingleStat(MapleStat.AVAILABLESP, player.getRemainingSp());
        } else if (splitted[0].equals("!job")) {
            c.getPlayer().changeJob(MapleJob.getById(Integer.parseInt(splitted[1])));
        } else if (splitted[0].equals("!whereami")) {
            new ServernoticeMapleClientMessageCallback(c).dropMessage("You are on map "
                    + c.getPlayer().getMap().getId());
        } else if (splitted[0].equals("!shop")) {
            MapleShopFactory sfact = MapleShopFactory.getInstance();
            MapleShop shop = sfact.getShop(getOptionalIntArg(splitted, 1, 1));
            shop.sendShop(c);
        } else if (splitted[0].equals("!levelup")) {
            c.getPlayer().levelUp();
            int newexp = c.getPlayer().getExp();
            if (newexp < 0) {
                c.getPlayer().gainExp(-newexp, false, false);
            }
        } else if (splitted[0].equals("!item")) {
            short quantity = (short) getOptionalIntArg(splitted, 2, 1);
            MapleInventoryManipulator.addById(c, Integer.parseInt(splitted[1]), quantity, c.getPlayer().getName()
                    + "used !item with quantity " + quantity, player.getName());
        } else if (splitted[0].equals("!drop")) {
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            int itemId = Integer.parseInt(splitted[1]);
            short quantity = (short) (short) getOptionalIntArg(splitted, 2, 1);
            IItem toDrop;
            if (ii.getInventoryType(itemId) == MapleInventoryType.EQUIP) {
                toDrop = ii.getEquipById(itemId);
            } else {
                toDrop = new Item(itemId, (byte) 0, (short) quantity);
            }
            StringBuilder logMsg = new StringBuilder("Created by ");
            logMsg.append(c.getPlayer().getName());
            logMsg.append(" using !drop. Quantity: ");
            logMsg.append(quantity);
            toDrop.log(logMsg.toString(), false);
            toDrop.setOwner(player.getName());
            c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), toDrop, c.getPlayer().getPosition(), true, true);
        }
    }

    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[]{
            new CommandDefinition("lowhp", "", "", 100),
            new CommandDefinition("fullhp", "", "", 100),
            new CommandDefinition("skill", "", "", 100),
            new CommandDefinition("sp", "", "", 100),
            new CommandDefinition("job", "", "", 100),
            new CommandDefinition("whereami", "", "", 100),
            new CommandDefinition("shop", "", "", 100),
            new CommandDefinition("levelup", "", "", 100),
            new CommandDefinition("item", "", "", 100),
            new CommandDefinition("drop", "", "", 100),};
    }

}
