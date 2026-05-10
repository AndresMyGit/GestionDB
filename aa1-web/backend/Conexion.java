import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Conexion {
    private static final String DEFAULT_URL = "jdbc:oracle:thin:@//localhost:1521/freepdb1";
    private static final String DEFAULT_USER = "ANDRES";
    private static final String DEFAULT_PASSWORD = "1234";
    private static final Conexion INSTANCE = new Conexion();

    private Conexion() {
    }

    public static Conexion getInstance() {
        return INSTANCE;
    }

    public static Connection getConnection() throws SQLException {
        return getInstance().openConnection();
    }

    public static String url() {
        return getInstance().databaseUrl();
    }

    public static String user() {
        return getInstance().databaseUser();
    }

    public static String password() {
        return getInstance().databasePassword();
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(databaseUrl(), databaseUser(), databasePassword());
    }

    private String databaseUrl() {
        return value("GESTIONDB_DB_URL", DEFAULT_URL);
    }

    private String databaseUser() {
        return value("GESTIONDB_DB_USER", DEFAULT_USER);
    }

    private String databasePassword() {
        return value("GESTIONDB_DB_PASSWORD", DEFAULT_PASSWORD);
    }

    private String value(String name, String fallback) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
