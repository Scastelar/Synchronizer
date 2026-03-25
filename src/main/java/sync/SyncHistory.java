
package sync;

// guarda y carga el historial de sincronizaciones

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.ProcessBuilder.Redirect.Type;
import java.util.ArrayList;
import java.util.List;

public class SyncHistory {
    private static final String FILE = "sync_history.json";
    private static final int MAX_KEEP = 50;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // agrega un resultado al historial y lo persiste
    public static synchronized void save(SyncResult result){
        List<SyncResult> history = load();
        history.add(0,result);
        
        if (history.size()> MAX_KEEP)
            history = history.subList(0, MAX_KEEP);
        
        try(Writer w= new FileWriter(FILE)){
            GSON.toJson(history,w);
        } catch (IOException e){
            System.err.println("No se pudo guardar: "+e.getMessage());
        }
    }
    
    
    // carga el historial desde el disco
    public static List<SyncResult> load(){
        File f = new File(FILE);
        if(!f.exists()) return new ArrayList<>();
        try (Reader r= new FileReader(f)){
            java.lang.reflect.Type listType = new TypeToken<List<SyncResult>>(){}.getType();
            List<SyncResult> list = GSON.fromJson(r, listType);
            return list != null ? list: new ArrayList<>();
        }catch (IOException e){
            return new ArrayList<>();
        } 
    }
    
}
