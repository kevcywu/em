package constant;

public class ServerConstant {

    public final static short MAPLE_VERSION = 62;

    public static final int CHANNEL_COUNT = 4;
    public static final int EXP_RATE = 8;
    public static final int MESO_RATE = 8;

    public static final String SERVER_IP = "localhost";
    public static final String[] EVENTS = {"lolcastle", "3rdjob", "ZakumPQ", "KerningPQ"};
    public static final boolean faekchar = false;
    public static final int USER_LIMIT = 50;
    public static String SERVER_MESSAGE = "The sweetest honey Is loathsome in his own deliciousness And in the taste confounds the appetite:Therefore love moderately; long love doth so;Too swift arrives as tardy as too slow.";

    // time in milliseconds between loginqueue runs
    public static final int LOGIN_INTERVAL = 2000;
    // time in milliseconds between ranking updates (default: 30 minutes)
    public static final long RANKING_INTERVAL = 1800000;
}
