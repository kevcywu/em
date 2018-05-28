package tools.data.input;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InputStreamByteStream implements ByteInputStream {

    private InputStream is;
    private long read = 0;
    private static Logger log = LoggerFactory.getLogger(InputStreamByteStream.class);

    public InputStreamByteStream(InputStream is) {
        this.is = is;
    }

    @Override
    public int readByte() {
        int temp;
        try {
            temp = is.read();
            if (temp == -1) {
                throw new RuntimeException("EOF");
            }
            read++;
            return temp;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long getBytesRead() {
        return read;
    }

    @Override
    public long available() {
        try {
            return is.available();
        } catch (IOException e) {
            log.error("ERROR", e);
            return 0;
        }
    }
}
