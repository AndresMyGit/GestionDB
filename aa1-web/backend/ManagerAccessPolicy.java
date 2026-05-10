import com.sun.net.httpserver.HttpExchange;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public final class ManagerAccessPolicy implements AccessPolicy {
    public static final ManagerAccessPolicy INSTANCE = new ManagerAccessPolicy();

    private ManagerAccessPolicy() {
    }

    @Override
    public boolean isAllowed(Connection connection, HttpExchange exchange) throws Exception {
        int employeeId = Api.employeeId(exchange);
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

    @Override
    public String deniedMessage() {
        return "Solo el gerente puede realizar esta accion";
    }
}
