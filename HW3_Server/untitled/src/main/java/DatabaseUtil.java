import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseUtil {
    private static final HikariDataSource ds;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://database-3.cdydja2nksqm.us-west-2.rds.amazonaws.com:3306/albumstore?useSSL=false");
        config.setUsername("admin");
        config.setPassword("goodluck123!");
        config.setMaximumPoolSize(50); // Set desired pool size
        ds = new HikariDataSource(config);
    }
    private DatabaseUtil() {
    }
    public static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }
}
