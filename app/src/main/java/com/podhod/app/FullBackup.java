package com.podhod.app;
import android.content.*;
import org.json.*;
import java.io.*;
final class FullBackup {
 static final int LIMIT=50*1024*1024;
 static JSONObject create(Context c,JSONObject workouts){
  JSONObject result=new JSONObject(),steps=new JSONObject(),settings=new JSONObject();Engine.put(result,"format","trace-backup");Engine.put(result,"version",2);Engine.put(result,"createdAt",System.currentTimeMillis());Engine.put(result,"workouts",Engine.copy(workouts));
  synchronized(Steps.class){JSONObject days=Steps.data(c).optJSONObject("days");Engine.put(steps,"days",days==null?new JSONObject():Engine.copy(days));Engine.put(steps,"goal",Steps.goal(c));Engine.put(steps,"stepLengthCm",Steps.stepLength(c));Engine.put(steps,"enabled",Steps.enabled(c));}
  Engine.put(settings,"language",Lang.english()?"en":"ru");Engine.put(settings,"lastBackupAt",c.getSharedPreferences("trace-settings",0).getLong("lastBackupAt",0));Engine.put(result,"steps",steps);Engine.put(result,"settings",settings);return result;
 }
 static JSONObject workouts(JSONObject backup){
  if(!backup.has("format"))return backup;
  try{
   if(!"trace-backup".equals(backup.getString("format"))||backup.getInt("version")!=2)throw new IllegalArgumentException();
   JSONObject steps=backup.getJSONObject("steps"),settings=backup.getJSONObject("settings"),days=steps.getJSONObject("days");int goal=steps.getInt("goal");double length=steps.getDouble("stepLengthCm");steps.getBoolean("enabled");String language=settings.getString("language");
   if(goal<100||goal>100000||!Double.isFinite(length)||length<20||length>200||!(language.equals("ru")||language.equals("en")))throw new IllegalArgumentException();
   java.util.Iterator<String> it=days.keys();while(it.hasNext()){String day=it.next();java.time.LocalDate.parse(day);Object value=days.get(day);if(!(value instanceof Number)||((Number)value).doubleValue()<0||((Number)value).doubleValue()!=((Number)value).longValue())throw new IllegalArgumentException();}
   return backup.getJSONObject("workouts");
  }catch(Exception e){throw new IllegalArgumentException(Lang.t("Повреждена полная резервная копия"));}
 }
 static void apply(Context c,Store store,JSONObject backup,JSONObject workouts){
  JSONObject previous=store.data;SharedPreferences steps=Steps.prefs(c),settings=c.getSharedPreferences("trace-settings",0);java.util.Map<String,?> oldSteps=steps.getAll(),oldSettings=settings.getAll();
  synchronized(Steps.class){try{
   store.data=workouts;Zlat.ensure(store.data);store.ensureCatalog();store.save();
   if(backup.has("format"))restoreSettings(c,backup);
  }catch(Exception e){store.data=previous;store.save();restorePrefs(steps,oldSteps);restorePrefs(settings,oldSettings);Lang.init(c);throw new IllegalArgumentException(Lang.t("Не удалось восстановить копию"),e);}}
  if(!Steps.enabled(c))c.stopService(new Intent(c,StepsService.class));
 }
 static void restoreSettings(Context c,JSONObject backup){
  workouts(backup);JSONObject s=backup.optJSONObject("steps"),data=new JSONObject();Engine.put(data,"days",Engine.copy(s.optJSONObject("days")));
  if(!Steps.prefs(c).edit().putString("data",data.toString()).putInt("goal",s.optInt("goal")).putFloat("stepLengthCm",(float)s.optDouble("stepLengthCm")).putBoolean("enabled",s.optBoolean("enabled")).commit())throw new IllegalStateException();
  JSONObject config=backup.optJSONObject("settings");
  if(!c.getSharedPreferences("trace-settings",0).edit().putString("language",config.optString("language")).putLong("lastBackupAt",backup.optLong("createdAt",config.optLong("lastBackupAt"))).commit())throw new IllegalStateException();Lang.init(c);
 }
 static void restorePrefs(SharedPreferences prefs,java.util.Map<String,?> values){SharedPreferences.Editor e=prefs.edit().clear();for(String k:values.keySet()){Object v=values.get(k);if(v instanceof String)e.putString(k,(String)v);else if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);else if(v instanceof Integer)e.putInt(k,(Integer)v);else if(v instanceof Long)e.putLong(k,(Long)v);else if(v instanceof Float)e.putFloat(k,(Float)v);}e.commit();}
 static byte[] read(InputStream in)throws IOException{if(in==null)throw new IOException();ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>LIMIT)throw new IOException(Lang.t("Копия больше 50 МБ"));out.write(b,0,n);}return out.toByteArray();}
}
