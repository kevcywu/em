package tools.data.output;

import java.io.ByteArrayOutputStream;

import net.ByteArrayMaplePacket;
import net.MaplePacket;
import net.WriteToFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.HexTool;

public class MaplePacketLittleEndianWriter extends GenericLittleEndianWriter {

    private final static Logger log = LoggerFactory.getLogger(MaplePacketLittleEndianWriter.class);
    private ByteArrayOutputStream baos;

    public MaplePacketLittleEndianWriter() {
        this(32);
    }

    public MaplePacketLittleEndianWriter(int size) {
        this.baos = new ByteArrayOutputStream(size);
        setByteOutputStream(new BAOSByteOutputStream(baos));
    }

    public MaplePacket getPacket() {
//        log.info("Send: {}", HexTool.toString(baos.toByteArray()));
        return new ByteArrayMaplePacket(baos.toByteArray());
    }

    public void record() {
        WriteToFile.write("\n" + HexTool.toString(baos.toByteArray()));
    }

    @Override
    public String toString() {
        return HexTool.toString(baos.toByteArray());
    }
}
