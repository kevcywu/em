package net.packetcreator;

import net.MaplePacket;
import net.SendOpcode;
import tools.HexTool;
import tools.data.output.MaplePacketLittleEndianWriter;

public class NPCPacketCreator {

//    public static MaplePacket getNPCTalk(int result) {
//        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
//        mplew.writeShort(SendOpcode.C_SCRIPT_MAN.getValue());
//        mplew.write(0);
//        mplew.writeInt(0);
//        mplew.write(result);
//        switch (result) {
//            case 0:
//                mplew.writeMapleAsciiString("");
//                mplew.write(0);
//                mplew.write(0);
//                break;
//            case 1:
//                break;
//            case 2:
//                mplew.writeMapleAsciiString("");
//                mplew.writeMapleAsciiString("");
////                result = sub_6419E8(v4, Stra, v3);
//                break;
////            case 3:
////                result = sub_641D12(v4, Stra, v3);
////                break;
////            case 4:
////                result = sub_641E75(v4, Stra, v3);
////                break;
////            case 5:
////                result = sub_6427F9(v4, Stra, v3);
////                break;
////            case 6:
////                result = sub_64280B(v4, Stra, v3);
////                break;
////            case 7:
////                result = sub_641FC9(v4, Stra, v3);
////                break;
////            case 8:
////                result = sub_642162(v4, Stra, v3);
////                break;
////            case 9:
////                result = sub_6422FB(v4, Stra, v3);
////                break;
////            case 10:
////                result = sub_642571(v2, v4, Stra, (int) v3);
////                break;
////            case 12:
////                v7 = 0;
////                goto
////                LABEL_8;
////            case 13:
////                v7 = 1;
////                LABEL_8:
////                v6 = 1;
////                LABEL_9:
////                result = sub_641896(v4, Stra, v3, 0, v6, v7);
////                break;
////            case 14:
////                result = sub_641B90(v4, Stra, v3);
////                break;
////            default:
////                break;
//        }
//
//        return mplew.getPacket();
//    }
    public static MaplePacket getNPCTalk(int npc, byte msgType, String talk, String endBytes) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.C_SCRIPT_MAN.getValue());
        mplew.write(4); // ?
        mplew.writeInt(npc);
        mplew.write(msgType);
        mplew.writeMapleAsciiString(talk);
        mplew.write(HexTool.getByteArrayFromHexString(endBytes));
        return mplew.getPacket();
    }

    public static MaplePacket getNPCTalkStyle(int npc, String talk, int styles[]) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.C_SCRIPT_MAN.getValue());
        mplew.write(4); // ?
        mplew.writeInt(npc);
        mplew.write(7);
        mplew.writeMapleAsciiString(talk);
        mplew.write(styles.length);
        for (int i = 0; i < styles.length; i++) {
            mplew.writeInt(styles[i]);
        }
        return mplew.getPacket();
    }

    public static MaplePacket getNPCTalkNum(int npc, String talk, int def, int min, int max) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.C_SCRIPT_MAN.getValue());
        mplew.write(4); // ?
        mplew.writeInt(npc);
        mplew.write(4);
        mplew.writeMapleAsciiString(talk);
        mplew.writeInt(def);
        mplew.writeInt(min);
        mplew.writeInt(max);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static MaplePacket getNPCTalkText(int npc, String talk) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.C_SCRIPT_MAN.getValue());
        mplew.write(4); // ?
        mplew.writeInt(npc);
        mplew.write(3);
        mplew.writeMapleAsciiString(talk);
        mplew.writeInt(0);
        mplew.writeInt(0);
        return mplew.getPacket();
    }
}
