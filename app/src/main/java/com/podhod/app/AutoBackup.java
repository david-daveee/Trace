package com.podhod.app;
import android.app.job.*;
import android.content.*;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
/** Checks daily; writes a new full backup only when fourteen days have elapsed. */
final class AutoBackup {
 static final int JOB=1401, FIRST=1402;
 static final long INTERVAL=14L*24*60*60*1000;
 static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("trace-auto-backup",0);}
 static boolean enabled(Context c){return !prefs(c).getString("tree","").isEmpty();}
 static void schedule(Context c){
  if(!enabled(c))return;JobScheduler s=c.getSystemService(JobScheduler.class);
  if(s.getPendingJob(JOB)==null&&s.schedule(new JobInfo.Builder(JOB,new ComponentName(c,AutoBackupService.class)).setPersisted(true).setPeriodic(24L*60*60*1000).build())!=JobScheduler.RESULT_SUCCESS)throw new IllegalStateException("Backup scheduling failed");
 }
 static void enableAutomatic(Context c){enable(c,Uri.parse("downloads"));}
 static void enable(Context c,Uri tree){
  prefs(c).edit().putString("tree",tree.toString()).putLong("last",0).putBoolean("error",false).commit();schedule(c);
  if(c.getSystemService(JobScheduler.class).schedule(new JobInfo.Builder(FIRST,new ComponentName(c,AutoBackupService.class)).setPersisted(true).setMinimumLatency(0).build())!=JobScheduler.RESULT_SUCCESS)throw new IllegalStateException("Backup scheduling failed");
 }
 static void disable(Context c){prefs(c).edit().remove("tree").commit();JobScheduler s=c.getSystemService(JobScheduler.class);s.cancel(JOB);s.cancel(FIRST);}
 static boolean due(long now,long last){return last==0||now-last>=INTERVAL;}
 static synchronized boolean run(Context c){
  if(!enabled(c)||!due(System.currentTimeMillis(),prefs(c).getLong("last",0)))return true;
  boolean automatic="downloads".equals(prefs(c).getString("tree",""));Uri file=null;try{
   String state=c.getSharedPreferences("podhod",0).getString("state",null);if(state==null)throw new IOException("No saved data");
   byte[] bytes=FullBackup.create(c,Engine.obj(state)).toString().getBytes(StandardCharsets.UTF_8);
   if(bytes.length>FullBackup.LIMIT)throw new IOException("Backup exceeds limit");
   String name="Trace-backup-"+new SimpleDateFormat("yyyy-MM-dd-HHmmss",Locale.US).format(new Date())+".json";
   if(automatic){
    if(android.os.Build.VERSION.SDK_INT<29)throw new IOException("Automatic folder requires Android 10");
    ContentValues values=new ContentValues();values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME,name);values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE,"application/json");values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH,android.os.Environment.DIRECTORY_DOWNLOADS+"/Trace/Backup");values.put(android.provider.MediaStore.MediaColumns.IS_PENDING,1);
    file=c.getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);
   }else{Uri tree=Uri.parse(prefs(c).getString("tree",""));Uri parent=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));file=DocumentsContract.createDocument(c.getContentResolver(),parent,"application/json",name);}
   if(file==null)throw new IOException("Cannot create backup");
   try(OutputStream out=c.getContentResolver().openOutputStream(file,"w")){if(out==null)throw new IOException("Cannot write backup");out.write(bytes);out.flush();}
   try(InputStream in=c.getContentResolver().openInputStream(file)){if(in==null||!Arrays.equals(bytes,FullBackup.read(in)))throw new IOException("Backup verification failed");}
   if(automatic&&android.os.Build.VERSION.SDK_INT>=29){ContentValues ready=new ContentValues();ready.put(android.provider.MediaStore.MediaColumns.IS_PENDING,0);if(c.getContentResolver().update(file,ready,null,null)!=1)throw new IOException("Cannot publish backup");}
   prefs(c).edit().putString("lastUri",file.toString()).commit();
   long now=System.currentTimeMillis();BackupHistory.record(c,file,now,bytes.length,name);prefs(c).edit().putLong("last",now).putBoolean("error",false).commit();c.getSharedPreferences("trace-settings",0).edit().putLong("lastBackupAt",now).commit();return true;
  }catch(Exception e){if(file!=null)try{if(automatic)c.getContentResolver().delete(file,null,null);else DocumentsContract.deleteDocument(c.getContentResolver(),file);}catch(Exception ignored){}prefs(c).edit().putBoolean("error",true).commit();return false;}
 }
 static String status(Context c){
  if(!enabled(c))return Lang.t("Выключен");
  if(prefs(c).getBoolean("error",false))return Lang.t("Не удалось сохранить копию. Проверь доступ к папке и свободное место.");
  long last=prefs(c).getLong("last",0);if(last==0)return Lang.t("Первая копия ожидает сохранения");
  return ("downloads".equals(prefs(c).getString("tree",""))?Lang.t("Загрузки → Trace → Backup")+"\n":"")+Lang.t("Автокопия: ")+new SimpleDateFormat("d MMM yyyy · HH:mm",Lang.locale()).format(new Date(last))+"\n"+Lang.t("Следующая — после ")+new SimpleDateFormat("d MMM yyyy",Lang.locale()).format(new Date(last+INTERVAL));
 }
}
