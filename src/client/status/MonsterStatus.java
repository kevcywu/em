package client.status;

import net.IntValueHolder;

public enum MonsterStatus implements IntValueHolder {
    WATK(0x1),
    WDEF(0x2),
    SPEED(0x40),
    STUN(0x80), //this is possibly only the bowman stun
    FREEZE(0x100),
    POISON(0x200),
    SEAL(0x400),
    DOOM(0x10000),
    SHADOW_WEB(0x20000),
    MAGIC_DEFENSE_UP(0x8000),;

    private final int i;

    private MonsterStatus(int i) {
        this.i = i;
    }

    @Override
    public int getValue() {
        return i;
    }
}
