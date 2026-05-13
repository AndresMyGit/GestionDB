import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Productos implements HttpHandler {
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
            Api.error(exchange, 500, "Error en productos: " + exception.getMessage());
        }
    }

    private void get(HttpExchange exchange) throws Exception {
        try (Connection connection = Conexion.getConnection()) {
            List<Map<String, Object>> categories = Api.rows(
                    connection,
                    "SELECT idcategoria, nombre FROM categoria ORDER BY nombre",
                    null,
                    resultSet -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("id", resultSet.getInt("idcategoria"));
                        row.put("name", Api.text(resultSet, "nombre"));
                        return row;
                    });

            List<Map<String, Object>> products = Api.rows(
                    connection,
                    productSelectSql() + " ORDER BY vp.nombre",
                    null,
                    resultSet -> productRow(resultSet));

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("categories", categories);
            response.put("products", products);
            response.put("total", products.size());
            response.put("categoryCount", categories.size());
            response.put("lowStockCount", Api.scalarInt(connection,
                    "SELECT COUNT(*) FROM producto p JOIN vista_productos vp ON vp.codigo = p.codigobarras WHERE p.stock < 10"));
            Api.ok(exchange, response);
        }
    }

    private void post(HttpExchange exchange) throws Exception {
        Map<String, Object> body = Api.readJsonObject(exchange);
        String action = Api.str(body, "action");
        if (action.isBlank()) {
            action = "create";
        }

        try (Connection connection = Conexion.getConnection()) {
            if (!Api.requireManager(exchange, connection)) {
                return;
            }
            connection.setAutoCommit(false);
            String message;
            if ("update".equalsIgnoreCase(action)) {
                message = updateProduct(connection, body);
            } else {
                message = createProduct(connection, body);
            }

            if (message.startsWith("Error")) {
                connection.rollback();
                Api.error(exchange, 400, message);
                return;
            }

            connection.commit();
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("message", message);
            Api.ok(exchange, response);
        }
    }

    private String createProduct(Connection connection, Map<String, Object> body) throws Exception {
        try (CallableStatement statement = connection.prepareCall("{ ? = call insertar_producto(?, ?, ?, ?, ?, ?) }")) {
            statement.registerOutParameter(1, Types.VARCHAR);
            statement.setInt(2, Api.integer(body, "code", 0));
            statement.setString(3, Api.str(body, "name"));
            statement.setInt(4, Api.integer(body, "categoryId", 0));
            statement.setString(5, Api.str(body, "description"));
            statement.setDouble(6, Api.decimal(body, "price", 0));
            statement.setDouble(7, Api.decimal(body, "stock", 0));
            statement.execute();
            return statement.getString(1);
        }
    }

    private String updateProduct(Connection connection, Map<String, Object> body) throws Exception {
        int code = Api.integer(body, "code", 0);
        String name = Api.str(body, "name");
        int categoryId = Api.integer(body, "categoryId", 0);
        String description = Api.str(body, "description");
        double price = Api.decimal(body, "price", 0);
        double stock = Api.decimal(body, "stock", 0);

        if (code <= 0 || name.isBlank() || categoryId <= 0 || description.isBlank() || price <= 0 || stock < 0) {
            return "Error: Completa todos los datos del producto";
        }

        BigDecimal currentStock = currentStock(connection, code);
        if (currentStock == null) {
            return "Error: Producto no encontrado";
        }

        String message;
        try (CallableStatement statement = connection.prepareCall("{ ? = call actualizar_producto(?, ?, ?, ?, ?) }")) {
            statement.registerOutParameter(1, Types.VARCHAR);
            statement.setInt(2, code);
            statement.setString(3, name);
            statement.setInt(4, categoryId);
            statement.setString(5, description);
            statement.setDouble(6, price);
            statement.execute();
            message = statement.getString(1);
        }

        if (message != null && message.startsWith("Error")) {
            return message;
        }

        BigDecimal requestedStock = BigDecimal.valueOf(stock);
        if (requestedStock.compareTo(currentStock) != 0) {
            try (CallableStatement statement = connection.prepareCall(
                    "{ call procesar_movimiento_inventario(?, ?, ?) }")) {
                statement.setInt(1, code);
                statement.setBigDecimal(2, requestedStock);
                statement.setInt(3, 4);
                statement.execute();
            }
        }

        return message == null || message.isBlank() ? "Producto actualizado" : message;
    }

    private BigDecimal currentStock(Connection connection, int code) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT stock FROM producto WHERE codigobarras = ? FOR UPDATE")) {
            statement.setInt(1, code);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getBigDecimal("stock") : null;
            }
        }
    }

    static String productSelectSql() {
        return """
               SELECT p.codigobarras, vp.nombre, p.descripcion, vp.precio, p.stock,
                      p.idcategoria, vp.categoria
                 FROM vista_productos vp
                 JOIN producto p ON p.codigobarras = vp.codigo
               """;
    }

    static Map<String, Object> productRow(java.sql.ResultSet resultSet) throws java.sql.SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("code", resultSet.getInt("codigobarras"));
        row.put("name", Api.text(resultSet, "nombre"));
        row.put("description", Api.text(resultSet, "descripcion"));
        row.put("price", resultSet.getDouble("precio"));
        row.put("stock", resultSet.getDouble("stock"));
        row.put("categoryId", resultSet.getInt("idcategoria"));
        row.put("category", Api.text(resultSet, "categoria"));
        row.put("unit", "kg");
        return row;
    }
}
