import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Facturas implements HttpHandler {
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
            int month = parse(Api.query(exchange, "month"), 0);
            int year = parse(Api.query(exchange, "year"), 0);
            int selectedId = parse(Api.query(exchange, "id"), 0);
            String search = Api.query(exchange, "search").trim().toLowerCase();

            List<Object> binds = new ArrayList<>();
            StringBuilder sql = new StringBuilder(
                    """
                    SELECT id_factura, fecha, empleado, cliente, num_productos, total, metodo_pago
                      FROM vw_facturas_detalle
                     WHERE 1 = 1
                    """);
            if (year > 0) {
                sql.append(" AND EXTRACT(YEAR FROM fecha) = ?");
                binds.add(year);
            }
            if (month > 0) {
                sql.append(" AND EXTRACT(MONTH FROM fecha) = ?");
                binds.add(month);
            }
            if (!search.isBlank()) {
                sql.append(" AND LOWER(TO_CHAR(id_factura) || ' ' || NVL(cliente, '') || ' ' || NVL(metodo_pago, '')) LIKE ?");
                binds.add("%" + search + "%");
            }
            sql.append(" ORDER BY fecha DESC, id_factura DESC");

            List<Map<String, Object>> invoices = Api.rows(
                    connection,
                    sql.toString(),
                    statement -> bind(statement, binds),
                    resultSet -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("id", resultSet.getInt("id_factura"));
                        row.put("date", Api.date(resultSet, "fecha"));
                        row.put("employee", Api.text(resultSet, "empleado"));
                        row.put("client", Api.text(resultSet, "cliente"));
                        row.put("itemsCount", resultSet.getInt("num_productos"));
                        row.put("total", resultSet.getDouble("total"));
                        row.put("payment", Api.text(resultSet, "metodo_pago"));
                        return row;
                    });

            if (selectedId <= 0 && !invoices.isEmpty()) {
                selectedId = ((Number) invoices.get(0).get("id")).intValue();
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("invoices", invoices);
            response.put("detail", selectedId > 0 ? detail(connection, selectedId) : null);
            Api.ok(exchange, response);
        } catch (Exception exception) {
            Api.error(exchange, 500, "Error en facturas: " + exception.getMessage());
        }
    }

    static Map<String, Object> detail(Connection connection, int invoiceId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                """
                SELECT f.idfactura, f.fecha, f.total,
                       pc.nombre AS cliente, pc.documento AS documento_cliente,
                       pe.nombre AS empleado, mp.nombre AS metodo_pago,
                       d.iddetalle, d.codigobarras, d.cantidadkg, d.subtotal,
                       p.nombre AS producto, p.precio
                  FROM factura f
                  LEFT JOIN persona pc ON pc.id = f.id_cliente
                  LEFT JOIN persona pe ON pe.id = f.id_empleado
                  LEFT JOIN metodo_pago mp ON mp.id = f.id_metodo_pago
                  LEFT JOIN detallefactura d ON d.idfactura = f.idfactura
                  LEFT JOIN producto p ON p.codigobarras = d.codigobarras
                 WHERE f.idfactura = ?
                 ORDER BY d.iddetalle
                """)) {
            statement.setInt(1, invoiceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                Map<String, Object> invoice = null;
                List<Map<String, Object>> items = new ArrayList<>();
                while (resultSet.next()) {
                    if (invoice == null) {
                        invoice = new LinkedHashMap<>();
                        invoice.put("id", resultSet.getInt("idfactura"));
                        invoice.put("date", Api.date(resultSet, "fecha"));
                        invoice.put("total", resultSet.getDouble("total"));
                        invoice.put("client", Api.text(resultSet, "cliente"));
                        invoice.put("document", Api.text(resultSet, "documento_cliente"));
                        invoice.put("employee", Api.text(resultSet, "empleado"));
                        invoice.put("payment", Api.text(resultSet, "metodo_pago"));
                        invoice.put("items", items);
                    }
                    int detailId = resultSet.getInt("iddetalle");
                    if (!resultSet.wasNull()) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("detailId", detailId);
                        item.put("code", resultSet.getInt("codigobarras"));
                        item.put("name", Api.text(resultSet, "producto"));
                        item.put("price", resultSet.getDouble("precio"));
                        item.put("quantity", resultSet.getDouble("cantidadkg"));
                        item.put("subtotal", resultSet.getDouble("subtotal"));
                        items.add(item);
                    }
                }
                return invoice;
            }
        }
    }

    private static void bind(PreparedStatement statement, List<Object> binds) throws java.sql.SQLException {
        for (int i = 0; i < binds.size(); i++) {
            Object value = binds.get(i);
            if (value instanceof Integer number) {
                statement.setInt(i + 1, number);
            } else {
                statement.setString(i + 1, String.valueOf(value));
            }
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
