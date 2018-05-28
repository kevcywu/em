package database;

import constant.DatabaseConstant;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseConnection {

    private static final ThreadLocal<Connection> con = new ThreadLocalConnection();
    private final static Logger log = LoggerFactory.getLogger(DatabaseConnection.class);

    public static Connection getConnection() {
        return con.get();
    }

    public static void closeAll() throws SQLException {
        for (Connection con : ThreadLocalConnection.allConnections) {
            con.close();
        }
    }

    private static class ThreadLocalConnection extends ThreadLocal<Connection> {

        public static Collection<Connection> allConnections = new LinkedList<Connection>();

        @Override
        protected Connection initialValue() {
            String driver = DatabaseConstant.DRIVER;
            String url = DatabaseConstant.URL;
            String user = DatabaseConstant.USER;
            String password = DatabaseConstant.PASSWORD;
            try {
                Class.forName(driver); // touch the mysql driver
            } catch (ClassNotFoundException e) {
                log.error("ERROR", e);
            }
            try {
                Connection con = DriverManager.getConnection(url, user, password);
                allConnections.add(con);
                return con;
            } catch (SQLException e) {
                log.error("ERROR", e);
                return null;
            }
        }
    }
}
