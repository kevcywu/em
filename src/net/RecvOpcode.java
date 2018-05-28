package net;

public enum RecvOpcode {
    // GENERAL
    PONG(0x18),
    // LOGIN
    LOGIN_PASSWORD(0x01),
    SERVERLIST_REREQUEST(0x04),
    CHARLIST_REQUEST(0x05),
    SERVERSTATUS_REQUEST(0x06),
    AFTER_LOGIN(0x09),
    SERVERLIST_REQUEST(0x0B),
    CHAR_SELECT(0x13),
    CHECK_CHAR_NAME(0x15),
    CREATE_CHAR(0x16),
    DELETE_CHAR(0x17),
    RELOG(0x1C),
    // CHANNEL
    PLAYER_LOGGEDIN(0x14),
    STRANGE_DATA(0x1A),
    CHANGE_MAP(0x23),
    CHANGE_CHANNEL(0x24),
    MOVE_PLAYER(0x26),
    CLOSE_RANGE_ATTACK(0x29),
    RANGED_ATTACK(0x2A, false),
    TAKE_DAMAGE(0x2D),
    GENERAL_CHAT(0x2E),
    FACE_EXPRESSION(0x30),
    NPC_TALK(0x36),
    NPC_SHOP(0x39),
    ITEM_MOVE(0x42),
    DISTRIBUTE_AP(0x50),
    HEAL_OVER_TIME(0x51),
    DISTRIBUTE_SP(0x52),
    SKILL_EFFECT(0x53, false),
    CHANGE_KEYMAP(0x7B),
    MOVE_LIFE(0x9D),
    ITEM_PICKUP(0xAB),
    SPECIAL_MOVE(0xD8, false),
    //
    ENTER_CASH_SHOP(0x28, false),
    STORAGE(0x20, false),
    NPC_TALK_MORE(0x21, false),
    MAGIC_ATTACK(0x2E, false),
    USE_ITEM(0x63, false),
    CANCEL_ITEM_EFFECT(0x49, false),
    USE_CASH_ITEM(0x53, false),
    USE_RETURN_SCROLL(0x64, false),
    USE_UPGRADE_SCROLL(0x65, false),
    CANCEL_BUFF(0x4E, false),
    MESO_DROP(0x68, false),
    GIVE_FAME(0x69, false),
    CHAR_INFO_REQUEST(0x44, false),
    CHANGE_MAP_SPECIAL(0x47, false),
    QUEST_ACTION(0x6B, false),
    PARTYCHAT(0x3A, false),
    WHISPER(0x58, false),
    PLAYER_INTERACTION(0x3E, false),
    PARTY_OPERATION(0x31, false),
    DENY_PARTY_REQUEST(0x32, false),
    BUDDYLIST_MODIFY(0x33, false),
    USE_DOOR(0x41, false),
    SUMMON_ATTACK(0x7B, false),
    MOVE_SUMMON(0x7C, false),
    DAMAGE_SUMMON(0x79, false),
    ENTER_MTS(0x77, false),
    USE_ITEMEFFECT(0x5D, false),
    USE_CHAIR(0x2D, false),
    CANCEL_CHAIR(0x2B, false),
    DAMAGE_REACTOR(0x8C, false),
    GUILD_OPERATION(0x3C, false),
    BBS_OPERATION(0x74, false);

    private int code = -2;

    private RecvOpcode(int code, boolean updated) {
        this.code = updated ? code : -2;
    }

    private RecvOpcode(int code) {
        this.code = code;
    }

    public int getValue() {
        return code;
    }

}
