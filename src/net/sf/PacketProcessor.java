/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
                       Matthias Butz <matze@odinms.de>
                       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License version 3
    as published by the Free Software Foundation. You may not use, modify
    or distribute this program under any other version of the
    GNU Affero General Public License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package net.sf;

import net.sf.channel.handler.BuddylistModifyHandler;
import net.sf.channel.handler.CancelBuffHandler;
import net.sf.channel.handler.CancelItemEffectHandler;
import net.sf.channel.handler.ChangeChannelHandler;
import net.sf.channel.handler.ChangeMapHandler;
import net.sf.channel.handler.ChangeMapSpecialHandler;
import net.sf.channel.handler.CharInfoRequestHandler;
import net.sf.channel.handler.CloseRangeDamageHandler;
import net.sf.channel.handler.DamageSummonHandler;
import net.sf.channel.handler.DenyPartyRequestHandler;
import net.sf.channel.handler.DistributeAPHandler;
import net.sf.channel.handler.DistributeSPHandler;
import net.sf.channel.handler.DoorHandler;
import net.sf.channel.handler.EnterCashShopHandler;
import net.sf.channel.handler.EnterMTSHandler;
import net.sf.channel.handler.FaceExpressionHandler;
import net.sf.channel.handler.GeneralchatHandler;
import net.sf.channel.handler.GiveFameHandler;
import net.sf.channel.handler.HealOvertimeHandler;
import net.sf.channel.handler.ItemMoveHandler;
import net.sf.channel.handler.ItemPickupHandler;
import net.sf.channel.handler.KeymapChangeHandler;
import net.sf.channel.handler.MagicDamageHandler;
import net.sf.channel.handler.MesoDropHandler;
import net.sf.channel.handler.MoveLifeHandler;
import net.sf.channel.handler.MovePlayerHandler;
import net.sf.channel.handler.MoveSummonHandler;
import net.sf.channel.handler.NPCMoreTalkHandler;
import net.sf.channel.handler.NPCShopHandler;
import net.sf.channel.handler.NPCTalkHandler;
import net.sf.channel.handler.PartyOperationHandler;
import net.sf.channel.handler.PartychatHandler;
import net.sf.channel.handler.PlayerInteractionHandler;
import net.sf.channel.handler.PlayerLoggedinHandler;
import net.sf.channel.handler.QuestActionHandler;
import net.sf.channel.handler.RangedAttackHandler;
import net.sf.channel.handler.ScrollHandler;
import net.sf.channel.handler.SkillEffectHandler;
import net.sf.channel.handler.SpecialMoveHandler;
import net.sf.channel.handler.StorageHandler;
import net.sf.channel.handler.SummonDamageHandler;
import net.sf.channel.handler.TakeDamageHandler;
import net.sf.channel.handler.UseCashItemHandler;
import net.sf.channel.handler.UseItemHandler;
import net.sf.channel.handler.WhisperHandler;
import net.sf.channel.handler.UseItemEffectHandler;
import net.sf.channel.handler.UseChairHandler;
import net.sf.channel.handler.CancelChairHandler;
import net.sf.channel.handler.ReactorHitHandler;
import net.sf.channel.handler.GuildOperationHandler;
import net.sf.channel.handler.BBSOperationHandler;
import net.sf.handler.KeepAliveHandler;
import net.sf.handler.LoginRequiringNoOpHandler;
import net.sf.login.handler.AfterLoginHandler;
import net.sf.login.handler.CharSelectedHandler;
import net.sf.login.handler.CharlistRequestHandler;
import net.sf.login.handler.CheckCharNameHandler;
import net.sf.login.handler.CreateCharHandler;
import net.sf.login.handler.DeleteCharHandler;
import net.sf.login.handler.LoginPasswordHandler;
import net.sf.login.handler.RelogRequestHandler;
import net.sf.login.handler.ServerStatusRequestHandler;
import net.sf.login.handler.ServerlistRequestHandler;

public final class PacketProcessor {
	//private static Logger log = LoggerFactory.getLogger(PacketProcessor.class);
	public enum Mode {
		LOGINSERVER,
		CHANNELSERVER
	};

	private static PacketProcessor instance;
	private MaplePacketHandler[] handlers;

