import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Clientes implements HttpHandler {
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
            Api.error(exchange, 500, "Error en clientes: " + exception.getMessage());
        }
    }

    private void get(HttpExchange exchange) throws Exception {
        try (Connection connection = Conexion.getConnection()) {
            List<Map<String, Object>> clients = Api.rows(
                    connection,
                    """
                    SELECT p.id, p.nombre, p.documento, p.telefono, c.direccion, c.estadocredito,
                           NVL((
                               SELECT SUM(cr.saldo)
                                 FROM credito cr
                                WHERE cr.cliente = c.id
                                  AND cr.estado <> 2
                           ), 0) AS deuda
                      FROM cliente c
                      JOIN persona p ON p.id = c.id
                     ORDER BY p.nombre
                    """,
                    null,
                    resultSet -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("id", resultSet.getInt("id"));
                        row.put("name", Api.text(resultSet, "nombre"));
                        row.put("document", Api.text(resultSet, "documento"));
                        row.put("phone", Api.text(resultSet, "telefono"));
                        row.put("address", Api.text(resultSet, "direccion"));
                        row.put("creditEnabled", resultSet.getInt("estadocredito") == 1);
                        row.put("debt", resultSet.getDouble("deuda"));
                        return row;
                    });

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("clients", clients);
            Api.ok(exchange, response);
        }
    }

    private void post(HttpExchange exchange) throws Exception {
        Map<String, Object> body = Api.readJsonObject(exchange);
        int clientId = Api.integer(body, "clientId", 0);
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        if (clientId <= 0) {
            Api.error(exchange, 400, "Cliente invalido");
            return;
        }

        try (Connection connection = Conexion.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "UPDATE cliente SET estadocredito = ? WHERE id = ?")) {
            statement.setInt(1, enabled ? 1 : 0);
            statement.setInt(2, clientId);
            int updated = statement.executeUpdate();
            if (updated == 0) {
                Api.error(exchange, 404, "Cliente no encontrado");
                return;
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("message", enabled ? "Credito habilitado" : "Credito bloqueado");
            Api.ok(exchange, response);
        }
    }
}
