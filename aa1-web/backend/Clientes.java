import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
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
        String search = Api.query(exchange, "search").trim().toLowerCase();
        int limit = boundedLimit(Api.query(exchange, "limit"));

        try (Connection connection = Conexion.getConnection()) {
            List<Map<String, Object>> clients = search.isBlank()
                    ? listClients(connection)
                    : searchClients(connection, search, limit);

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
            if ("update".equalsIgnoreCase(action)) {
                updateClient(exchange, connection, body);
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

        if (document.isBlank()) {
            Api.error(exchange, 400, "El documento es obligatorio");
            return;
        }

        connection.setAutoCommit(false);
        try {
            Map<String, Object> person = findPersonByDocument(connection, document);
            boolean reusedPerson = person != null;

            int clientId;
            String resolvedName;
            String resolvedPhone;

            if (reusedPerson) {
                clientId = ((Number) person.get("id")).intValue();
                if (clientExists(connection, clientId)) {
                    connection.rollback();
                    Api.error(exchange, 400, "Ya existe un cliente con ese documento");
                    return;
                }
                resolvedName = String.valueOf(person.get("name"));
                resolvedPhone = String.valueOf(person.get("phone"));
            } else {
                if (name.isBlank()) {
                    connection.rollback();
                    Api.error(exchange, 400, "Si el documento no existe, el nombre es obligatorio");
                    return;
                }

                clientId = insertPersona(connection, name, document, phone);
                resolvedName = name;
                resolvedPhone = phone;
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
            response.put("message", reusedPerson
                    ? "Cliente creado usando la persona ya registrada"
                    : "Cliente creado");
            response.put("clientId", clientId);
            response.put("name", resolvedName);
            response.put("document", document);
            response.put("phone", resolvedPhone);
            response.put("address", address);
            response.put("creditEnabled", creditEnabled);
            response.put("debt", 0);
            response.put("reusedPerson", reusedPerson);
            Api.ok(exchange, response);
        } catch (Exception exception) {
            connection.rollback();
            throw exception;
        }
    }

    private void updateClient(HttpExchange exchange, Connection connection, Map<String, Object> body) throws Exception {
        int clientId = Api.integer(body, "clientId", 0);
        String name = Api.str(body, "name").trim();
        String document = Api.str(body, "document").trim();
        String phone = Api.str(body, "phone").trim();
        String address = Api.str(body, "address").trim();
        boolean creditEnabled = Boolean.TRUE.equals(body.get("creditEnabled"));

        if (clientId <= 0 || name.isBlank() || document.isBlank()) {
            Api.error(exchange, 400, "Cliente, nombre y documento son obligatorios");
            return;
        }

        connection.setAutoCommit(false);
        try {
            if (documentExistsForOtherClient(connection, document, clientId)) {
                connection.rollback();
                Api.error(exchange, 400, "Ya existe otra persona con ese documento");
                return;
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE persona SET nombre = ?, documento = ?, telefono = ? WHERE id = ?")) {
                statement.setString(1, name);
                statement.setString(2, document);
                if (phone.isBlank()) {
                    statement.setNull(3, java.sql.Types.VARCHAR);
                } else {
                    statement.setString(3, phone);
                }
                statement.setInt(4, clientId);
                int updated = statement.executeUpdate();
                if (updated == 0) {
                    connection.rollback();
                    Api.error(exchange, 404, "Cliente no encontrado");
                    return;
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE cliente SET direccion = ?, estadocredito = ? WHERE id = ?")) {
                if (address.isBlank()) {
                    statement.setNull(1, java.sql.Types.VARCHAR);
                } else {
                    statement.setString(1, address);
                }
                statement.setInt(2, creditEnabled ? 1 : 0);
                statement.setInt(3, clientId);
                statement.executeUpdate();
            }

            connection.commit();
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("message", "Cliente actualizado");
            response.put("clientId", clientId);
            response.put("name", name);
            response.put("document", document);
            response.put("phone", phone);
            response.put("address", address);
            response.put("creditEnabled", creditEnabled);
            Api.ok(exchange, response);
        } catch (Exception exception) {
            connection.rollback();
            throw exception;
        }
    }

    private List<Map<String, Object>> listClients(Connection connection) throws Exception {
        return Api.rows(
                connection,
                """
                SELECT p.id, p.nombre, p.documento, p.telefono, c.direccion, c.estadocredito,
                       NVL((
                           SELECT SUM(vc.saldo)
                             FROM vista_creditos_resumida vc
                            WHERE vc.id_cliente = c.id
                              AND UPPER(vc.estado_credito) <> 'PAGADO'
                       ), 0) AS deuda
                  FROM cliente c
                  JOIN persona p ON p.id = c.id
                 ORDER BY p.nombre
                """,
                null,
                this::clientRow);
    }

    private List<Map<String, Object>> searchClients(Connection connection, String search, int limit) throws Exception {
        String exact = search;
        String prefix = search + "%";
        String contains = "%" + search + "%";

        return Api.rows(
                connection,
                """
                SELECT id, nombre, documento, telefono, direccion, estadocredito, deuda
                  FROM (
                        SELECT p.id, p.nombre, p.documento, p.telefono, c.direccion, c.estadocredito,
                               NVL((
                                   SELECT SUM(vc.saldo)
                                     FROM vista_creditos_resumida vc
                                    WHERE vc.id_cliente = c.id
                                      AND UPPER(vc.estado_credito) <> 'PAGADO'
                               ), 0) AS deuda,
                               CASE
                                   WHEN LOWER(p.documento) = ? THEN 0
                                   WHEN LOWER(p.documento) LIKE ? THEN 1
                                   WHEN LOWER(p.nombre) LIKE ? THEN 2
                                   ELSE 3
                               END AS prioridad
                          FROM cliente c
                          JOIN persona p ON p.id = c.id
                         WHERE LOWER(p.documento) LIKE ?
                            OR LOWER(p.nombre) LIKE ?
                         ORDER BY prioridad, p.nombre
                       )
                 WHERE ROWNUM <= ?
                """,
                statement -> {
                    statement.setString(1, exact);
                    statement.setString(2, prefix);
                    statement.setString(3, prefix);
                    statement.setString(4, contains);
                    statement.setString(5, contains);
                    statement.setInt(6, limit);
                },
                this::clientRow);
    }

    private Map<String, Object> clientRow(ResultSet resultSet) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", resultSet.getInt("id"));
        row.put("name", Api.text(resultSet, "nombre"));
        row.put("document", Api.text(resultSet, "documento"));
        row.put("phone", Api.text(resultSet, "telefono"));
        row.put("address", Api.text(resultSet, "direccion"));
        row.put("creditEnabled", resultSet.getInt("estadocredito") == 1);
        row.put("debt", resultSet.getDouble("deuda"));
        return row;
    }

    private Map<String, Object> findPersonByDocument(Connection connection, String document) throws Exception {
        return Api.one(
                connection,
                """
                SELECT id, nombre, telefono
                  FROM persona
                 WHERE documento = ?
                """,
                statement -> statement.setString(1, document),
                resultSet -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", resultSet.getInt("id"));
                    row.put("name", Api.text(resultSet, "nombre"));
                    row.put("phone", Api.text(resultSet, "telefono"));
                    return row;
                });
    }

    private int insertPersona(Connection connection, String name, String document, String phone) throws Exception {
        try (CallableStatement statement = connection.prepareCall(
                """
                BEGIN
                    INSERT INTO persona (id, nombre, documento, telefono)
                    VALUES (NULL, ?, ?, ?)
                    RETURNING id INTO ?;
                END;
                """)) {
            statement.setString(1, name);
            statement.setString(2, document);
            if (phone.isBlank()) {
                statement.setNull(3, Types.VARCHAR);
            } else {
                statement.setString(3, phone);
            }
            statement.registerOutParameter(4, Types.INTEGER);
            statement.execute();
            return statement.getInt(4);
        }
    }

    private boolean clientExists(Connection connection, int clientId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM cliente WHERE id = ?")) {
            statement.setInt(1, clientId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private boolean documentExistsForOtherClient(Connection connection, String document, int clientId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM persona WHERE documento = ? AND id <> ?")) {
            statement.setString(1, document);
            statement.setInt(2, clientId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private int boundedLimit(String raw) {
        int limit = 8;
        try {
            limit = Integer.parseInt(String.valueOf(raw).trim());
        } catch (Exception ignored) {
            // Usa el valor por defecto si el query param no es valido.
        }
        return Math.max(1, Math.min(limit, 25));
    }
}
