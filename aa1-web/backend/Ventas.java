import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Ventas implements HttpHandler {
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
            Api.error(exchange, 500, "Error en ventas: " + exception.getMessage());
        }
    }

    private void get(HttpExchange exchange) throws Exception {
        try (Connection connection = Conexion.getConnection()) {
            List<Map<String, Object>> products = Api.rows(
                    connection,
                    Productos.productSelectSql() + " ORDER BY vp.nombre",
                    null,
                    resultSet -> Productos.productRow(resultSet));

            List<Map<String, Object>> methods = Api.rows(
                    connection,
                    "SELECT id, nombre FROM metodo_pago ORDER BY id",
                    null,
                    resultSet -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("id", resultSet.getInt("id"));
                        row.put("name", Api.text(resultSet, "nombre"));
                        return row;
                    });

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("products", products);
            response.put("methods", methods);
            response.put("lastInvoiceId", Api.scalarInt(connection, "SELECT NVL(MAX(id_factura), 0) FROM vw_facturas_detalle"));
            Api.ok(exchange, response);
        }
    }

    private void post(HttpExchange exchange) throws Exception {
        Map<String, Object> body = Api.readJsonObject(exchange);
        int clientId = Api.integer(body, "clientId", 0);
        int employeeId = Api.integer(body, "employeeId", 0);
        String payment = Api.str(body, "payment");
        BigDecimal received = decimal(body.get("received"));
        List<Object> itemInputs = Api.array(body, "items");

        if (clientId <= 0 || payment.isBlank() || itemInputs.isEmpty()) {
            Api.error(exchange, 400, "Cliente, pago y productos son obligatorios");
            return;
        }

        try (Connection connection = Conexion.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int paymentId = paymentId(connection, payment);
                if (paymentId == 4 && !clientHasCredit(connection, clientId)) {
                    throw new IllegalArgumentException("Este cliente no tiene credito habilitado");
                }

                List<Map<String, Object>> items = loadSaleItems(connection, itemInputs);
                BigDecimal total = items.stream()
                        .map(item -> (BigDecimal) item.get("subtotal"))
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .setScale(2, RoundingMode.HALF_UP);
                if ("Efectivo".equalsIgnoreCase(payment) && received.compareTo(total) < 0) {
                    throw new IllegalArgumentException("El valor recibido no cubre el total");
                }

                int invoiceId = insertInvoice(connection, total, clientId, employeeId, paymentId);

                try (PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO detallefactura (iddetalle, idfactura, codigobarras, cantidadkg, subtotal)
                        VALUES (NULL, ?, ?, ?, ?)
                        """)) {
                    for (Map<String, Object> item : items) {
                        statement.setInt(1, invoiceId);
                        statement.setInt(2, ((Number) item.get("code")).intValue());
                        statement.setBigDecimal(3, (BigDecimal) item.get("quantity"));
                        statement.setBigDecimal(4, (BigDecimal) item.get("subtotal"));
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }

                connection.commit();
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("message", "Factura creada");
                response.put("invoiceId", invoiceId);
                response.put("total", total);
                response.put("change", received.subtract(total).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
                Api.ok(exchange, response);
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private int paymentId(Connection connection, String payment) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM metodo_pago WHERE LOWER(nombre) = LOWER(?)")) {
            statement.setString(1, payment);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("Metodo de pago no encontrado");
                }
                return resultSet.getInt("id");
            }
        }
    }

    private boolean clientHasCredit(Connection connection, int clientId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT estadocredito FROM cliente WHERE id = ?")) {
            statement.setInt(1, clientId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt("estadocredito") == 1;
            }
        }
    }

    private List<Map<String, Object>> loadSaleItems(Connection connection, List<Object> itemInputs) throws Exception {
        List<Map<String, Object>> items = new ArrayList<>();
        for (Object input : itemInputs) {
            if (!(input instanceof Map<?, ?> raw)) {
                continue;
            }
            int code = number(raw.get("code")).intValue();
            BigDecimal quantity = decimal(raw.get("quantity"));
            if (code <= 0 || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Producto o cantidad invalida");
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    """
                    SELECT p.codigobarras, vp.nombre, vp.precio, p.stock
                      FROM producto p
                      JOIN vista_productos vp ON vp.codigo = p.codigobarras
                     WHERE p.codigobarras = ?
                     FOR UPDATE OF p.stock
                    """)) {
                statement.setInt(1, code);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new IllegalArgumentException("Producto no encontrado: " + code);
                    }
                    BigDecimal stock = resultSet.getBigDecimal("stock");
                    if (quantity.compareTo(stock) > 0) {
                        throw new IllegalArgumentException("Stock insuficiente para " + Api.text(resultSet, "nombre"));
                    }
                    BigDecimal price = resultSet.getBigDecimal("precio");
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("code", code);
                    item.put("name", Api.text(resultSet, "nombre"));
                    item.put("price", price);
                    item.put("quantity", quantity);
                    item.put("subtotal", price.multiply(quantity).setScale(2, RoundingMode.HALF_UP));
                    items.add(item);
                }
            }
        }

        if (items.isEmpty()) {
            throw new IllegalArgumentException("No hay productos validos para facturar");
        }
        return items;
    }

    private int insertInvoice(Connection connection, BigDecimal total, int clientId, int employeeId, int paymentId)
            throws Exception {
        try (CallableStatement statement = connection.prepareCall(
                """
                BEGIN
                    INSERT INTO factura (idfactura, fecha, total, id_cliente, id_empleado, id_metodo_pago)
                    VALUES (NULL, TRUNC(SYSDATE), ?, ?, ?, ?)
                    RETURNING idfactura INTO ?;
                END;
                """)) {
            statement.setBigDecimal(1, total);
            statement.setInt(2, clientId);
            if (employeeId > 0) {
                statement.setInt(3, employeeId);
            } else {
                statement.setNull(3, Types.INTEGER);
            }
            statement.setInt(4, paymentId);
            statement.registerOutParameter(5, Types.INTEGER);
            statement.execute();
            return statement.getInt(5);
        }
    }

    private Number number(Object value) {
        if (value instanceof Number number) {
            return number;
        }
        return Double.parseDouble(String.valueOf(value));
    }

    private BigDecimal decimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.setScale(2, RoundingMode.HALF_UP);
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }
        return new BigDecimal(String.valueOf(value)).setScale(2, RoundingMode.HALF_UP);
    }
}
