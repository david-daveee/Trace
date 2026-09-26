package com.podhod.app;
import android.content.*;
import android.net.Uri;
import android.database.Cursor;
import android.provider.*;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.text.SimpleDateFormat;
final class BackupHistory {
 static synchronized JSONObject index(Context c){return Engine.obj(c.getSharedPreferences("trace-backup-history",0).getString("index","{}"));}
 static synchronized void record(Context c,Uri uri,long date,long size,String name){JSONObject all=index(c),entry=new JSONObject();Engine.put(entry,"uri",uri.toString());Engine.put(entry,"date",date);Engine.put(entry,"size",size);Engine.put(entry,"name",name);Engine.put(all,uri.toString(),entry);c.getSharedPreferences("trace-backup-history",0).edit().putString("index",all.toString()).commit();}
 static void remember(Context c,Uri uri,long date,long size){String name="Trace backup";try(Cursor cursor=c.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(cursor!=null&&cursor.moveToFirst())name=cursor.getString(0);}catch(Exception ignored){}record(c,uri,date,size,name);}
 static ArrayList<JSONObject> load(Context c){
  if(android.os.Build.VERSION.SDK_INT>=29)try(Cursor cursor=c.getContentResolver().query(MediaStore.Downloads.EXTERNAL_CONTENT_URI,new String[]{MediaStore.MediaColumns._ID,MediaStore.MediaColumns.DISPLAY_NAME,MediaStore.MediaColumns.DATE_ADDED,MediaStore.MediaColumns.SIZE},MediaStore.MediaColumns.RELATIVE_PATH+"=? AND "+MediaStore.MediaColumns.IS_PENDING+"=0",new String[]{"Download/Trace/Backup/"},null)){if(cursor!=null)while(cursor.moveToNext()){String name=cursor.getString(1);if(name.startsWith("Trace-backup-")&&name.endsWith(".json"))record(c,ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI,cursor.getLong(0)),cursor.getLong(2)*1000,cursor.getLong(3),name);}}catch(Exception ignored){}
  String treeString=AutoBackup.prefs(c).getString("tree","");if(treeString.startsWith("content:"))try{Uri tree=Uri.parse(treeString);Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));try(Cursor cursor=c.getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_LAST_MODIFIED,DocumentsContract.Document.COLUMN_SIZE},null,null,null)){if(cursor!=null)while(cursor.moveToNext()){String name=cursor.getString(1);if(name.startsWith("Trace-backup-")&&name.endsWith(".json"))record(c,DocumentsContract.buildDocumentUriUsingTree(tree,cursor.getString(0)),cursor.getLong(2),cursor.getLong(3),name);}}}catch(Exception ignored){}
  JSONObject all=index(c);ArrayList<JSONObject> result=new ArrayList<>();Iterator<String> keys=all.keys();while(keys.hasNext()){JSONObject item=all.optJSONObject(keys.next());if(item!=null)result.add(item);}result.sort((x,y)->Long.compare(y.optLong("date"),x.optLong("date")));return result;
 }
 static void show(MainActivity a){LinearLayout box=a.form();TextView loading=a.text(Lang.t("Загрузка копий…"),14,a.MUTED);box.addView(loading);android.app.AlertDialog dialog=a.panel(Lang.t("История резервных копий"),box,Lang.t("Закрыть"),()->{},false);
  new Thread(()->{ArrayList<JSONObject> entries=load(a);a.runOnUiThread(()->{if(a.isDestroyed()||!dialog.isShowing())return;box.removeAllViews();box.addView(a.text(Lang.t("Копии на этом устройстве. Для файла из другой папки используй «Восстановить из файла»."),13,a.MUTED));if(entries.isEmpty())box.addView(a.text(Lang.t("Сохранённых копий пока нет"),18,a.TEXT));for(JSONObject entry:entries){LinearLayout card=a.card(box);String date=new SimpleDateFormat("d MMM yyyy · HH:mm",Lang.locale()).format(new Date(entry.optLong("date")));card.addView(a.title(date,18));a.space(card,6);card.addView(a.text(android.text.format.Formatter.formatFileSize(a,entry.optLong("size")),14,a.GREEN));card.addView(a.text(entry.optString("name"),12,a.MUTED));a.button(card,Lang.t("Восстановить"),false,()->{Uri uri=Uri.parse(entry.optString("uri"));a.importFile(uri,true);});}});},"trace-backup-history").start();
 }
}
