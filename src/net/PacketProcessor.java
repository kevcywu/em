package net;

import net.channel.handler.BuddylistModifyHandler;
import net.channel.handler.CancelBuffHandler;
import net.channel.handler.CancelItemEffectHandler;
import net.channel.handler.ChangeChannelHandler;
import net.channel.handler.ChangeMapHandler;
import net.channel.handler.ChangeMapSpecialHandler;
import net.channel.handler.CharInfoRequestHandler;
import net.channel.handler.CloseRangeDamageHandler;
import net.channel.handler.DamageSummonHandler;
import net.channel.handler.DenyPartyRequestHandler;
import net.channel.handler.DistributeAPHandler;
import net.channel.handler.DistributeSPHandler;
import net.channel.handler.DoorHandler;
import net.channel.handler.EnterCashShopHandler;
import net.channel.handler.EnterMTSHandler;
import net.channel.handler.FaceExpressionHandler;
import net.channel.handler.GeneralchatHandler;
import net.channel.handler.GiveFameHandler;
import net.channel.handler.HealOvertimeHandler;
import net.channel.handler.ItemMoveHandler;
import net.channel.handler.ItemPickupHandler;
import net.channel.handler.KeymapChangeHandler;
import net.channel.handler.MagicDamageHandler;
import net.channel.handler.MesoDropHandler;
import net.channel.handler.MoveLifeHandler;
import net.channel.handler.MovePlayerHandler;
import net.channel.handler.MoveSummonHandler;
import net.channel.handler.NPCMoreTalkHandler;
import net.channel.handler.NPCShopHandler;
import net.channel.handler.NPCTalkHandler;
import net.channel.handler.PartyOperationHandler;
import net.channel.handler.PartychatHandler;
import net.channel.handler.PlayerInteractionHandler;
import net.channel.handler.PlayerLoggedinHandler;
import net.channel.handler.QuestActionHandler;
import net.channel.handler.RangedAttackHandler;
import net.channel.handler.ScrollHandler;
import net.channel.handler.SkillEffectHandler;
import net.channel.handler.SpecialMoveHandler;
import net.channel.handler.StorageHandler;
import net.channel.handler.SummonDamageHandler;
import net.channel.handler.TakeDamageHandler;
import net.channel.handler.UseCashItemHandler;
import net.channel.handler.UseItemHandler;
import net.channel.handler.WhisperHandler;
import net.channel.handler.UseItemEffectHandler;
import net.channel.handler.UseChairHandler;
import net.channel.handler.CancelChairHandler;
import net.channel.handler.ReactorHitHandler;
import net.channel.handler.GuildOperationHandler;
import net.channel.handler.BBSOperationHandler;
import net.handler.KeepAliveHandler;
import net.handler.LoginRequiringNoOpHandler;
import net.login.handler.AfterLoginHandler;
import net.login.handler.CharSelectedHandler;
import net.login.handler.CharlistRequestHandler;
import net.login.handler.CheckCharNameHandler;
import net.login.handler.CreateCharHandler;
import net.login.handler.DeleteCharHandler;
import net.login.handler.LoginPasswordHandler;
import net.login.handler.RelogRequestHandler;
import net.login.handler.ServerStatusRequestHandler;
import net.login.handler.ServerlistRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PacketProcessor {

    private static final Logger log = LoggerFactory.getLogger(PacketProcessor.class);

    public enum Mode {
        LOGINSERVER, CHANNELSERVER
    };

    private static PacketProcessor instance;
    private MaplePacketHandler[] handlers;

    private PacketProcessor() {
        int maxRecvOp = 0;
        for (RecvOpcode op : RecvOpcode.values()) {
            if (op.getValue() > maxRecvOp) {
                maxRecvOp = op.getValue();
            }
        }
        handlers = new MaplePacketHandler[maxRecvOp + 1];
    }

    public MaplePacketHandler getHandler(short packetId) {
        if (packetId > handlers.length) {
            return null;
        }
        MaplePacketHandler handler = handlers[packetId];
        if (handler != null) {
            return handler;
        }
        return null;
    }

    public void registerHandler(RecvOpcode code, MaplePacketHandler handler) {
        if (code.getValue() != -2) {
            try {
                handlers[code.getValue()] = handler;
            } catch (ArrayIndexOutOfBoundsException e) {
                log.error("Error registering handler - " + code.name());
                e.printStackTrace();
            }
        }
    }

    public synchronized static PacketProcessor getProcessor(Mode mode) {
        if (instance == null) {
            instance = new PacketProcessor();
            instance.reset(mode);
        }
        return instance;
    }

    public void reset(Mode mode) {
        handlers = new MaplePacketHandler[handlers.length];
        registerHandler(RecvOpcode.PONG, new KeepAliveHandler());
        if (null == mode) {
            throw new RuntimeException("Unknown packet processor mode");
        } else {
            switch (mode) {
                case LOGINSERVER:
                    registerHandler(RecvOpcode.AFTER_LOGIN, new AfterLoginHandler());
                    registerHandler(RecvOpcode.SERVERLIST_REREQUEST, new ServerlistRequestHandler());
                    registerHandler(RecvOpcode.CHARLIST_REQUEST, new CharlistRequestHandler());
                    registerHandler(RecvOpcode.CHAR_SELECT, new CharSelectedHandler());
                    registerHandler(RecvOpcode.LOGIN_PASSWORD, new LoginPasswordHandler());
                    registerHandler(RecvOpcode.RELOG, new RelogRequestHandler());
                    registerHandler(RecvOpcode.SERVERLIST_REQUEST, new ServerlistRequestHandler());
                    registerHandler(RecvOpcode.SERVERSTATUS_REQUEST, new ServerStatusRequestHandler());
                    registerHandler(RecvOpcode.CHECK_CHAR_NAME, new CheckCharNameHandler());
                    registerHandler(RecvOpcode.CREATE_CHAR, new CreateCharHandler());
                    registerHandler(RecvOpcode.DELETE_CHAR, new DeleteCharHandler());
                    break;
                case CHANNELSERVER:
                    registerHandler(RecvOpcode.CHANGE_CHANNEL, new ChangeChannelHandler());
                    registerHandler(RecvOpcode.STRANGE_DATA, LoginRequiringNoOpHandler.getInstance());
                    registerHandler(RecvOpcode.GENERAL_CHAT, new GeneralchatHandler());
                    registerHandler(RecvOpcode.WHISPER, new WhisperHandler());
                    registerHandler(RecvOpcode.NPC_TALK, new NPCTalkHandler());
                    registerHandler(RecvOpcode.NPC_TALK_MORE, new NPCMoreTalkHandler());
                    registerHandler(RecvOpcode.QUEST_ACTION, new QuestActionHandler());
                    registerHandler(RecvOpcode.NPC_SHOP, new NPCShopHandler());
                    registerHandler(RecvOpcode.ITEM_MOVE, new ItemMoveHandler());
                    registerHandler(RecvOpcode.MESO_DROP, new MesoDropHandler());
                    registerHandler(RecvOpcode.PLAYER_LOGGEDIN, new PlayerLoggedinHandler());
                    registerHandler(RecvOpcode.CHANGE_MAP, new ChangeMapHandler());
                    registerHandler(RecvOpcode.MOVE_LIFE, new MoveLifeHandler());
                    registerHandler(RecvOpcode.CLOSE_RANGE_ATTACK, new CloseRangeDamageHandler());
                    registerHandler(RecvOpcode.RANGED_ATTACK, new RangedAttackHandler());
                    registerHandler(RecvOpcode.MAGIC_ATTACK, new MagicDamageHandler());
                    registerHandler(RecvOpcode.TAKE_DAMAGE, new TakeDamageHandler());
                    registerHandler(RecvOpcode.MOVE_PLAYER, new MovePlayerHandler());
                    registerHandler(RecvOpcode.USE_CASH_ITEM, new UseCashItemHandler());
                    registerHandler(RecvOpcode.USE_ITEM, new UseItemHandler());
                    registerHandler(RecvOpcode.USE_RETURN_SCROLL, new UseItemHandler());
                    registerHandler(RecvOpcode.USE_UPGRADE_SCROLL, new ScrollHandler());
                    registerHandler(RecvOpcode.FACE_EXPRESSION, new FaceExpressionHandler());
                    registerHandler(RecvOpcode.HEAL_OVER_TIME, new HealOvertimeHandler());
                    registerHandler(RecvOpcode.ITEM_PICKUP, new ItemPickupHandler());
                    registerHandler(RecvOpcode.CHAR_INFO_REQUEST, new CharInfoRequestHandler());
                    registerHandler(RecvOpcode.SPECIAL_MOVE, new SpecialMoveHandler());
                    registerHandler(RecvOpcode.CANCEL_BUFF, new CancelBuffHandler());
                    registerHandler(RecvOpcode.CANCEL_ITEM_EFFECT, new CancelItemEffectHandler());
                    registerHandler(RecvOpcode.PLAYER_INTERACTION, new PlayerInteractionHandler());
                    registerHandler(RecvOpcode.DISTRIBUTE_AP, new DistributeAPHandler());
                    registerHandler(RecvOpcode.DISTRIBUTE_SP, new DistributeSPHandler());
                    registerHandler(RecvOpcode.CHANGE_KEYMAP, new KeymapChangeHandler());
                    registerHandler(RecvOpcode.CHANGE_MAP_SPECIAL, new ChangeMapSpecialHandler());
                    registerHandler(RecvOpcode.STORAGE, new StorageHandler());
                    registerHandler(RecvOpcode.GIVE_FAME, new GiveFameHandler());
                    registerHandler(RecvOpcode.PARTY_OPERATION, new PartyOperationHandler());
                    registerHandler(RecvOpcode.DENY_PARTY_REQUEST, new DenyPartyRequestHandler());
                    registerHandler(RecvOpcode.PARTYCHAT, new PartychatHandler());
                    registerHandler(RecvOpcode.USE_DOOR, new DoorHandler());
                    registerHandler(RecvOpcode.ENTER_MTS, new EnterMTSHandler());
                    registerHandler(RecvOpcode.ENTER_CASH_SHOP, new EnterCashShopHandler());
                    registerHandler(RecvOpcode.DAMAGE_SUMMON, new DamageSummonHandler());
                    registerHandler(RecvOpcode.MOVE_SUMMON, new MoveSummonHandler());
                    registerHandler(RecvOpcode.SUMMON_ATTACK, new SummonDamageHandler());
                    registerHandler(RecvOpcode.BUDDYLIST_MODIFY, new BuddylistModifyHandler());
                    registerHandler(RecvOpcode.USE_ITEMEFFECT, new UseItemEffectHandler());
                    registerHandler(RecvOpcode.USE_CHAIR, new UseChairHandler());
                    registerHandler(RecvOpcode.CANCEL_CHAIR, new CancelChairHandler());
                    registerHandler(RecvOpcode.DAMAGE_REACTOR, new ReactorHitHandler());
                    registerHandler(RecvOpcode.GUILD_OPERATION, new GuildOperationHandler());
                    registerHandler(RecvOpcode.BBS_OPERATION, new BBSOperationHandler());
                    registerHandler(RecvOpcode.SKILL_EFFECT, new SkillEffectHandler());
                    break;
                default:
                    throw new RuntimeException("Unknown packet processor mode");
            }
        }
    }
}
