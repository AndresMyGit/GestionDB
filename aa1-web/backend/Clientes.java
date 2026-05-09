import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
        String action = Api.str(body, "action");
        if (action.isBlank()) {
            action = body.containsKey("name") || body.containsKey("document") ? "create" : "toggleCredit";
        }

        try (Connection connection = Conexion.getConnection()) {
            if ("create".equalsIgnoreCase(action)) {
                createClient(exchange, connection, body);
                return;
            }
            toggleCredit(exchange, connection, body);
        }
    }

    private void toggleCredit(HttpExchange exchange, Connection connection, Map<String, Object> body) throws Exception {
        int clientId = Api.integer(body, "clientId", 0);
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        if (clientId <= 0) {
            Api.error(exchange, 400, "Cliente invalido");
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement(
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

    private void createClient(HttpExchange exchange, Connection connection, Map<String, Object> body) throws Exception {
        String name = Api.str(body, "name").trim();
        String document = Api.str(body, "document").trim();
        String phone = Api.str(body, "phone").trim();
        String address = Api.str(body, "address").trim();
        boolean creditEnabled = Boolean.TRUE.equals(body.get("creditEnabled"));

        if (name.isBlank() || document.isBlank()) {
            Api.error(exchange, 400, "Nombre y documento son obligatorios");
            return;
        }

        connection.setAutoCommit(false);
        try {
            if (documentExists(connection, document)) {
                connection.rollback();
                Api.error(exchange, 400, "Ya existe un cliente con ese documento");
                return;
            }

            int clientId = nextClientId(connection);
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO persona (id, nombre, documento, telefono) VALUES (?, ?, ?, ?)")) {
                statement.setInt(1, clientId);
                statement.setString(2, name);
                statement.setString(3, document);
                if (phone.isBlank()) {
                    statement.setNull(4, java.sql.Types.VARCHAR);
                } else {
                    statement.setString(4, phone);
                }
                statement.executeUpdate();
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO cliente (id, direccion, estadocredito) VALUES (?, ?, ?)")) {
                statement.setInt(1, clientId);
                if (address.isBlank()) {
                    statement.setNull(2, java.sql.Types.VARCHAR);
                } else {
                    statement.setString(2, address);
                }
                statement.setInt(3, creditEnabled ? 1 : 0);
                statement.executeUpdate();
            }

            connection.commit();
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("message", "Cliente creado");
            response.put("clientId", clientId);
            response.put("name", name);
            response.put("document", document);
            Api.ok(exchange, response);
        } catch (Exception exception) {
            connection.rollback();
            throw exception;
        }
    }

    private boolean documentExists(Connection connection, String document) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM persona WHERE documento = ?")) {
            statement.setString(1, document);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private int nextClientId(Connection connection) throws Exception {
        String[] sequences = {
                "persona_id_seq",
                "persona_seq",
                "persona_idpersona_seq",
                "cliente_id_seq",
                "cliente_seq"
        };

        for (String sequence : sequences) {
            try {
                return nextValue(connection, sequence);
            } catch (SQLException ignored) {
                // Usa la primera secuencia existente; si no hay, cae al calculo por maximo.
            }
        }

        return Api.scalarInt(connection, "SELECT NVL(MAX(id), 0) + 1 FROM persona");
    }

    private int nextValue(Connection connection, String sequence) throws Exception {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT " + sequence + ".NEXTVAL FROM dual")) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
}
