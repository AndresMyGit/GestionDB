import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Api {
    private Api() {
    }

    @FunctionalInterface
    public interface SqlBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    @FunctionalInterface
    public interface RowMapper {
        Map<String, Object> map(ResultSet resultSet) throws SQLException;
    }

    public static boolean options(HttpExchange exchange) throws IOException {
        cors(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return true;
        }
        return false;
    }

    public static void cors(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, X-Employee-Id");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
    }

    public static Map<String, Object> readJsonObject(HttpExchange exchange) throws IOException {
        String body;
        try (InputStream input = exchange.getRequestBody()) {
            body = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        if (body.isBlank()) {
            return new LinkedHashMap<>();
        }
        Object parsed = Json.parse(body);
        if (parsed instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return result;
        }
        return new LinkedHashMap<>();
    }

    public static void json(HttpExchange exchange, int status, Object value) throws IOException {
        cors(exchange);
        byte[] bytes = Json.stringify(value).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    public static void ok(HttpExchange exchange, Map<String, Object> data) throws IOException {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ok", true);
        response.putAll(data);
        json(exchange, 200, response);
    }

    public static void error(HttpExchange exchange, int status, String message) throws IOException {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ok", false);
        response.put("message", message);
        json(exchange, status, response);
    }

    public static int employeeId(HttpExchange exchange) {
        String value = exchange.getRequestHeaders().getFirst("X-Employee-Id");
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    public static boolean isManager(Connection connection, HttpExchange exchange) throws SQLException {
        int employeeId = employeeId(exchange);
        if (employeeId <= 0) {
            return false;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM empleado WHERE id = ? AND id_cargo = 1 AND NVL(estado, 1) = 1")) {
            statement.setInt(1, employeeId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    public static boolean requireManager(HttpExchange exchange, Connection connection) throws IOException, SQLException {
        if (isManager(connection, exchange)) {
            return true;
        }
        error(exchange, 403, "Solo el gerente puede realizar esta accion");
        return false;
    }

    public static String query(HttpExchange exchange, String name) {
        String raw = exchange.getRequestURI().getRawQuery();
        if (raw == null || raw.isBlank()) {
            return "";
        }
        for (String pair : raw.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = decode(parts[0]);
            if (key.equals(name)) {
                return parts.length > 1 ? decode(parts[1]) : "";
            }
        }
        return "";
    }

    public static String str(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    public static int integer(Map<String, Object> data, String key, int fallback) {
        Object value = data.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public static double decimal(Map<String, Object> data, String key, double fallback) {
        Object value = data.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Object> array(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof List<?> list) {
            return (List<Object>) list;
        }
        return List.of();
    }

    public static List<Map<String, Object>> rows(Connection connection, String sql, SqlBinder binder, RowMapper mapper)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (binder != null) {
                binder.bind(statement);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                while (resultSet.next()) {
                    rows.add(mapper.map(resultSet));
                }
                return rows;
            }
        }
    }

    public static Map<String, Object> one(Connection connection, String sql, SqlBinder binder, RowMapper mapper)
            throws SQLException {
        List<Map<String, Object>> rows = rows(connection, sql, binder, mapper);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public static int scalarInt(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }

    public static double scalarDouble(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            return resultSet.next() ? resultSet.getDouble(1) : 0;
        }
    }

    public static int callActualizarEstadoVencido(Connection connection) throws SQLException {
        SQLException lastError = null;
        String[] functionNames = {"actualizar_estado_vencido", "ADMIN.actualizar_estado_vencido"};
        for (String functionName : functionNames) {
            try (CallableStatement statement = connection.prepareCall("{ ? = call " + functionName + "() }")) {
                statement.registerOutParameter(1, Types.NUMERIC);
                statement.execute();
                return statement.getInt(1);
            } catch (SQLException error) {
                lastError = error;
                if (!looksLikeMissingRoutine(error)) {
                    throw error;
                }
            }
        }
        throw lastError == null ? new SQLException("No se pudo ejecutar actualizar_estado_vencido") : lastError;
    }

    public static String text(ResultSet resultSet, String column) throws SQLException {
        String value = resultSet.getString(column);
        return value == null ? "" : value;
    }

    public static String date(ResultSet resultSet, String column) throws SQLException {
        java.sql.Date value = resultSet.getDate(column);
        return value == null ? "" : value.toString();
    }

    public static String timestamp(ResultSet resultSet, String column) throws SQLException {
        java.sql.Timestamp value = resultSet.getTimestamp(column);
        return value == null ? "" : value.toLocalDateTime().toString().replace('T', ' ');
    }

    public static BigDecimal money(ResultSet resultSet, String column) throws SQLException {
        BigDecimal value = resultSet.getBigDecimal(column);
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String decode(String value) {
        return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static boolean looksLikeMissingRoutine(SQLException error) {
        String message = error.getMessage();
        if (message == null) {
            return false;
        }
        String normalized = message.toUpperCase();
        return normalized.contains("PLS-00201")
                || normalized.contains("ORA-06550")
                || normalized.contains("ORA-00904");
    }
}
