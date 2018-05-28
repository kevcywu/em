package net;

public enum SendOpCodeIDB {
    // <= 9
    ON_CHECK_PASSWORD_RESULT(0x00),
    ON_GUEST_ID_LOGIN_RESULT(0x01, false),
    ON_CHECK_USER_LIMIT_RESULT(0x03),
    ON_SET_ACCOUNT_RESULT(0x04, false),
    ON_CONFIRM_EULA_RESULT(0x05, false),
    ON_CHECK_PIN_CODE_RESULT(0x06),
    ON_UPDATE_PIN_CODE_RESULT(0x07, false),
    ON_VIEW_ALL_CHARR_ESULT(0x08, false),
    ON_WORLD_INFORMATION(0x0A),
    ON_SELECT_WORLD_RESULT(0x0B, false),
    OnSelectCharacterResult(0x0C, false),
    OnCheckDuplicatedIDResult(0x0D, false),
    OnCreateNewCharacterResult(0x0E, false),
    ON_DELETE_CHARACTER_RESULT(0x0F),
    RELOG_RESPONSE(0x16), // ok
    //
    ON_PLACE_HOLDER(0xFF);
    private int code = -2;

    private SendOpCodeIDB(int code, boolean updated) {
        this.code = updated ? code : -2;
    }

    private SendOpCodeIDB(int code) {
        this.code = code;
    }

    public int getValue() {
        return code;
    }

}
