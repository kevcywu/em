package net.login.handler;

import java.util.Calendar;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import net.packetcreator.LoginPacketCreator;
import net.packetcreator.LoginPacketCreator.DeleteCharResult;
import tools.data.input.SeekableLittleEndianAccessor;

public class DeleteCharHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int idate = slea.readInt();
        int cid = slea.readInt();

        int year = idate / 10000;
        int month = (idate - year * 10000) / 100;
        int day = idate - year * 10000 - month * 100;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(0);
        cal.set(year, month - 1, day);
        boolean shallDelete = c.checkBirthDate(cal);

        DeleteCharResult state = DeleteCharResult.INVALID_BIRTHDAY;
        if (shallDelete) {
            state = DeleteCharResult.OK;
            if (!c.deleteCharacter(cid)) {
//                state = 1; //actually something else would be good o.o
            }
        }
        c.announce(LoginPacketCreator.deleteCharResponse(cid, state));
    }
}
