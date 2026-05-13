import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Empleados implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (Api.options(exchange)) {
            return;
        }

        try (Connection connection = Conexion.getConnection()) {
            if (!Api.requireManager(exchange, connection)) {
                return;
            }

            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                get(exchange, connection);
                return;
            }
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                post(exchange, connection);
                return;
            }
            Api.error(exchange, 405, "Metodo no permitido");
        } catch (Exception exception) {
            Api.error(exchange, 500, "Error en empleados: " + exception.getMessage());
        }
    }

    private void get(HttpExchange exchange, Connection connection) throws Exception {
        List<Map<String, Object>> employees = Api.rows(
                connection,
                """
                SELECT p.id, p.nombre, p.documento, p.telefono,
                       e.salario, e.id_cargo, NVL(e.estado, 1) AS estado,
                       CASE e.id_cargo
                           WHEN 1 THEN 'Gerente'
                           WHEN 2 THEN 'Empleado'
                           ELSE 'Sin cargo'
                       END AS cargo
                  FROM empleado e
                  JOIN persona p ON p.id = e.id
                 ORDER BY NVL(e.estado, 1), p.nombre
                """,
                null,
                resultSet -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", resultSet.getInt("id"));
                    row.put("name", Api.text(resultSet, "nombre"));
                    row.put("document", Api.text(resultSet, "documento"));
                    row.put("phone", Api.text(resultSet, "telefono"));
                    row.put("salary", resultSet.getDouble("salario"));
                    row.put("roleId", resultSet.getInt("id_cargo"));
                    row.put("role", Api.text(resultSet, "cargo"));
                    row.put("stateId", resultSet.getInt("estado"));
                    row.put("state", resultSet.getInt("estado") == 1 ? "Activo" : "Despedido");
                    return row;
                });

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("employees", employees);
        Api.ok(exchange, response);
    }

    private void post(HttpExchange exchange, Connection connection) throws Exception {
        Map<String, Object> body = Api.readJsonObject(exchange);
        String action = Api.str(body, "action");
        if ("dismiss".equalsIgnoreCase(action)) {
            dismiss(exchange, connection, body);
            return;
        }
        create(exchange, connection, body);
    }

    private void create(HttpExchange exchange, Connection connection, Map<String, Object> body) throws Exception {
        String name = Api.str(body, "name");
        String document = Api.str(body, "document");
        String phone = Api.str(body, "phone");
        String password = Api.str(body, "password");
        int roleId = Api.integer(body, "roleId", 2);
        double salary = Api.decimal(body, "salary", 0);

        if (name.isBlank() || document.isBlank() || password.isBlank() || (roleId != 1 && roleId != 2)) {
            Api.error(exchange, 400, "Nombre, documento, contrasena y cargo son obligatorios");
            return;
        }

        connection.setAutoCommit(false);
        try {
            String message;
            try (CallableStatement statement = connection.prepareCall(
                    "{ ? = call insertar_empleado(?, ?, ?, ?, ?, ?) }")) {
                statement.registerOutParameter(1, Types.VARCHAR);
                statement.setString(2, name);
                statement.setString(3, document);
                statement.setString(4, phone);
                statement.setString(5, password);
                statement.setDouble(6, salary);
                statement.setInt(7, roleId);
                statement.execute();
                message = statement.getString(1);
            }

            if (message != null && message.startsWith("Error")) {
                connection.rollback();
                Api.error(exchange, 400, message);
                return;
            }

            int employeeId = employeeIdByDocument(connection, document);
            connection.commit();
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("message", message == null || message.isBlank() ? "Empleado creado" : message);
            response.put("employeeId", employeeId);
            Api.ok(exchange, response);
        } catch (Exception exception) {
            connection.rollback();
            throw exception;
        }
    }

    private void dismiss(HttpExchange exchange, Connection connection, Map<String, Object> body) throws Exception {
        int employeeId = Api.integer(body, "employeeId", 0);
        if (employeeId <= 0) {
            Api.error(exchange, 400, "Empleado invalido");
            return;
        }
        if (employeeId == Api.employeeId(exchange)) {
            Api.error(exchange, 400, "No puedes despedir tu propio usuario");
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE empleado SET estado = 2 WHERE id = ?")) {
            statement.setInt(1, employeeId);
            int updated = statement.executeUpdate();
            if (updated == 0) {
                Api.error(exchange, 404, "Empleado no encontrado");
                return;
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Empleado despedido");
        Api.ok(exchange, response);
    }

    private int employeeIdByDocument(Connection connection, String document) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM persona WHERE documento = ?")) {
            statement.setString(1, document);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt("id") : 0;
            }
        }
    }
}
