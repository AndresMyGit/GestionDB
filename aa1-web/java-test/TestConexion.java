import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TestConexion {
    private static final String DEFAULT_URL = "jdbc:oracle:thin:@gestiondb2_medium?TNS_ADMIN=./wallet";
    private static final String DEFAULT_USER = "ADMIN";
    private static final String DEFAULT_PASSWORD = "DS1405gg2005?";

    public static void main(String[] args) {
        String url = getenv("GESTIONDB_DB_URL", DEFAULT_URL);
        String user = getenv("GESTIONDB_DB_USER", DEFAULT_USER);
        String password = getenv("GESTIONDB_DB_PASSWORD", DEFAULT_PASSWORD);

        System.out.println("Intentando conectar a Oracle...");
        System.out.println("URL: " + url);
        System.out.println("Usuario: " + user);

        try (Connection conexion = DriverManager.getConnection(url, user, password)) {
            System.out.println("Conexión establecida con éxito.");
            System.out.println("Base de datos: " + conexion.getMetaData().getURL());
            System.out.println("Usuario conectado: " + conexion.getMetaData().getUserName());
        } catch (SQLException error) {
            System.err.println("Error al conectar a la base de datos:");
            error.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static String getenv(String name, String fallback) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
