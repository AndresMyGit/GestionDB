import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Credito implements HttpHandler {
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
            Api.error(exchange, 500, "Error en credito: " + exception.getMessage());
        }
    }

    private void get(HttpExchange exchange) throws Exception {
        try (Connection connection = Conexion.getConnection()) {
            connection.setAutoCommit(false);
            int overdueUpdated = Api.callActualizarEstadoVencido(connection);
            connection.commit();

            List<Map<String, Object>> credits = Api.rows(
                    connection,
                    """
                    SELECT vc.id_credito, vc.id_cliente, p.documento, vc.nombre_cliente,
                           vc.id_factura, vc.total_factura, vc.saldo,
                           vc.fecha_inicio, vc.fecha_fin, vc.estado_credito
                      FROM vista_creditos_resumida vc
                      JOIN persona p ON p.id = vc.id_cliente
                     ORDER BY vc.fecha_fin ASC, vc.id_credito DESC
                    """,
                    null,
                    resultSet -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("id", resultSet.getInt("id_credito"));
                        row.put("clientId", resultSet.getInt("id_cliente"));
                        row.put("document", Api.text(resultSet, "documento"));
                        row.put("client", Api.text(resultSet, "nombre_cliente"));
                        row.put("invoiceId", resultSet.getInt("id_factura"));
                        row.put("invoiceTotal", resultSet.getDouble("total_factura"));
                        row.put("balance", resultSet.getDouble("saldo"));
                        row.put("startDate", Api.date(resultSet, "fecha_inicio"));
                        row.put("endDate", Api.date(resultSet, "fecha_fin"));
                        row.put("state", Api.text(resultSet, "estado_credito"));
                        return row;
                    });

            List<Map<String, Object>> payments = Api.rows(
                    connection,
                    """
                    SELECT ac.credito_id,
                           ac.id AS id_abono_credito,
                           ac.fecha,
                           cr.cliente AS id_cliente,
                           cr.factura AS id_factura,
                           p.documento,
                           p.nombre AS nombre_cliente,
                           ac.monto
                      FROM abono_credito ac
                      JOIN credito cr ON cr.id = ac.credito_id
                      JOIN persona p ON p.id = cr.cliente
                     ORDER BY ac.fecha DESC, ac.id DESC
                     FETCH FIRST 100 ROWS ONLY
                    """,
                    null,
                    resultSet -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("creditId", resultSet.getInt("credito_id"));
                        row.put("paymentId", resultSet.getInt("id_abono_credito"));
                        row.put("date", Api.date(resultSet, "fecha"));
                        row.put("clientId", resultSet.getInt("id_cliente"));
                        row.put("invoiceId", resultSet.getInt("id_factura"));
                        row.put("document", Api.text(resultSet, "documento"));
                        row.put("client", Api.text(resultSet, "nombre_cliente"));
                        row.put("amount", resultSet.getDouble("monto"));
                        return row;
                    });

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("credits", credits);
            response.put("payments", payments);
            response.put("overdueUpdated", overdueUpdated);
            response.put("totalDebt", Api.scalarDouble(connection, "SELECT NVL(SUM(saldo), 0) FROM credito WHERE estado <> 2"));
            response.put("clientCount", Api.scalarInt(connection, "SELECT COUNT(DISTINCT cliente) FROM credito WHERE saldo > 0"));
            response.put("blockedCount", Api.scalarInt(connection, "SELECT COUNT(*) FROM cliente WHERE estadocredito = 0"));
            Api.ok(exchange, response);
        }
    }

    private void post(HttpExchange exchange) throws Exception {
        Map<String, Object> body = Api.readJsonObject(exchange);
        String action = Api.str(body, "action");

        try (Connection connection = Conexion.getConnection()) {
            connection.setAutoCommit(false);
            if ("refreshOverdue".equals(action)) {
                int updated = Api.callActualizarEstadoVencido(connection);
                connection.commit();
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("message", "Creditos vencidos actualizados");
                response.put("updated", updated);
                Api.ok(exchange, response);
                return;
            }

            int creditId = Api.integer(body, "creditId", 0);
            double amount = Api.decimal(body, "amount", 0);
            if (creditId <= 0 || amount <= 0) {
                Api.error(exchange, 400, "Credito o abono invalido");
                return;
            }

            try (CallableStatement statement = connection.prepareCall(
                    "{ call registrar_abono(?, ?, ?, ?) }")) {
                statement.setInt(1, creditId);
                statement.setDouble(2, amount);
                statement.registerOutParameter(3, Types.VARCHAR);
                statement.registerOutParameter(4, Types.NUMERIC);
                statement.execute();

                String message = statement.getString(3);
                double balance = statement.getDouble(4);
                if (message != null && message.startsWith("Error")) {
                    connection.rollback();
                    Api.error(exchange, 400, message);
                    return;
                }

                connection.commit();
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("message", message);
                response.put("newBalance", balance);
                Api.ok(exchange, response);
            }
        }
    }
}
