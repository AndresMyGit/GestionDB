import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

public class Login implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (Api.options(exchange)) {
            return;
        }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            Api.error(exchange, 405, "Metodo no permitido");
            return;
        }

        try {
            Map<String, Object> body = Api.readJsonObject(exchange);
            String username = Api.str(body, "username");
            String password = Api.str(body, "password");

            if (username.isBlank() || password.isBlank()) {
                Api.error(exchange, 400, "Usuario y contrasena son obligatorios");
                return;
            }

            try (Connection connection = Conexion.getConnection()) {
                Map<String, Object> employee = Api.one(
                        connection,
                        """
                        SELECT p.id, p.nombre, e.id_cargo,
                               CASE e.id_cargo
                                   WHEN 1 THEN 'Gerente'
                                   WHEN 2 THEN 'Empleado'
                                   ELSE 'Sin cargo'
                               END AS cargo
                          FROM empleado e
                          JOIN persona p ON p.id = e.id
                         WHERE (LOWER(p.nombre) = LOWER(?) OR p.documento = ?)
                           AND e.contrasena = ?
                           AND NVL(e.estado, 1) = 1
                        """,
                        statement -> {
                            statement.setString(1, username);
                            statement.setString(2, username);
                            statement.setString(3, password);
                        },
                        resultSet -> {
                            Map<String, Object> row = new LinkedHashMap<>();
                            row.put("employeeId", resultSet.getInt("id"));
                            row.put("name", Api.text(resultSet, "nombre"));
                            row.put("roleId", resultSet.getInt("id_cargo"));
                            row.put("role", Api.text(resultSet, "cargo"));
                            return row;
                        });

                if (employee == null) {
                    Api.error(exchange, 401, "Credenciales invalidas");
                    return;
                }

                Api.ok(exchange, employee);
            }
        } catch (Exception exception) {
            Api.error(exchange, 500, "Error de login: " + exception.getMessage());
        }
    }
}
