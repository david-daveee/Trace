package com.podhod.app;

import android.content.*;
import android.util.AtomicFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

/** Durable undo journal: a killed multi-preference restore rolls back before Store is opened. */
final class SyncRecovery {
    private static final String[] NAMES={"podhod","trace-steps","trace-walks","trace-settings"};
    private static AtomicFile file(Context c){return new AtomicFile(new File(c.getFilesDir(),"sync-restore-journal.json"));}
    static void begin(Context c)throws Exception {
        JSONObject all=new JSONObject();
        for(String name:NAMES){
            JSONObject values=new JSONObject();
            for(var entry:c.getSharedPreferences(name,0).getAll().entrySet()){
                Object value=entry.getValue();JSONObject item=new JSONObject();
                item.put("type",value instanceof String?"string":value instanceof Boolean?"bool":value instanceof Integer?"int":value instanceof Long?"long":value instanceof Float?"float":"set");
                item.put("value",value instanceof Set?new JSONArray((Set<?>)value):value);values.put(entry.getKey(),item);
            }
            all.put(name,values);
        }
        AtomicFile journal=file(c);FileOutputStream out=null;
        try{out=journal.startWrite();out.write(all.toString().getBytes(StandardCharsets.UTF_8));journal.finishWrite(out);}
        catch(Exception e){if(out!=null)journal.failWrite(out);throw e;}
    }
    static void finish(Context c){file(c).delete();}
    static void recover(Context c){
        AtomicFile journal=file(c);if(!journal.getBaseFile().exists()&&!new File(journal.getBaseFile()+".bak").exists())return;
        try{
            JSONObject all;
            try(InputStream in=journal.openRead()){all=new JSONObject(new String(FullBackup.read(in),StandardCharsets.UTF_8));}
            for(String name:NAMES){
                JSONObject values=all.getJSONObject(name);SharedPreferences.Editor editor=c.getSharedPreferences(name,0).edit().clear();
                Iterator<String> keys=values.keys();while(keys.hasNext()){
                    String key=keys.next();JSONObject item=values.getJSONObject(key);
                    switch(item.getString("type")){
                        case "string":editor.putString(key,item.getString("value"));break;
                        case "bool":editor.putBoolean(key,item.getBoolean("value"));break;
                        case "int":editor.putInt(key,item.getInt("value"));break;
                        case "long":editor.putLong(key,item.getLong("value"));break;
                        case "float":editor.putFloat(key,(float)item.getDouble("value"));break;
                        case "set":Set<String> set=new HashSet<>();JSONArray a=item.getJSONArray("value");for(int i=0;i<a.length();i++)set.add(a.getString(i));editor.putStringSet(key,set);break;
                        default:throw new IOException("Invalid restore journal");
                    }
                }
                if(!editor.commit())throw new IOException("Cannot recover "+name);
            }
            if(!c.getSharedPreferences("trace-account",0).edit().remove("consentUid").putString("status","restored").commit())throw new IOException("Cannot suspend sync after recovery");
            journal.delete();
        }catch(Exception e){throw new IllegalStateException("Could not recover interrupted restore; safety copy retained",e);}
    }
}
