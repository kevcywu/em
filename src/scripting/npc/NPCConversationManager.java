package scripting.npc;

import java.util.LinkedList;
import java.util.List;

import client.IItem;
import client.Item;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleInventory;
import client.MapleInventoryType;
import client.MapleJob;
import client.MapleSkinColor;
import client.MapleStat;
import client.SkillFactory;
import net.world.guild.MapleGuild;
import scripting.AbstractPlayerInteraction;
import scripting.event.EventManager;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleShopFactory;
import server.quest.MapleQuest;
import net.packetcreator.MaplePacketCreator;
import net.packetcreator.NPCPacketCreator;

public class NPCConversationManager extends AbstractPlayerInteraction {

    private final int npc;
    private String getText;

    public NPCConversationManager(MapleClient c, int npc) {
        super(c);
        this.npc = npc;
    }

    public void dispose() {
        NPCScriptManager.getInstance().dispose(this);
    }

    public void sendNext(String text) {
        getClient().announce(NPCPacketCreator.getNPCTalk(npc, (byte) 0, text, "00 01"));
    }

    public void sendPrev(String text) {
        getClient().announce(NPCPacketCreator.getNPCTalk(npc, (byte) 0, text, "01 00"));
    }

    public void sendNextPrev(String text) {
        getClient().announce(NPCPacketCreator.getNPCTalk(npc, (byte) 0, text, "01 01"));
    }

    public void sendOk(String text) {
        getClient().announce(NPCPacketCreator.getNPCTalk(npc, (byte) 0, text, "00 00"));
    }

    public void sendYesNo(String text) {
        getClient().announce(NPCPacketCreator.getNPCTalk(npc, (byte) 1, text, ""));
    }

    public void sendAcceptDecline(String text) {
        getClient().announce(NPCPacketCreator.getNPCTalk(npc, (byte) 2, text, ""));
    }

    public void sendSimple(String text) {
        getClient().announce(NPCPacketCreator.getNPCTalk(npc, (byte) 5, text, ""));
    }

    public void sendStyle(String text, int styles[]) {
        getClient().announce(NPCPacketCreator.getNPCTalkStyle(npc, text, styles));
    }

    public void sendGetNumber(String text, int def, int min, int max) {
        getClient().announce(NPCPacketCreator.getNPCTalkNum(npc, text, def, min, max));
    }

    public void sendGetText(String text) {
        getClient().announce(NPCPacketCreator.getNPCTalkText(npc, text));
    }

    public void setGetText(String text) {
        this.getText = text;
    }

    public String getText() {
        return this.getText;
    }

    public void openShop(int id) {
        MapleShopFactory.getInstance().getShop(id).sendShop(getClient());
    }

    public void changeJob(MapleJob job) {
        getPlayer().changeJob(job);
    }

    public MapleJob getJob() {
        return getPlayer().getJob();
    }

    public void startQuest(int id) {
        MapleQuest.getInstance(id).start(getPlayer(), npc);
    }

    public void completeQuest(int id) {
        MapleQuest.getInstance(id).complete(getPlayer(), npc);
    }

    public void forfeitQuest(int id) {
        MapleQuest.getInstance(id).forfeit(getPlayer());
    }

    /**
     * use getPlayer().getMeso() instead
     *
     * @return
     */
    @Deprecated
    public int getMeso() {
        return getPlayer().getMeso();
    }

    public void gainMeso(int gain) {
        getPlayer().gainMeso(gain, true, false, true);
    }

    public void gainExp(int gain) {
        getPlayer().gainExp(gain, true, true);
    }

    public int getNpc() {
        return npc;
    }

    /**
     * use getPlayer().getLevel() instead
     *
     * @return
     */
    @Deprecated
    public int getLevel() {
        return getPlayer().getLevel();
    }

    public void unequipEverything() {
        MapleInventory equipped = getChar().getInventory(MapleInventoryType.EQUIPPED);
        MapleInventory equip = getChar().getInventory(MapleInventoryType.EQUIP);
        List<Byte> ids = new LinkedList<Byte>();
        for (IItem item : equipped.list()) {
            ids.add(item.getPosition());
        }
        for (byte id : ids) {
            MapleInventoryManipulator.unequip(getC(), id, equip.getNextFreeSlot());
        }
    }

    public void teachSkill(int id, int level, int masterlevel) {
        getPlayer().changeSkillLevel(SkillFactory.getSkill(id), level, masterlevel);
    }

    /**
     * Use getPlayer() instead (for consistency with MapleClient)
     *
     * @return
     */
    @Deprecated
    public MapleCharacter getChar() {
        return getPlayer();
    }

    public MapleClient getC() {
        return getClient();
    }

    public void rechargeStars() {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        IItem stars = getChar().getInventory(MapleInventoryType.USE).getItem((byte) 1);
        if (ii.isThrowingStar(stars.getItemId())) {
            stars.setQuantity(ii.getSlotMax(stars.getItemId()));
            getC().getSession().write(MaplePacketCreator.updateInventorySlot(MapleInventoryType.USE, (Item) stars));
        }
    }

    public EventManager getEventManager(String event) {
        return getClient().getChannelServer().getEventSM().getEventManager(event);
    }

    public void showEffect(String effect) {
        getPlayer().getMap().broadcastMessage(MaplePacketCreator.showEffect(effect));
    }

    public void playSound(String sound) {
        getClient().getPlayer().getMap().broadcastMessage(MaplePacketCreator.playSound(sound));
    }

    public void setHair(int hair) {
        getPlayer().setHair(hair);
        getPlayer().updateSingleStat(MapleStat.HAIR, hair);
        getPlayer().equipChanged();
    }

    public void setFace(int face) {
        getPlayer().setFace(face);
        getPlayer().updateSingleStat(MapleStat.FACE, face);
        getPlayer().equipChanged();
    }

    public void setSkin(int color) {
        getPlayer().setSkinColor(MapleSkinColor.getById(color));
        getPlayer().updateSingleStat(MapleStat.SKIN, color);
        getPlayer().equipChanged();
    }

    public void updateBuddyCapacity(int capacity) {
        getPlayer().getBuddylist().setCapacity(capacity);
        getClient().announce(MaplePacketCreator.updateBuddyCapacity(capacity));
    }

    public void displayGuildRanks() {
        MapleGuild.displayGuildRanks(getClient(), npc);
    }

    @Override
    public String toString() {
        return "Conversation with NPC: " + npc;
    }
}
