import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TestConexion {
    private static final String DEFAULT_TNS_ALIAS = "gestiondb2_high";

    public static void main(String[] args) {
        String explicitUrl = getenv("GESTIONDB_DB_URL");
        String tnsAdmin = resolveTnsAdmin();
        String url = explicitUrl.isBlank()
                ? "jdbc:oracle:thin:@" + getenv("GESTIONDB_TNS_ALIAS", DEFAULT_TNS_ALIAS) + "?TNS_ADMIN=" + tnsAdmin
                : explicitUrl;
        String user = getenv("GESTIONDB_DB_USER");
        String password = getenv("GESTIONDB_DB_PASSWORD");

        if (user.isBlank() || password.isBlank()) {
            System.err.println("Faltan GESTIONDB_DB_USER o GESTIONDB_DB_PASSWORD.");
            System.exit(1);
        }

        System.out.println("Intentando conectar a Oracle...");
        System.out.println("URL: " + url);
        System.out.println("Usuario: " + user);
        System.out.println("Wallet: " + tnsAdmin);

        try (Connection conexion = DriverManager.getConnection(url, user, password)) {
            System.out.println("Conexion establecida con exito.");
            System.out.println("Base de datos: " + conexion.getMetaData().getURL());
            System.out.println("Usuario conectado: " + conexion.getMetaData().getUserName());
        } catch (SQLException error) {
            System.err.println("Error al conectar a la base de datos:");
            error.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static String getenv(String name) {
        return getenv(name, "");
    }

    private static String getenv(String name, String fallback) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private static String resolveTnsAdmin() {
        String explicit = getenv("GESTIONDB_TNS_ADMIN");
        if (!explicit.isBlank()) {
            return Path.of(explicit).toAbsolutePath().normalize().toString();
        }
        return Path.of("wallet").toAbsolutePath().normalize().toString();
    }
}
