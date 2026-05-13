import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Resumen implements HttpHandler {
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
            double salesToday = Api.scalarDouble(
                    connection,
                    "SELECT NVL(SUM(total), 0) FROM vw_facturas_detalle WHERE TRUNC(fecha) = TRUNC(SYSDATE)");
            double totalDebt = Api.scalarDouble(
                    connection,
                    "SELECT NVL(SUM(saldo), 0) FROM vista_creditos_resumida WHERE UPPER(estado_credito) <> 'PAGADO'");
            int lowStockCount = Api.scalarInt(
                    connection,
                    "SELECT COUNT(*) FROM producto p JOIN vista_productos vp ON vp.codigo = p.codigobarras WHERE p.stock < 10");
            int invoiceCount = Api.scalarInt(
                    connection,
                    "SELECT COUNT(*) FROM vw_facturas_detalle");

            List<Map<String, Object>> lowStock = Api.rows(
                    connection,
                    """
                    SELECT p.codigobarras, vp.nombre, vp.categoria, p.stock
                      FROM producto p
                      JOIN vista_productos vp ON vp.codigo = p.codigobarras
                     WHERE p.stock < 10
                     ORDER BY p.stock ASC, vp.nombre
                     FETCH FIRST 8 ROWS ONLY
                    """,
                    null,
                    resultSet -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("code", resultSet.getInt("codigobarras"));
                        row.put("name", Api.text(resultSet, "nombre"));
                        row.put("category", Api.text(resultSet, "categoria"));
                        row.put("stock", resultSet.getDouble("stock"));
                        return row;
                    });

            List<Map<String, Object>> invoices = Api.rows(
                    connection,
                    """
                    SELECT id_factura, fecha, empleado, cliente, num_productos, total, metodo_pago
                      FROM vw_facturas_detalle
                     ORDER BY fecha DESC, id_factura DESC
                     FETCH FIRST 5 ROWS ONLY
                    """,
                    null,
                    resultSet -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("id", resultSet.getInt("id_factura"));
                        row.put("date", Api.date(resultSet, "fecha"));
                        row.put("employee", Api.text(resultSet, "empleado"));
                        row.put("client", Api.text(resultSet, "cliente"));
                        row.put("items", resultSet.getInt("num_productos"));
                        row.put("total", resultSet.getDouble("total"));
                        row.put("payment", Api.text(resultSet, "metodo_pago"));
                        return row;
                    });

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("salesToday", salesToday);
            response.put("totalDebt", totalDebt);
            response.put("lowStockCount", lowStockCount);
            response.put("invoiceCount", invoiceCount);
            response.put("lowStock", lowStock);
            response.put("recentInvoices", invoices);
            Api.ok(exchange, response);
        } catch (Exception exception) {
            Api.error(exchange, 500, "Error al cargar resumen: " + exception.getMessage());
        }
    }
}
