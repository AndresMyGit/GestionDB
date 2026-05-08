import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
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
                    """
                    SELECT p.codigobarras, p.nombre, p.descripcion, p.precio, p.stock,
                           c.idcategoria, c.nombre AS categoria
                      FROM producto p
                      LEFT JOIN categoria c ON c.idcategoria = p.idcategoria
                     ORDER BY p.nombre
                    """,
                    null,
                    resultSet -> productRow(resultSet));

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("categories", categories);
            response.put("products", products);
            response.put("total", products.size());
            response.put("categoryCount", categories.size());
            response.put("lowStockCount", Api.scalarInt(connection, "SELECT COUNT(*) FROM producto WHERE stock < 10"));
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
        try (CallableStatement statement = connection.prepareCall("{ ? = call actualizar_producto(?, ?, ?, ?, ?) }")) {
            statement.registerOutParameter(1, Types.VARCHAR);
            statement.setInt(2, Api.integer(body, "code", 0));
            statement.setString(3, Api.str(body, "name"));
            statement.setInt(4, Api.integer(body, "categoryId", 0));
            statement.setString(5, Api.str(body, "description"));
            statement.setDouble(6, Api.decimal(body, "price", 0));
            statement.execute();
            return statement.getString(1);
        }
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
