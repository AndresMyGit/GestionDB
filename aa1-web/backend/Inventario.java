import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Inventario implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (Api.options(exchange)) {
            return;
        }

        try {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                get(exchange);
                return;
            }
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                post(exchange);
                return;
            }
            Api.error(exchange, 405, "Metodo no permitido");
        } catch (Exception exception) {
            Api.error(exchange, 500, "Error en inventario: " + exception.getMessage());
        }
    }

    private void get(HttpExchange exchange) throws Exception {
        try (Connection connection = Conexion.getConnection()) {
            List<Map<String, Object>> products = Api.rows(
                    connection,
                    """
                    SELECT p.codigobarras, p.nombre, p.descripcion, p.precio, p.stock,
                           c.idcategoria, c.nombre AS categoria
                      FROM producto p
                      LEFT JOIN categoria c ON c.idcategoria = p.idcategoria
                     ORDER BY p.nombre
                    """,
                    null,
                    resultSet -> Productos.productRow(resultSet));

            List<Map<String, Object>> lowStock = Api.rows(
                    connection,
                    """
                    SELECT p.codigobarras, p.nombre, p.stock
                      FROM producto p
                     WHERE p.stock < 10
                     ORDER BY p.stock ASC, p.nombre
                    """,
                    null,
                    resultSet -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("code", resultSet.getInt("codigobarras"));
                        row.put("name", Api.text(resultSet, "nombre"));
                        row.put("stock", resultSet.getDouble("stock"));
                        return row;
                    });

            List<Map<String, Object>> movements = Api.rows(
                    connection,
                    """
                    SELECT mi.id, mi.codigobarras, p.nombre AS producto, mi.cantidad, mi.fecha,
                           m.nombre AS motivo
                      FROM movimiento_inventario mi
                      LEFT JOIN producto p ON p.codigobarras = mi.codigobarras
                      LEFT JOIN motivo_movimiento m ON m.id = mi.id_motivo
                     ORDER BY mi.fecha DESC, mi.id DESC
                     FETCH FIRST 100 ROWS ONLY
                    """,
                    null,
                    resultSet -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("id", resultSet.getInt("id"));
                        row.put("productCode", resultSet.getInt("codigobarras"));
                        row.put("product", Api.text(resultSet, "producto"));
                        row.put("quantity", resultSet.getDouble("cantidad"));
                        row.put("date", Api.timestamp(resultSet, "fecha"));
                        row.put("motive", Api.text(resultSet, "motivo"));
                        return row;
                    });

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("products", products);
            response.put("lowStock", lowStock);
            response.put("movements", movements);
            Api.ok(exchange, response);
        }
    }

    private void post(HttpExchange exchange) throws Exception {
        Map<String, Object> body = Api.readJsonObject(exchange);
        int code = Api.integer(body, "code", 0);
        double amount = Api.decimal(body, "amount", 0);
        String mode = Api.str(body, "mode");
        if (code <= 0 || amount <= 0) {
            Api.error(exchange, 400, "Producto o cantidad invalida");
            return;
        }

        try (Connection connection = Conexion.getConnection()) {
            connection.setAutoCommit(false);
            if ("manualOut".equals(mode)) {
                manualOut(connection, code, amount);
            } else {
                int motive = switch (mode) {
                    case "return" -> 3;
                    case "adjust" -> 4;
                    default -> 1;
                };
                try (CallableStatement statement = connection.prepareCall(
                        "{ call procesar_movimiento_inventario(?, ?, ?) }")) {
                    statement.setInt(1, code);
                    statement.setDouble(2, amount);
                    statement.setInt(3, motive);
                    statement.execute();
                }
            }
            connection.commit();

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("message", "Inventario actualizado");
            Api.ok(exchange, response);
        }
    }

    private void manualOut(Connection connection, int code, double amount) throws Exception {
        double stock;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT stock FROM producto WHERE codigobarras = ? FOR UPDATE")) {
            statement.setInt(1, code);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("Producto no encontrado");
                }
                stock = resultSet.getDouble("stock");
            }
        }

        if (amount > stock) {
            throw new IllegalArgumentException("La salida manual supera el stock actual");
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE producto SET stock = stock - ? WHERE codigobarras = ?")) {
            statement.setDouble(1, amount);
            statement.setInt(2, code);
            statement.executeUpdate();
        }

        try (CallableStatement statement = connection.prepareCall(
                "{ call registrar_movimiento_inventario(?, ?, ?) }")) {
            statement.setInt(1, code);
            statement.setDouble(2, -amount);
            statement.setInt(3, 4);
            statement.execute();
        }
    }
}
