package sync;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {
    // MASTER Postgresql con pagila
    private static final String MASTER_URL = "jdbc:postgresql://localhost:1722/pagila";
    private static final String MASTER_USER = "postgres";
    private static final String MASTER_PASS = "clave123";
            
    
    // SLAVE mariaDB
    private static final String SLAVE_URL = "jdbc:mariadb://localhost:3306/pagila_slave?characterEncoding=utf8mb4";
    private static final String SLAVE_USER = "root";
    private static final String SLAVE_PASS = "sofia123";
    
    public static Connection getMasterConnection() throws SQLException {
        return DriverManager.getConnection(MASTER_URL, MASTER_USER,MASTER_PASS);
    }
    
    public static Connection getSlaveConnection() throws SQLException {
        return DriverManager.getConnection(SLAVE_URL, SLAVE_USER,SLAVE_PASS);
    }
    
    public static void close(AutoCloseable... resources){
        for (AutoCloseable r: resources){
            if (r!=null){
                try { 
                    r.close();
                } catch (Exception ignored){}
            }
        }
    }
}

