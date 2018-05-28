package net.login.handler;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import tools.data.input.SeekableLittleEndianAccessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.packetcreator.LoginPacketCreator;
import net.packetcreator.LoginPacketCreator.PinCodeResult;

public class AfterLoginHandler extends AbstractMaplePacketHandler {

    private static final Logger log = LoggerFactory.getLogger(AfterLoginHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        byte c2 = slea.readByte();
        byte c3 = slea.readByte();
        if (c2 == 1 && c3 == 1) {
            // Official requests the pin here - but pins suck so we just accept
            c.getSession().write(LoginPacketCreator.getPinCodeResult(PinCodeResult.PIN_ACCEPTED));
        } else if (c2 == 1 && c3 == 0) {
            slea.seek(8);
            String pin = slea.readMapleAsciiString();
            log.info("Received Pin: " + pin);
//            c.getSession().write(pin.equals("1234") ? MaplePacketCreator.pinAccepted() : MaplePacketCreator.requestPinAfterFailure());
            c.getSession().write(LoginPacketCreator.getPinCodeResult(PinCodeResult.PIN_ACCEPTED));
        } else {
            // abort login attempt
        }
    }
}
