package com.podhod.app;

import android.content.*;
import android.net.Uri;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

/** Called on the main thread; sensor/walk writes are locked during capture and apply. */
final class SyncLocal {
    static JSONObject capture(Context c) { return SyncPayload.portable(FullBackup.create(c,Store.get(c).data)); }
    static boolean active(Context c) {
        JSONObject workouts=Store.get(c).data, saved=workouts.optJSONObject("savedSessions");
        return workouts.optJSONObject("active")!=null || (saved!=null&&saved.length()>0) || Walks.data(c).optJSONObject("active")!=null;
    }
    static void validate(JSONObject payload) {
        if(payload==null||payload.optJSONObject("walks")==null||payload.optJSONObject("steps")==null||payload.optJSONObject("settings")==null)throw new IllegalArgumentException("Incomplete account snapshot");
        JSONObject full=Engine.copy(payload);Engine.put(full.optJSONObject("steps"),"enabled",false);
        JSONObject workouts=FullBackup.workouts(full);
        JSONArray programs=workouts.optJSONArray("programs"),history=workouts.optJSONArray("history");
        if(programs==null||history==null||payload.optJSONObject("walks").has("active"))throw new IllegalArgumentException("Invalid account snapshot");
        Set<String> ids=new HashSet<>();
        for(int i=0;i<programs.length();i++){
            JSONObject p=programs.optJSONObject(i);Engine.validateProgram(p);
            if(p.optString("id").isEmpty()||!ids.add(p.optString("id"))||p.optInt("nextDay")<0||p.optInt("nextDay")>p.optJSONArray("days").length())throw new IllegalArgumentException("Invalid plan progress");
        }
        Set<String> sessionIds=new HashSet<>();
        for(int i=0;i<history.length();i++)validateSession(history.optJSONObject(i),null,sessionIds);
        if(workouts.has("active"))validateSession(workouts.optJSONObject("active"),ids,sessionIds);
        if(workouts.has("savedSessions")){
            JSONObject saved=workouts.optJSONObject("savedSessions");if(saved==null)throw new IllegalArgumentException("Invalid paused workouts");
            Iterator<String> keys=saved.keys();while(keys.hasNext()){
                String key=keys.next();JSONObject session=saved.optJSONObject(key);validateSession(session,ids,sessionIds);
                if(!key.equals(session.optString("programId")))throw new IllegalArgumentException("Invalid paused workout plan");
            }
        }
        JSONArray deleted=payload.optJSONObject("walks").optJSONArray("deletedIds");
        JSONArray walks=payload.optJSONObject("walks").optJSONArray("history");
        if(walks==null)throw new IllegalArgumentException("Missing walk history");
        for(int i=0;i<walks.length();i++){
            JSONObject walk=walks.optJSONObject(i);
            if(walk==null||walk.optLong("ended")<walk.optLong("started")||walk.optLong("ended")<=0)throw new IllegalArgumentException("Unfinished walk in account snapshot");
        }
        if(deleted!=null)for(int i=0;i<deleted.length();i++)if(!(deleted.opt(i) instanceof String)||deleted.optString(i).isEmpty())throw new IllegalArgumentException("Invalid walk deletion record");
    }
    static void validateSession(JSONObject session,Set<String> plans,Set<String> ids){
        JSONArray sets=session==null?null:session.optJSONArray("sets");
        if(sets==null||sets.length()==0||session.optString("id").isEmpty()||!ids.add(session.optString("id"))||session.optInt("cursor")<0||session.optInt("cursor")>sets.length())throw new IllegalArgumentException("Invalid workout session");
        if(plans!=null&&(!plans.contains(session.optString("programId"))||session.optInt("day",-1)<0||session.has("ended")))throw new IllegalArgumentException("Invalid unfinished workout plan");
        for(int j=0;j<sets.length();j++){
            JSONObject set=sets.optJSONObject(j);
            if(set==null||set.optString("exercise").isEmpty()||set.optInt("reps")<1||set.optInt("sets")<1||!Double.isFinite(set.optDouble("kg",-1))||set.optDouble("kg",-1)<-1)throw new IllegalArgumentException("Invalid workout set");
        }
    }
    static File backup(Context c,JSONObject full)throws IOException {
        byte[] bytes=full.toString().getBytes(StandardCharsets.UTF_8);
        if(bytes.length>FullBackup.LIMIT)throw new IOException("Backup exceeds 50 MB");
        File folder=new File(c.getFilesDir(),"account-backups");if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException("Cannot create backup folder");
        File target=new File(folder,"Trace-before-sync-"+System.currentTimeMillis()+"-"+UUID.randomUUID()+".json");
        try(FileOutputStream out=new FileOutputStream(target)){out.write(bytes);out.getFD().sync();}
        try(FileInputStream in=new FileInputStream(target)){if(!CloudSnapshot.hash(bytes).equals(CloudSnapshot.hash(FullBackup.read(in))))throw new IOException("Backup verification failed");}
        BackupHistory.record(c,Uri.fromFile(target),System.currentTimeMillis(),bytes.length,target.getName());
        // Bound only app-created sync safety copies; exported/manual backups are untouched.
        File[] copies=folder.listFiles((dir,name)->name.startsWith("Trace-before-sync-")&&name.endsWith(".json"));
        if(copies!=null){Arrays.sort(copies,Comparator.comparingLong(File::lastModified).reversed());for(int i=20;i<copies.length;i++)if(copies[i].delete())BackupHistory.forget(c,Uri.fromFile(copies[i]));}
        return target;
    }
    static void apply(Context c,JSONObject payload) {
        validate(payload);Store store=Store.get(c);
        synchronized(Steps.class){synchronized(Walks.class){
            JSONObject oldWorkouts=store.data;Map<String,?> oldSteps=Steps.prefs(c).getAll(),oldWalks=Walks.prefs(c).getAll(),oldSettings=c.getSharedPreferences("trace-settings",0).getAll();
            try {SyncRecovery.begin(c);}catch(Exception e){throw new IllegalStateException("Cannot create restore journal",e);}
            try {
                JSONObject stepData=Steps.data(c),incoming=payload.optJSONObject("steps");
                // Preserve counter, boot ID, sensor enablement and all permission-related state.
                Engine.put(stepData,"days",Engine.copy(incoming.optJSONObject("days")));
                if(!Steps.prefs(c).edit().putString("data",stepData.toString()).putInt("goal",incoming.optInt("goal")).putFloat("stepLengthCm",(float)incoming.optDouble("stepLengthCm")).commit())throw new IOException("Cannot save steps");
                JSONObject incomingWalks=Engine.copy(payload.optJSONObject("walks"));
                if(Walks.data(c).optJSONObject("active")!=null)Engine.put(incomingWalks,"active",Engine.copy(Walks.data(c).optJSONObject("active")));
                Walks.save(c,incomingWalks);
                if(!c.getSharedPreferences("trace-settings",0).edit().putString("language",payload.optJSONObject("settings").optString("language")).commit())throw new IOException("Cannot save settings");
                store.data=Engine.copy(payload.optJSONObject("workouts"));store.ensureCatalog();store.save();Lang.init(c);SyncRecovery.finish(c);
            }catch(Exception e){
                store.data=oldWorkouts;SyncRecovery.recover(c);store.save();Lang.init(c);throw new IllegalStateException("Could not apply account data; local data restored",e);
            }
        }}
    }
}
