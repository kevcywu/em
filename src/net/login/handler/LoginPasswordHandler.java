package net.login.handler;

import client.MapleCharacter;
import client.MapleClient;
import net.MaplePacketHandler;
import net.login.LoginWorker;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.KoreanDateUtil;
import java.util.Calendar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.packetcreator.LoginPacketCreator;

public class LoginPasswordHandler implements MaplePacketHandler {

    private static final Logger log = LoggerFactory.getLogger(LoginPasswordHandler.class);

    @Override
    public boolean validateState(MapleClient c) {
        return !c.isLoggedIn();
    }

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        String login = slea.readAsciiString(slea.readShort());
        String pwd = slea.readAsciiString(slea.readShort());

        c.setAccountName(login);

        int loginok = 0;
        boolean ipBan = c.hasBannedIP();
        boolean macBan = c.hasBannedMac();
        loginok = c.login(login, pwd, ipBan || macBan);
        Calendar tempbannedTill = c.getTempBanCalendar();
        if (loginok == 0 && (ipBan || macBan)) {
            loginok = 3;

            if (macBan) {
                // this is only an ipban o.O" - maybe we should refactor this a bit so it's more readable
                String[] ipSplit = c.getSession().getRemoteAddress().toString().split(":");
                MapleCharacter.ban(ipSplit[0], "Enforcing account ban, account " + login, false);
            }
        }

        if (loginok != 0) {
            c.getSession().write(LoginPacketCreator.getLoginFailed(loginok));
            return;
        } else if (tempbannedTill.getTimeInMillis() != 0) {
            long tempban = KoreanDateUtil.getTempBanTimestamp(tempbannedTill.getTimeInMillis());
            byte reason = c.getBanReason();
            c.getSession().write(LoginPacketCreator.getTempBan(tempban, reason));
            return;
        }
        if (c.isGm()) {
            LoginWorker.getInstance().registerGMClient(c);
        } else {
            LoginWorker.getInstance().registerClient(c);
        }
    }
}