	private PacketProcessor() {
		int maxRecvOp = 0;
		for (RecvPacketOpcode op : RecvPacketOpcode.values()) {
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

	public void registerHandler(RecvPacketOpcode code, MaplePacketHandler handler) {
		handlers[code.getValue()] = handler;
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
		registerHandler(RecvPacketOpcode.PONG, new KeepAliveHandler());
		if (mode == Mode.LOGINSERVER) {
			registerHandler(RecvPacketOpcode.AFTER_LOGIN, new AfterLoginHandler());
			registerHandler(RecvPacketOpcode.SERVERLIST_REREQUEST, new ServerlistRequestHandler());
			registerHandler(RecvPacketOpcode.CHARLIST_REQUEST, new CharlistRequestHandler());
			registerHandler(RecvPacketOpcode.CHAR_SELECT, new CharSelectedHandler());
			registerHandler(RecvPacketOpcode.LOGIN_PASSWORD, new LoginPasswordHandler());
			registerHandler(RecvPacketOpcode.RELOG, new RelogRequestHandler());
			registerHandler(RecvPacketOpcode.SERVERLIST_REQUEST, new ServerlistRequestHandler());
			registerHandler(RecvPacketOpcode.SERVERSTATUS_REQUEST, new ServerStatusRequestHandler());
			registerHandler(RecvPacketOpcode.CHECK_CHAR_NAME, new CheckCharNameHandler());
			registerHandler(RecvPacketOpcode.CREATE_CHAR, new CreateCharHandler());
			registerHandler(RecvPacketOpcode.DELETE_CHAR, new DeleteCharHandler());
		} else if (mode == Mode.CHANNELSERVER) {
			registerHandler(RecvPacketOpcode.CHANGE_CHANNEL, new ChangeChannelHandler());
			registerHandler(RecvPacketOpcode.STRANGE_DATA, LoginRequiringNoOpHandler.getInstance());
			registerHandler(RecvPacketOpcode.GENERAL_CHAT, new GeneralchatHandler());
			registerHandler(RecvPacketOpcode.WHISPER, new WhisperHandler());
			registerHandler(RecvPacketOpcode.NPC_TALK, new NPCTalkHandler());
			registerHandler(RecvPacketOpcode.NPC_TALK_MORE, new NPCMoreTalkHandler());
			registerHandler(RecvPacketOpcode.QUEST_ACTION, new QuestActionHandler());
			registerHandler(RecvPacketOpcode.NPC_SHOP, new NPCShopHandler());
			registerHandler(RecvPacketOpcode.ITEM_MOVE, new ItemMoveHandler());
			registerHandler(RecvPacketOpcode.MESO_DROP, new MesoDropHandler());
			registerHandler(RecvPacketOpcode.PLAYER_LOGGEDIN, new PlayerLoggedinHandler());
			registerHandler(RecvPacketOpcode.CHANGE_MAP, new ChangeMapHandler());
			registerHandler(RecvPacketOpcode.MOVE_LIFE, new MoveLifeHandler());
			registerHandler(RecvPacketOpcode.CLOSE_RANGE_ATTACK, new CloseRangeDamageHandler());
			registerHandler(RecvPacketOpcode.RANGED_ATTACK, new RangedAttackHandler());
			registerHandler(RecvPacketOpcode.MAGIC_ATTACK, new MagicDamageHandler());
			registerHandler(RecvPacketOpcode.TAKE_DAMAGE, new TakeDamageHandler());
			registerHandler(RecvPacketOpcode.MOVE_PLAYER, new MovePlayerHandler());
			registerHandler(RecvPacketOpcode.USE_CASH_ITEM, new UseCashItemHandler());
			registerHandler(RecvPacketOpcode.USE_ITEM, new UseItemHandler());
			registerHandler(RecvPacketOpcode.USE_RETURN_SCROLL, new UseItemHandler());
			registerHandler(RecvPacketOpcode.USE_UPGRADE_SCROLL, new ScrollHandler());
			registerHandler(RecvPacketOpcode.FACE_EXPRESSION, new FaceExpressionHandler());
			registerHandler(RecvPacketOpcode.HEAL_OVER_TIME, new HealOvertimeHandler());
			registerHandler(RecvPacketOpcode.ITEM_PICKUP, new ItemPickupHandler());
			registerHandler(RecvPacketOpcode.CHAR_INFO_REQUEST, new CharInfoRequestHandler());
			registerHandler(RecvPacketOpcode.SPECIAL_MOVE, new SpecialMoveHandler());
			registerHandler(RecvPacketOpcode.CANCEL_BUFF, new CancelBuffHandler());
			registerHandler(RecvPacketOpcode.CANCEL_ITEM_EFFECT, new CancelItemEffectHandler());
			registerHandler(RecvPacketOpcode.PLAYER_INTERACTION, new PlayerInteractionHandler());
			registerHandler(RecvPacketOpcode.DISTRIBUTE_AP, new DistributeAPHandler());
			registerHandler(RecvPacketOpcode.DISTRIBUTE_SP, new DistributeSPHandler());
			registerHandler(RecvPacketOpcode.CHANGE_KEYMAP, new KeymapChangeHandler());
			registerHandler(RecvPacketOpcode.CHANGE_MAP_SPECIAL, new ChangeMapSpecialHandler());
			registerHandler(RecvPacketOpcode.STORAGE, new StorageHandler());
			registerHandler(RecvPacketOpcode.GIVE_FAME, new GiveFameHandler());
			registerHandler(RecvPacketOpcode.PARTY_OPERATION, new PartyOperationHandler());
			registerHandler(RecvPacketOpcode.DENY_PARTY_REQUEST, new DenyPartyRequestHandler());
			registerHandler(RecvPacketOpcode.PARTYCHAT, new PartychatHandler());
			registerHandler(RecvPacketOpcode.USE_DOOR, new DoorHandler());
			registerHandler(RecvPacketOpcode.ENTER_MTS, new EnterMTSHandler());
			registerHandler(RecvPacketOpcode.ENTER_CASH_SHOP, new EnterCashShopHandler());
			registerHandler(RecvPacketOpcode.DAMAGE_SUMMON, new DamageSummonHandler());
			registerHandler(RecvPacketOpcode.MOVE_SUMMON, new MoveSummonHandler());
			registerHandler(RecvPacketOpcode.SUMMON_ATTACK, new SummonDamageHandler());
			registerHandler(RecvPacketOpcode.BUDDYLIST_MODIFY, new BuddylistModifyHandler());
			registerHandler(RecvPacketOpcode.USE_ITEMEFFECT, new UseItemEffectHandler());
			registerHandler(RecvPacketOpcode.USE_CHAIR, new UseChairHandler());
			registerHandler(RecvPacketOpcode.CANCEL_CHAIR, new CancelChairHandler());
			registerHandler(RecvPacketOpcode.DAMAGE_REACTOR, new ReactorHitHandler());
			registerHandler(RecvPacketOpcode.GUILD_OPERATION, new GuildOperationHandler());
			registerHandler(RecvPacketOpcode.BBS_OPERATION, new BBSOperationHandler());
			registerHandler(RecvPacketOpcode.SKILL_EFFECT, new SkillEffectHandler());
		} else {
			throw new RuntimeException("Unknown packet processor mode");
		}
	}
}
