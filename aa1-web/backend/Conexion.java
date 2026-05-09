import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Conexion {
    private static final String DEFAULT_URL = "jdbc:oracle:thin:@//localhost:1521/freepdb1";
    private static final String DEFAULT_USER = "ANDRES";
    private static final String DEFAULT_PASSWORD = "1234";

    private Conexion() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url(), user(), password());
    }

    public static String url() {
        return value("GESTIONDB_DB_URL", DEFAULT_URL);
    }

    public static String user() {
        return value("GESTIONDB_DB_USER", DEFAULT_USER);
    }

    public static String password() {
        return value("GESTIONDB_DB_PASSWORD", DEFAULT_PASSWORD);
    }

    private static String value(String name, String fallback) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
