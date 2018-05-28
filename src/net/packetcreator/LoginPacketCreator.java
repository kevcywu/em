package net.packetcreator;

import java.util.Map;
import java.util.Set;
import net.MaplePacket;
import net.SendOpCodeIDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.HexTool;
import tools.data.output.MaplePacketLittleEndianWriter;

public class LoginPacketCreator {

    private static final Logger log = LoggerFactory.getLogger(LoginPacketCreator.class);

    // ON_CHECK_PASSWORD_RESULT 0x00
    public enum CheckPasswordResult {
        ID_DELETED_OR_BLOCKED((byte) 3), INCORRECT_PASSWORD((byte) 4), ID_UNREGISTERED((byte) 5),
        SYSTEM_ERROR1((byte) 6), ALREADY_LOGGED_IN((byte) 7), SYSTEM_ERROR2((byte) 8),
        SYSTEM_ERROR3((byte) 9), CONNECTION_OVERLOADED((byte) 10), AGE_RESTRICTION((byte) 11);

        private final byte value;

        private CheckPasswordResult(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return value;
        }
    }

    public static MaplePacket getPermBan(byte reason) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(16);
        mplew.writeShort(SendOpCodeIDB.ON_CHECK_PASSWORD_RESULT.getValue());
        mplew.writeShort(0x02); // Account is banned
        mplew.write(0x0);
        mplew.write(reason);
        mplew.write(HexTool.getByteArrayFromHexString("01 01 01 01 00"));
        return mplew.getPacket();
    }

    public static MaplePacket getTempBan(long timestampTill, byte reason) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(17);
        mplew.writeShort(SendOpCodeIDB.ON_CHECK_PASSWORD_RESULT.getValue());
        mplew.write(0x02);
        mplew.write(HexTool.getByteArrayFromHexString("00 00 00 00 00")); // Account is banned
        mplew.write(reason);
        mplew.writeLong(timestampTill); // Tempban date is handled as a 64-bit long, number of 100NS intervals since
        // 1/1/1601. Lulz.
        return mplew.getPacket();
    }

    public static MaplePacket getLoginFailed(CheckPasswordResult result) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(16);
        mplew.writeShort(SendOpCodeIDB.ON_CHECK_PASSWORD_RESULT.getValue());
        mplew.writeInt(result.getValue());
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static MaplePacket getLoginFailed(int result) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(16);
        mplew.writeShort(SendOpCodeIDB.ON_CHECK_PASSWORD_RESULT.getValue());
        mplew.writeInt(result);
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static MaplePacket getAuthSuccess(String account) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpCodeIDB.ON_CHECK_PASSWORD_RESULT.getValue());
        mplew.write(0);
        mplew.write(0);
        mplew.writeInt(0);
        mplew.write(new byte[]{(byte) 0xFF, 0x6A, 1, 0});
        mplew.write(0);
        mplew.write(0);
        mplew.write(0x4E);
        mplew.writeMapleAsciiString(account);
        mplew.write(3);
        mplew.write(0);
        mplew.writeLong(0);
        mplew.writeLong(MaplePacketCreator.getTime((long) System.currentTimeMillis()));
        mplew.writeInt(8);
        return mplew.getPacket();
    }

    // ON_CHECK_USER_LIMIT_RESULT 0x03
    public enum ServerStatus {
        NORMAL((byte) 0), HIGHLY_POPULATED((byte) 1), FULL((byte) 2);
        private final byte value;

        private ServerStatus(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return value;
        }
    }

    public static MaplePacket getServerStatus(ServerStatus status) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpCodeIDB.ON_CHECK_USER_LIMIT_RESULT.getValue());
        mplew.write(status.getValue());
        mplew.write(0);
        return mplew.getPacket();
    }

    // ON_CHECK_PIN_CODE_RESULT 0x06
    public enum PinCodeResult {
        PIN_ACCEPTED((byte) 0), REGISTER_NEW_PIN((byte) 1), INVALID_PIN((byte) 2), CONNECTION_FAILED((byte) 3), REQUEST_PIN((byte) 4);
        private final byte value;

        private PinCodeResult(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return value;
        }
    }

    public static MaplePacket getPinCodeResult(PinCodeResult mode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendOpCodeIDB.ON_CHECK_PIN_CODE_RESULT.getValue());
        mplew.write(mode.getValue());
        return mplew.getPacket();
    }

    // ON_WORLD_INFORMATION 0x0A
    public static MaplePacket getWorldInformation(int serverIndex, String serverName, String serverMessage, Map<Integer, Integer> channelLoad) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpCodeIDB.ON_WORLD_INFORMATION.getValue());
        mplew.write(serverIndex);
        if (serverIndex >= 0) {
            mplew.writeMapleAsciiString(serverName);
            mplew.write(2);
            mplew.writeMapleAsciiString(serverMessage);
            mplew.writeShort(0x64);
            mplew.writeShort(0x64);
            mplew.write(0);

            int lastChannel = 1;
            Set<Integer> channels = channelLoad.keySet();
            for (int i = 30; i > 0; i--) {
                if (channels.contains(i)) {
                    lastChannel = i;
                    break;
                }
            }

            mplew.write(lastChannel);
            int load;
            for (int i = 1; i <= lastChannel; i++) {
                load = channels.contains(i) ? channelLoad.get(i) : 1200;
                mplew.writeMapleAsciiString(serverName + "-" + i);
                mplew.writeInt(load);
                mplew.write(serverIndex);
                mplew.write(i - 1);
                mplew.write(0);
            }
            mplew.writeShort(0);
        }
        return mplew.getPacket();
    }

    public static MaplePacket getEndOfWorldInformation() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpCodeIDB.ON_WORLD_INFORMATION.getValue());
        mplew.write(0xFF);
        return mplew.getPacket();
    }
    // OnSelectWorldResult 0x0B

    // ON_DELETE_CHARACTER_RESULT 0x0F
    public enum DeleteCharResult {
        OK((byte) 0), INVALID_BIRTHDAY((byte) 12);
        private final byte value;

        private DeleteCharResult(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return value;
        }
    }

    public static MaplePacket deleteCharResponse(int cid, DeleteCharResult result) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpCodeIDB.ON_DELETE_CHARACTER_RESULT.getValue());
        mplew.writeInt(cid);
        mplew.write(result.getValue());
        return mplew.getPacket();
    }

    // RelogResponse 0x16
    public static MaplePacket getRelogResponse() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendOpCodeIDB.RELOG_RESPONSE.getValue());
        mplew.write(true);
        return mplew.getPacket();
    }
}
