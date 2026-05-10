import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
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
        int port = Integer.parseInt(System.getenv().getOrDefault("GESTIONDB_PORT", "8081"));
        Path root = Path.of("").toAbsolutePath().normalize();
        if (!Files.exists(root.resolve("index.html"))) {
            root = root.getParent();
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
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
        Path staticRoot = root;
        server.createContext("/", exchange -> serveStatic(exchange, staticRoot));
        server.setExecutor(null);
        server.start();

        System.out.println("GestionDB conectado a Oracle");
        System.out.println("URL: http://localhost:" + port + "/");
        System.out.println("Oracle: " + Conexion.url() + " usuario " + Conexion.user());
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

    private static void registerApi(HttpServer server, String name) {
        server.createContext("/api/" + name, HandlerFactory.create(name));
    }

    private static String mime(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        int dot = name.lastIndexOf('.');
        if (dot >= 0) {
            return MIME.getOrDefault(name.substring(dot), "application/octet-stream");
        }
        return "application/octet-stream";
    }
}
