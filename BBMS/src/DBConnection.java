import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    // ---- EDIT THESE THREE LINES TO MATCH YOUR SQL DEVELOPER CONNECTION ----
    private static final String URL  = "jdbc:oracle:thin:@DESKTOP-D1IO6QU:1522:XE";
    private static final String USER = "system";
    private static final String PASS = "system";
    // -------------------------------------------------------------------

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Oracle JDBC driver not found on classpath. Add ojdbc11.jar (or ojdbc8.jar) to your project.", e);
        }
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
