package sync;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SyncResult {
    
    public static final String STATUS_OK = "OK";
    public static final String STATUS_PARTIAL = "PARCIAL";
    public static final String STATUS_ERROR = "ERROR";
    
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    
    public String type;
    public String startedAt;
    public String finishedAt;
    public String status;
    public int totalErrors;
    public List<TableResult> tables = new ArrayList<>();
    
    
    public SyncResult(String type){
        this.type = type;
        this.startedAt = LocalDateTime.now().format(FMT);
        this.status = STATUS_OK;
    }
    
    public void finish(){
        this.finishedAt = LocalDateTime.now().format(FMT);
        this.totalErrors = (int) tables.stream().filter(t-> STATUS_ERROR.equals(t.status)).count();
        
        if (totalErrors > 0)
            this.status = STATUS_PARTIAL;
    }
    
    // clase TableResult
    public static class TableResult {
    public String tableName;
    public int rows;
    public String status;
    public String errorDetail;
    
    public TableResult(String tableName){
        this.tableName = tableName;
        this.status = STATUS_OK;
    }
}
}

