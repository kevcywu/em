package net;

import client.MapleClient;
import constant.ServerConstant;
import net.channel.ChannelServer;
import net.login.LoginWorker;
import tools.MapleAESOFB;
import net.packetcreator.MaplePacketCreator;
import tools.data.input.ByteArrayByteStream;
import tools.data.input.GenericSeekableLittleEndianAccessor;
import tools.data.input.SeekableLittleEndianAccessor;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapleServerHandler extends IoHandlerAdapter {

    private final static Logger log = LoggerFactory.getLogger(MapleServerHandler.class);
    private PacketProcessor processor;
    private int channel = -1;

    public MapleServerHandler(PacketProcessor processor) {
        this.processor = processor;
    }

    public MapleServerHandler(PacketProcessor processor, int channel) {
        this.processor = processor;
        this.channel = channel;
    }

    @Override
    public void messageSent(IoSession session, Object message) throws Exception {
        Runnable r = ((MaplePacket) message).getOnSend();
        if (r != null) {
            r.run();
        }
        super.messageSent(session, message);
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        MapleClient client = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);
        log.error(MapleClient.getLogMessage(client, cause.getMessage()), cause);
        // ChannelServer.getInstance(1).broadcastPacket(
        // MaplePacketCreator.getChatText(30000, "Exception: " + cause.getClass().getName() + ": " +
        // cause.getMessage()));
        // for (int i = 0; i < cause.getStackTrace().length; i++) {
        // StackTraceElement ste = cause.getStackTrace()[i];
        // ChannelServer.getInstance(1).broadcastPacket(MaplePacketCreator.getChatText(30000, ste.toString()));
        // if (i > 2) {
        // break;
        // }
        // }
    }

    @Override
    public void sessionOpened(IoSession session) throws Exception {
        log.info("IoSession with {} opened", session.getRemoteAddress());

        if (channel > -1) {
            if (ChannelServer.getInstance(channel).isShutdown()) {
                session.close();
                return;
            }
        }

        byte key[] = {0x13, 0x00, 0x00, 0x00, 0x08, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00, (byte) 0xB4, 0x00, 0x00,
            0x00, 0x1B, 0x00, 0x00, 0x00, 0x0F, 0x00, 0x00, 0x00, 0x33, 0x00, 0x00, 0x00, 0x52, 0x00, 0x00, 0x00};
        byte ivRecv[] = {70, 114, 122, 82};
        byte ivSend[] = {82, 48, 120, 115};

        ivRecv[3] = (byte) (Math.random() * 255);
        ivSend[3] = (byte) (Math.random() * 255);
        MapleAESOFB sendCypher = new MapleAESOFB(key, ivSend, (short) (0xFFFF - ServerConstant.MAPLE_VERSION));
        MapleAESOFB recvCypher = new MapleAESOFB(key, ivRecv, ServerConstant.MAPLE_VERSION);

        MapleClient client = new MapleClient(sendCypher, recvCypher, session);
        client.setChannel(channel);

        session.write(MaplePacketCreator.getHello(ServerConstant.MAPLE_VERSION, ivSend, ivRecv));
        session.setAttribute(MapleClient.CLIENT_KEY, client);
        session.setIdleTime(IdleStatus.READER_IDLE, 30);
        session.setIdleTime(IdleStatus.WRITER_IDLE, 30);
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        synchronized (session) {
            MapleClient client = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);

            if (client != null) {
                client.disconnect();
                LoginWorker.getInstance().deregisterClient(client);
                session.removeAttribute(MapleClient.CLIENT_KEY);
            }
        }
        super.sessionClosed(session);
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        SeekableLittleEndianAccessor slea = new GenericSeekableLittleEndianAccessor(new ByteArrayByteStream((byte[]) message));
//        log.info("Recv: {}", slea.toString());
        short packetId = slea.readShort();
        MapleClient client = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);
        MaplePacketHandler packetHandler = processor.getHandler(packetId);

        if (packetHandler != null && packetHandler.validateState(client)) {
            try {
                packetHandler.handlePacket(slea, client);
            } catch (Throwable t) {
                log.error(MapleClient.getLogMessage(client, "Exception during processing packet: " + packetHandler.getClass().getName() + ": " + t.getMessage()), t);
            }
        }

    }

    @Override
    public void sessionIdle(final IoSession session, final IdleStatus status) throws Exception {
        MapleClient client = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);

        if (client != null && client.getPlayer() != null && log.isTraceEnabled()) {
            log.trace("Player {} went idle", client.getPlayer().getName());
        }

        if (client != null) {
            client.sendPing();
        }

        super.sessionIdle(session, status);
    }
}
