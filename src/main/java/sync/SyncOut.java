package sync;

// lee las tablas log del slave y aplica los cambios al master

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SyncOut {
    
    private static final OutTableDef[] OUT_TABLES = {
        new OutTableDef("customer","customer_log","customer_id",new String[]{"customer_id","store_id","first_name","last_name",
            "email", "address_id","activebool","create_date","last_update","active"}),
        new OutTableDef("rental","rental_log","rental_id", new String[]{"rental_id","rental_date","inventory_id","customer_id","return_date","staff_id","last_update"}),
        new OutTableDef("payment","payment_log","payment_id", new String[]{"payment_id","customer_id","staff_id","rental_id","amount","payment_date"})  
    };
            
    private static final List<String> META_COLS = 
            List.of("log_id","operation","operated_at","synced");
    
    public static SyncResult run(Consumer<String> logger){
        SyncResult result = new SyncResult("SYNC-OUT");
        logger.accept("iniciando sync-out...");
        
         Connection master = null;
        Connection slave  = null;
        try {
            master = DatabaseConfig.getMasterConnection();
            slave  = DatabaseConfig.getSlaveConnection();
            master.setAutoCommit(false);
            slave.setAutoCommit(false);
 
            for (OutTableDef def : OUT_TABLES) {
                processLogTable(def, master, slave, result, logger);
            }
 
        } catch (SQLException e) {
            logger.accept("error de conexion: " + e.getMessage());
            result.status = SyncResult.STATUS_ERROR;
        } 
        
        result.finish();
        SyncHistory.save(result);
        logger.accept(" sync out finalizado! estado: "+ result.status);
        return result;
    }
            
    //procesar una tabla log
    private static void processLogTable (OutTableDef def, Connection master, Connection slave, SyncResult result, Consumer<String> logger){
        SyncResult.TableResult tr = new SyncResult.TableResult(def.masterTable);
        result.tables.add(tr);
        
        List<Integer> successIds = new ArrayList<>();
        List<Integer> failedIds = new ArrayList<>();
        
        // leer pendientes del slave
         String selectLog = "SELECT * FROM `" + def.logTable + "` WHERE synced = 0 ORDER BY log_id ASC";
         
         try(PreparedStatement sel = slave.prepareStatement(selectLog);
                 ResultSet rs = sel.executeQuery()){
             
             while (rs.next()){
                 int logId = rs.getInt("log_id");
                 String op = rs.getString("operation");
                 
                  try {
                    switch (op) {
                        case "INSERT" -> applyInsert(def, rs, master);
                        case "UPDATE" -> applyUpdate(def, rs, master);
                        case "DELETE" -> applyDelete(def, rs, master);
                    }
                    master.commit();
                    successIds.add(logId);
                    tr.rows++;
 
                } catch (SQLException e) {
                    master.rollback();
                    failedIds.add(logId);
                    logger.accept(" error log_id=" + logId + " op=" + op + ": " + e.getMessage());
                }
             }
         }catch (SQLException e) {
            tr.status      = SyncResult.STATUS_ERROR;
            tr.errorDetail = e.getMessage();
            logger.accept(" Error leyendo log: " + e.getMessage());
            return;
        }
         
         if (!successIds.isEmpty()){
             try {
                 deleteFromLog(def.logTable, successIds, slave);
                 slave.commit();
             } catch (SQLException e){
                 logger.accept("No se pudo limpiar log: " +e.getMessage());
             }
         }
         
         if (!failedIds.isEmpty()){
             tr.status = SyncResult.STATUS_PARTIAL;
             tr.errorDetail = "IDs fallidos: " + failedIds;
         }
    }
            
    //aplicar insert al master
    private static void applyInsert(OutTableDef def, ResultSet rs, Connection master) throws SQLException {
        String[] dataCols = getDataCols(def.cols);
        String sql = buildPgUpsert(def.masterTable, dataCols, def.pkCol);
        
        try(PreparedStatement ps = master.prepareStatement(sql)){
            for (int i = 0; i < dataCols.length; i++)
                ps.setObject(i+1, rs.getObject(dataCols[i]));
                ps.executeUpdate();
        }
    }
    
    //aplicar update al master
    private static void applyUpdate(OutTableDef def, ResultSet rs, Connection master) throws SQLException {
        applyInsert(def,rs,master);
    }
    
    //aplicar delete al master
    public static void applyDelete(OutTableDef def, ResultSet rs, Connection master) throws SQLException {
        String sql = "DELETE FROM \"" + def.masterTable + "\" WHERE \"" + def.pkCol + "\" = ?";
         try (PreparedStatement ps = master.prepareStatement(sql)) {
            ps.setObject(1, rs.getObject(def.pkCol));
            ps.executeUpdate();
        }
    }
    
    //eliminar IDs ya procesados de la taba log
    private static void deleteFromLog(String logTable, List<Integer> ids, Connection slave) throws SQLException{
        String placeholders = "?,".repeat(ids.size());
        placeholders = placeholders.substring(0,placeholders.length()-1);
        
        String sql = "DELETE FROM `" + logTable + "` WHERE log_id IN (" + placeholders + ")";
        
         try (PreparedStatement ps = slave.prepareStatement(sql)) {
            for (int i = 0; i < ids.size(); i++)
                ps.setInt(i + 1, ids.get(i));
            ps.executeUpdate();
        }
    }
    
    private static String buildPgUpsert(String table, String[]cols, String pk){
        StringBuilder sb= new StringBuilder("INSERTO INTO \"").append(table).append("\" (");
        
        for (int i = 0; i< cols.length; i++){
            if(i>0) sb.append(", ");
            sb.append('"').append(cols[i]).append('"');
        }
        sb.append(") VALUES (");
        sb.append("?, ".repeat(cols.length - 1)).append("?)");
        sb.append(" ON CONFLICT (\"").append(pk).append("\") DO UPDATE SET ");
        
        boolean first = true;
        for (String col: cols){
            if (col.equals(pk)) continue;
            if (!first) sb.append(", ");
            sb.append('"').append(col).append("\" = EXCLUDED.\"").append(col).append('"');
            first = false;
        }
        return sb.toString();
    }
    
    //filtra columnas de meta datos del log
    private static String[] getDataCols(String[] all ){
        List<String> data = new ArrayList<>();
        for (String c: all)
            if (!META_COLS.contains(c)) data.add(c);
        return data.toArray(new String[0]);
    }
            
            
  private record OutTableDef(String masterTable, String logTable, String pkCol, String[] cols){}
}
