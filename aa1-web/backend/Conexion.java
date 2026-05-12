import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Conexion {
<<<<<<< HEAD
    private static final String DEFAULT_URL = "jdbc:oracle:thin:@//localhost:1521/freepdb1";
    private static final String DEFAULT_USER = "ANDRES";
    private static final String DEFAULT_PASSWORD = "1234";
    private static final Conexion INSTANCE = new Conexion();
=======
    private static final String DEFAULT_TNS_ALIAS = "gestiondb2_high";
    private static final String DEFAULT_WALLET_DIR = "wallet";
>>>>>>> gestion-render

    private Conexion() {
    }

    public static Conexion getInstance() {
        return INSTANCE;
    }

    public static Connection getConnection() throws SQLException {
<<<<<<< HEAD
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
=======
        validateConfiguration();
        return DriverManager.getConnection(url(), user(), password());
    }

    public static String url() {
        String explicitUrl = value("GESTIONDB_DB_URL");
        if (!explicitUrl.isBlank()) {
            return explicitUrl;
        }
        return "jdbc:oracle:thin:@" + tnsAlias() + "?TNS_ADMIN=" + tnsAdmin();
    }

    public static String user() {
        return value("GESTIONDB_DB_USER");
    }

    public static String password() {
        return value("GESTIONDB_DB_PASSWORD");
    }

    public static String tnsAlias() {
        String alias = value("GESTIONDB_TNS_ALIAS");
        return alias.isBlank() ? DEFAULT_TNS_ALIAS : alias;
    }

    public static String tnsAdmin() {
        String explicitPath = value("GESTIONDB_TNS_ADMIN");
        if (!explicitPath.isBlank()) {
            return Path.of(explicitPath).toAbsolutePath().normalize().toString();
>>>>>>> gestion-render
        }
        return Path.of(DEFAULT_WALLET_DIR).toAbsolutePath().normalize().toString();
    }

    public static boolean isConfigured() {
        return !user().isBlank() && !password().isBlank()
                && (!value("GESTIONDB_DB_URL").isBlank() || Files.exists(Path.of(tnsAdmin())));
    }

    private static void validateConfiguration() {
        if (user().isBlank() || password().isBlank()) {
            throw new IllegalStateException(
                    "Faltan GESTIONDB_DB_USER o GESTIONDB_DB_PASSWORD. Configuralos antes de iniciar la app.");
        }

        if (value("GESTIONDB_DB_URL").isBlank() && !Files.exists(Path.of(tnsAdmin()))) {
            throw new IllegalStateException(
                    "No se encontro el wallet Oracle. Define GESTIONDB_DB_URL o ajusta GESTIONDB_TNS_ADMIN.");
        }
    }

    private static String value(String name) {
        String value = System.getenv(name);
        return value == null ? "" : value.trim();
    }
}
