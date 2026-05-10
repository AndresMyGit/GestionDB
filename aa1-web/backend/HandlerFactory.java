import com.sun.net.httpserver.HttpHandler;

public final class HandlerFactory {
    private HandlerFactory() {
    }

    public static HttpHandler create(String name) {
        return switch (name) {
            case "login" -> new Login();
            case "resumen" -> new Resumen();
            case "ventas" -> new Ventas();
            case "productos" -> new Productos();
            case "clientes" -> new Clientes();
            case "inventario" -> new Inventario();
            case "credito" -> new Credito();
            case "facturas" -> new Facturas();
            case "cortes" -> new Cortes();
            case "empleados" -> new Empleados();
            default -> throw new IllegalArgumentException("Handler no registrado: " + name);
        };
    }
}
