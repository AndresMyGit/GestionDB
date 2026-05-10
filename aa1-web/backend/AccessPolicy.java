import com.sun.net.httpserver.HttpExchange;
import java.sql.Connection;

public interface AccessPolicy {
    boolean isAllowed(Connection connection, HttpExchange exchange) throws Exception;

    String deniedMessage();
}
