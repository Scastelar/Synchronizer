package sync;

// descarga datos del master (pagila) y los carga en el slave (mariadb)

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Consumer;

public class SyncIn {
    
    private static final TableDef[] TABLES = {
        new TableDef("language", "language",new String[]{"language_id","name","last_update"}),
        new TableDef("country", "country",new String[]{"country_id","country","last_update"}),
        new TableDef("actor", "actor",new String[]{"actor_id","first_name","last_name","last_update"}),
        new TableDef("category", "category",new String[]{"category_id","name","last_update"}),
        new TableDef("city", "city",new String[]{"city_id","city","country_id","last_update"}),
        new TableDef("address", "address",new String[]{"address_id","address","address2","district","city_id","postal_code","phone","last_update"}),
        new TableDef("film", "film",new String[]{"film_id","title","description","release_year","language_id","original_language_id","rental_duration","rental_rate","length","replacement_cost","rating","last_update","special_features"}),
        new TableDef("film_actor", "film_actor",new String[]{"actor_id","film_id","last_update"}),
        new TableDef("film_category", "film_category",new String[]{"film_id","category_id","last_update"}),
        new TableDef("inventory", "inventory",new String[]{"inventory_id","film_id","store_id","last_update"}),
    };
            
    public static SyncResult run(Consumer<String> logger){
        SyncResult result = new SyncResult("SYNC-IN");
        logger.accept("Iniciando sync-in...");
        
        Connection master = null;
        Connection slave = null;
        try {
            master = DatabaseConfig.getMasterConnection();
            slave = DatabaseConfig.getSlaveConnection();
            slave.setAutoCommit(false);
            
            // sincronizar store y staff
            syncStoreAndStaff(master,slave,result,logger);
            
            //el resto de tablas in
            for (TableDef td: TABLES) {
                syncTable(td, master, slave, result, logger);
            }
            
        }catch (SQLException e){
            logger.accept("Error de conexion:" + e.getMessage());
            result.status = SyncResult.STATUS_ERROR;
        } finally {
            DatabaseConfig.close(master,slave);
        }
        
        
        result.finish();
        SyncHistory.save(result);
        logger.accept("sync-in finalizado! estado: " + result.status);
        return result;
    } 
            
    //metodos de sincronizacion
   private static void syncTable(TableDef td,
                                  Connection master, Connection slave,
                                  SyncResult result, Consumer<String> logger) {
        SyncResult.TableResult tr = new SyncResult.TableResult(td.masterTable);
        result.tables.add(tr);
 
        String selectSQL = buildSelect(td.masterTable, td.cols);
        String upsertSQL = buildUpsert(td.slaveTable,  td.cols);
 
        try (PreparedStatement sel = master.prepareStatement(selectSQL);
             ResultSet rs          = sel.executeQuery();
             PreparedStatement ups = slave.prepareStatement(upsertSQL)) {
 
            int count = 0;
            while (rs.next()) {
                for (int i = 0; i < td.cols.length; i++) {
                    Object val = rs.getObject(td.cols[i]);
                    if (val instanceof Array arr) {
                        Object[] elements = (Object[]) arr.getArray();
                        StringBuilder sb = new StringBuilder();
                        for (Object el : elements) {
                            if (sb.length() > 0) sb.append(",");
                            sb.append(el);
                        }
                        val = sb.toString();
                    }
                    ups.setObject(i + 1, val);
                }
                ups.addBatch();
                count++;
                if (count % 500 == 0) ups.executeBatch(); // batches de 500
            }
            ups.executeBatch();
            slave.commit();
 
            tr.rows = count;
 
        } catch (SQLException e) {
            safeRollback(slave);
            tr.status      = SyncResult.STATUS_ERROR;
            tr.errorDetail = e.getMessage();
        }
    }
 
    private static void syncStoreAndStaff(Connection master, Connection slave,
                                          SyncResult result, Consumer<String> logger) {
        SyncResult.TableResult trStore = new SyncResult.TableResult("store");
        SyncResult.TableResult trStaff = new SyncResult.TableResult("staff");
        result.tables.add(trStore);
        result.tables.add(trStaff);
 
        try (Statement stmtFk = slave.createStatement()) {
            stmtFk.execute("SET FOREIGN_KEY_CHECKS = 0");
            slave.commit();
 
            //store 
            String[] storeCols = {"store_id","manager_staff_id","address_id","last_update"};
            syncTableRaw("store", "store", storeCols, master, slave, trStore, logger);
 
            //staff
            String[] staffCols = {"staff_id","first_name","last_name","address_id",
                                  "email","store_id","active","username","password","last_update"};
            syncTableRaw("staff", "staff", staffCols, master, slave, trStaff, logger);
 
            stmtFk.execute("SET FOREIGN_KEY_CHECKS = 1");
            slave.commit();
 
        } catch (SQLException e) {
            safeRollback(slave);
            trStore.status = trStaff.status = SyncResult.STATUS_ERROR;
            trStore.errorDetail = trStaff.errorDetail = e.getMessage();
        }
    }
 
    private static void syncTableRaw(String masterTable, String slaveTable,
                                     String[] cols,
                                     Connection master, Connection slave,
                                     SyncResult.TableResult tr,
                                     Consumer<String> logger) throws SQLException {
        String sel = buildSelect(masterTable, cols);
        String ups = buildUpsert(slaveTable,  cols);
 
        try (PreparedStatement pSel = master.prepareStatement(sel);
             ResultSet rs           = pSel.executeQuery();
             PreparedStatement pUps = slave.prepareStatement(ups)) {
 
            int count = 0;
            while (rs.next()) {
                for (int i = 0; i < cols.length; i++)
                    pUps.setObject(i + 1, rs.getObject(cols[i]));
                pUps.addBatch();
                count++;
            }
            pUps.executeBatch();
            slave.commit();
            tr.rows = count;
        }
    }
            
    // builders de sql
    private static String buildSelect(String table, String[] cols){
        StringBuilder sb = new StringBuilder("SELECT ");
        for (int i=0; i < cols.length; i++){
            if (i>0) sb.append(", ");
            sb.append('"').append(cols[i]).append('"');
        }
            sb.append(" FROM \"").append(table).append('"');
            return sb.toString();
    }
            
    private static String buildUpsert(String table, String[] cols){
        StringBuilder sb = new StringBuilder("INSERT INTO `").append(table).append("` (");
        for (int i=0; i < cols.length; i++){
            if (i>0) sb.append(", ");
            sb.append('`').append(cols[i]).append('`');
        }
            sb.append(") VALUES (");
            sb.append("?, ".repeat(cols.length - 1)).append("?)");
            sb.append(" ON DUPLICATE KEY UPDATE ");
            for (int i = 0; i < cols.length; i++){
                if (i>0) sb.append(", ");
                sb.append('`').append(cols[i]).append("` = VALUES(`").append(cols[i]).append("`)");
            }
            return sb.toString();
    }
    
    private static void safeRollback(Connection c){
        try { if (c != null) c.rollback();} catch (SQLException ignored){}
    }
            
  private record TableDef(String masterTable, String slaveTable, String[] cols){}          
}
