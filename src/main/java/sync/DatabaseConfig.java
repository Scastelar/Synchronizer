package sync;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {
    // MASTER Postgresql con pagila
    private static final String MASTER_URL = "";
    private static final String MASTER_USER = "postgres";
    private static final String MASTER_PASS = "admin123";
            
    
    // SLAVE mariaDB
    private static final String SLAVE_URL = "";
    private static final String SLAVE_USER = "root";
    private static final String SLAVE_PASS = "sofia123";
    
    public static Connection getMasterConnection() throws SQLException {
        return DriverManager.getConnection(MASTER_URL, MASTER_USER,MASTER_PASS);
    }
    
    public static Connection getSlaveConnection() throws SQLException {
        return DriverManager.getConnection(SLAVE_URL, SLAVE_USER,SLAVE_PASS);
    }
}
