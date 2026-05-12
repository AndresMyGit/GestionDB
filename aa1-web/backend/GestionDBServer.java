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
        
        // Extract Oracle wallet to temp directory
        String[] walletFiles = {"cwallet.sso", "ewallet.p12", "ewallet.pem", "keystore.jks", "ojdbc.properties", "sqlnet.ora", "tnsnames.ora", "truststore.jks"};
        Path tempWallet = Files.createTempDirectory("oracle_wallet");
        for (String file : walletFiles) {
            try (var is = GestionDBServer.class.getResourceAsStream("/" + file)) {
                if (is != null) {
                    Files.copy(is, tempWallet.resolve(file));
                }
            }
        }
        System.setProperty("oracle.net.tns_admin", tempWallet.toString());
        
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
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
        server.createContext("/", exchange -> serveStatic(exchange));
        server.setExecutor(null);
        server.start();

        System.out.println("GestionDB conectado a Oracle");
        System.out.println("URL: http://localhost:" + port + "/");
        System.out.println("Oracle: " + Conexion.url() + " usuario " + Conexion.user());
    }

    private static void serveStatic(HttpExchange exchange) throws IOException {
        if (Api.options(exchange)) {
            return;
        }

        String rawPath = exchange.getRequestURI().getPath();
        String cleanPath = rawPath.equals("/") ? "/index.html" : rawPath;
        try (var is = GestionDBServer.class.getResourceAsStream(cleanPath)) {
            if (is == null) {
                Api.error(exchange, 404, "Archivo no encontrado");
                return;
            }

            byte[] bytes = is.readAllBytes();
            Api.cors(exchange);
            exchange.getResponseHeaders().set("Content-Type", mime(cleanPath));
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }

    private static String mime(String path) {
        String name = path.toLowerCase();
        int dot = name.lastIndexOf('.');
        if (dot >= 0) {
            return MIME.getOrDefault(name.substring(dot), "application/octet-stream");
        }
        return "application/octet-stream";
    }
}
