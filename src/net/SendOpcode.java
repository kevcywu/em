package net;

public enum SendOpcode {
    // GENERAL
    PING(0x11),
    // LOGIN
    CHARLIST(0x0B),
    CHAR_NAME_RESPONSE(0x0D),
    ADD_NEW_CHAR_ENTRY(0x0E),
    SERVER_IP(0x0C),
    UPDATE_STATS(0x1C),
    // CHANNEL
    CHANGE_CHANNEL(0x10),
    MODIFY_INVENTORY_ITEM(0x1A),
    UPDATE_SKILLS(0x21),
    SHOW_STATUS_INFO(0x24),
    BUDDYLIST(0x3C),
    GUILD_OPERATION(0x3E),
    SERVERMESSAGE(0x41),
    WARP_TO_MAP(0x5C),
    SPAWN_PLAYER(0x78),
    CHATTEXT(0x7A),
    SPAWN_MONSTER(0xAF),
    KILL_MONSTER(0xB0),
    SPAWN_MONSTER_CONTROL(0xB1),
    MOVE_MONSTER(0xB2),
    MOVE_MONSTER_RESPONSE(0xB3),
    SHOW_MONSTER_HP(0xBD),
    SPAWN_NPC(0xC2),
    SPAWN_NPC_REQUEST_CONTROLLER(0xC4),
    DROP_ITEM_FROM_MAPOBJECT(0xCD),
    REMOVE_ITEM_FROM_MAP(0xCE),
    C_SCRIPT_MAN(0xED, false), // NPCTalk
    CONFIRM_SHOP_TRANSACTION(0xD8, false),
    OPEN_NPC_SHOP(0xEE),
    KEYMAP(0x107),
    //
    AVATAR_MEGA(0x19, false),
    SHOW_QUEST_COMPLETION(0x1F, false),
    CANCEL_BUFF(0x24, false),
    SPAWN_PORTAL(0x29, false),
    FAME_RESPONSE(0x31, false),
    SHOW_MESO_GAIN(0x33, false),
    CHAR_INFO(0x2A, false),
    PARTY_OPERATION(0x39, false),
    GIVE_BUFF(0x3B, false),
    BOSS_ENV(0x54, false),
    MULTICHAT(0x56, false),
    WHISPER(0x5F, false),
    CLOCK(0x62, false),
    CANCEL_CHAIR(0x67, false),
    UPDATE_QUEST_INFO(0x6d, false),
    REMOVE_PLAYER_FROM_MAP(0x71, false),
    SPAWN_SPECIAL_MAPOBJECT(0x73, false),
    REMOVE_SPECIAL_MAPOBJECT(0x74, false),
    MOVE_SUMMON(0x75, false),
    SUMMON_ATTACK(0x76, false),
    DAMAGE_SUMMON(0x78, false),
    SHOW_SCROLL_EFFECT(0x7B, false),
    MOVE_PLAYER(0x85, false),
    SHOW_FOREIGN_EFFECT(0x86, false),
    CLOSE_RANGE_ATTACK(0x88, false),
    RANGED_ATTACK(0x8E, false),
    DAMAGE_PLAYER(0x8A, false),
    MAGIC_ATTACK(0x94, false),
    CANCEL_FOREIGN_BUFF(0x8B, false),
    UPDATE_PARTYMEMBER_HP(0x8C, false),
    FACIAL_EXPRESSION(0x8D, false),
    UPDATE_CHAR_LOOK(0x93, false),
    GIVE_FOREIGN_BUFF(0x87, false),
    SHOW_ITEM_GAIN_INCHAT(0x68, false),
    SHOW_ITEM_EFFECT(0x8F, false),
    DAMAGE_MONSTER(0x9E, false),
    SHOW_CHAIR(0x92, false),
    APPLY_MONSTER_STATUS(0x9B, false),
    CANCEL_MONSTER_STATUS(0x8C, false),
    REACTOR_SPAWN(0xB3, false),
    REACTOR_HIT(0xB4, false),
    REACTOR_DESTROY(0x85, false),
    SPAWN_MIST(0xBE, false),
    REMOVE_MIST(0xBF, false),
    SPAWN_DOOR(0xC0, false),
    REMOVE_DOOR(0xC1, false),
    OPEN_STORAGE(0xD9, false),
    PLAYER_INTERACTION(0xDE, false),
    MAP_EFFECT(0x55, false),
    UPDATE_CHAR_BOX(0xFFFF, false),
    BBS_OPERATION(0x42, false),
    SKILL_EFFECT(0x91, false),
    CANCEL_SKILL_EFFECT(0x89, false);

    private int code = -2;

    private SendOpcode(int code, boolean updated) {
        this.code = updated ? code : -2;
    }

    private SendOpcode(int code) {
        this.code = code;
    }

    public int getValue() {
        return code;
    }

}
