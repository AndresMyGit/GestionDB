import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Cortes implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (Api.options(exchange)) {
            return;
        }
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            Api.error(exchange, 405, "Metodo no permitido");
            return;
        }

        try (Connection connection = Conexion.getConnection()) {
            LocalDate today = LocalDate.now();
            int month = parse(Api.query(exchange, "month"), today.getMonthValue());
            int year = parse(Api.query(exchange, "year"), today.getYear());

            Map<String, Object> summary = Api.one(
                    connection,
                    """
                    SELECT NVL(SUM(total), 0) AS revenue,
                           COUNT(*) AS tickets,
                           NVL(AVG(total), 0) AS average_ticket,
                           NVL(SUM(CASE WHEN id_metodo_pago = 4 THEN total ELSE 0 END), 0) AS credit_sales
                      FROM factura
                     WHERE EXTRACT(YEAR FROM fecha) = ?
                       AND EXTRACT(MONTH FROM fecha) = ?
                    """,
                    statement -> {
                        statement.setInt(1, year);
                        statement.setInt(2, month);
                    },
                    resultSet -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("revenue", resultSet.getDouble("revenue"));
                        row.put("tickets", resultSet.getInt("tickets"));
                        row.put("average", resultSet.getDouble("average_ticket"));
                        row.put("creditSales", resultSet.getDouble("credit_sales"));
                        return row;
                    });

            List<Map<String, Object>> payments = Api.rows(
                    connection,
                    """
                    SELECT mp.nombre, SUM(f.total) AS total
                      FROM factura f
                      JOIN metodo_pago mp ON mp.id = f.id_metodo_pago
                     WHERE EXTRACT(YEAR FROM f.fecha) = ?
                       AND EXTRACT(MONTH FROM f.fecha) = ?
                     GROUP BY mp.nombre
                     ORDER BY total DESC
                    """,
                    statement -> {
                        statement.setInt(1, year);
                        statement.setInt(2, month);
                    },
                    resultSet -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("payment", Api.text(resultSet, "nombre"));
                        row.put("total", resultSet.getDouble("total"));
                        return row;
                    });

            Map<String, Object> topInvoice = Api.one(
                    connection,
                    """
                    SELECT f.idfactura, f.total, p.nombre AS cliente
                      FROM factura f
                      LEFT JOIN persona p ON p.id = f.id_cliente
                     WHERE EXTRACT(YEAR FROM f.fecha) = ?
                       AND EXTRACT(MONTH FROM f.fecha) = ?
                     ORDER BY f.total DESC
                     FETCH FIRST 1 ROWS ONLY
                    """,
                    statement -> {
                        statement.setInt(1, year);
                        statement.setInt(2, month);
                    },
                    resultSet -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("id", resultSet.getInt("idfactura"));
                        row.put("total", resultSet.getDouble("total"));
                        row.put("client", Api.text(resultSet, "cliente"));
                        return row;
                    });

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("month", month);
            response.put("year", year);
            response.put("summary", summary);
            response.put("payments", payments);
            response.put("lowStockCount", Api.scalarInt(connection, "SELECT COUNT(*) FROM producto WHERE stock < 10"));
            response.put("topInvoice", topInvoice);
            Api.ok(exchange, response);
        } catch (Exception exception) {
            Api.error(exchange, 500, "Error en cortes: " + exception.getMessage());
        }
    }

    private static int parse(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
