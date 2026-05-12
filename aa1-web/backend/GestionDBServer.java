import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class GestionDBServer {
    private static final Map<String, String> MIME = Map.ofEntries(
            Map.entry(".html", "text/html; charset=utf-8"),
            Map.entry(".css", "text/css; charset=utf-8"),
            Map.entry(".js", "application/javascript; charset=utf-8"),
            Map.entry(".png", "image/png"),
            Map.entry(".jpg", "image/jpeg"),
            Map.entry(".jpeg", "image/jpeg"),
            Map.entry(".svg", "image/svg+xml"));

    public static void main(String[] args) throws Exception {
        int port = resolvePort();
        Path root = Path.of("").toAbsolutePath().normalize();
        if (!Files.exists(root.resolve("index.html"))) {
            root = root.getParent();
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
<<<<<<< HEAD
        registerApi(server, "login");
        registerApi(server, "resumen");
        registerApi(server, "ventas");
        registerApi(server, "productos");
        registerApi(server, "clientes");
        registerApi(server, "inventario");
        registerApi(server, "credito");
        registerApi(server, "facturas");
        registerApi(server, "cortes");
        registerApi(server, "empleados");
=======
        server.createContext("/api/health", GestionDBServer::health);
        server.createContext("/api/login", new Login());
        server.createContext("/api/resumen", new Resumen());
        server.createContext("/api/ventas", new Ventas());
        server.createContext("/api/productos", new Productos());
        server.createContext("/api/clientes", new Clientes());
        server.createContext("/api/inventario", new Inventario());
        server.createContext("/api/credito", new Credito());
        server.createContext("/api/facturas", new Facturas());
        server.createContext("/api/cortes", new Cortes());
        server.createContext("/api/empleados", new Empleados());
>>>>>>> gestion-render
        Path staticRoot = root;
        server.createContext("/", exchange -> serveStatic(exchange, staticRoot));
        server.setExecutor(null);
        server.start();

        System.out.println("GestionDB listo");
        System.out.println("Puerto HTTP: " + port);
        System.out.println("Raiz estaticos: " + staticRoot);
        System.out.println("Oracle URL: " + Conexion.url());
        System.out.println("Oracle usuario: " + mask(Conexion.user()));
        System.out.println("Health check: /api/health");
    }

    private static void serveStatic(HttpExchange exchange, Path root) throws IOException {
        if (Api.options(exchange)) {
            return;
        }

        String rawPath = exchange.getRequestURI().getPath();
        String cleanPath = rawPath.equals("/") ? "/index.html" : rawPath;
        Path file = root.resolve(cleanPath.substring(1)).normalize();
        if (!file.startsWith(root) || !Files.exists(file) || Files.isDirectory(file)) {
            Api.error(exchange, 404, "Archivo no encontrado");
            return;
        }

        byte[] bytes = Files.readAllBytes(file);
        Api.cors(exchange);
        exchange.getResponseHeaders().set("Content-Type", mime(file));
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

<<<<<<< HEAD
    private static void registerApi(HttpServer server, String name) {
        server.createContext("/api/" + name, HandlerFactory.create(name));
=======
    private static void health(HttpExchange exchange) throws IOException {
        if (Api.options(exchange)) {
            return;
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ok", true);
        response.put("service", "gestiondb");
        response.put("port", resolvePort());
        response.put("configured", Conexion.isConfigured());

        if (!Conexion.isConfigured()) {
            response.put("ok", false);
            response.put("database", "not-configured");
            response.put("message", "Configura GESTIONDB_DB_USER y GESTIONDB_DB_PASSWORD para habilitar Oracle.");
            Api.json(exchange, 503, response);
            return;
        }

        try (Connection connection = Conexion.getConnection()) {
            response.put("database", "up");
            response.put("user", mask(connection.getMetaData().getUserName()));
            Api.json(exchange, 200, response);
        } catch (IllegalStateException | SQLException error) {
            response.put("ok", false);
            response.put("database", "down");
            response.put("message", compactMessage(error));
            Api.json(exchange, 503, response);
        }
>>>>>>> gestion-render
    }

    private static String mime(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        int dot = name.lastIndexOf('.');
        if (dot >= 0) {
            return MIME.getOrDefault(name.substring(dot), "application/octet-stream");
        }
        return "application/octet-stream";
    }

    private static int resolvePort() {
        String value = firstNonBlank(System.getenv("PORT"), System.getenv("GESTIONDB_PORT"), "8081");
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException error) {
            return 8081;
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String compactMessage(Exception error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            return error.getClass().getSimpleName();
        }
        int lineBreak = message.indexOf('\n');
        return lineBreak >= 0 ? message.substring(0, lineBreak).trim() : message.trim();
    }

    private static String mask(String value) {
        if (value == null || value.isBlank()) {
            return "(vacio)";
        }
        if (value.length() <= 2) {
            return "*".repeat(value.length());
        }
        return value.charAt(0) + "*".repeat(value.length() - 2) + value.charAt(value.length() - 1);
    }
}
