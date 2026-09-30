package com.podhod.app;
import android.content.*;
import android.content.pm.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.*;
import java.lang.ref.WeakReference;

/** Private APK cache; downloads never install silently or leave the approved repository. */
final class UpdateDownload {
 static volatile boolean busy,cancelled;static volatile int percent;static String error="";
 static final Handler main=new Handler(Looper.getMainLooper());
 static WeakReference<MainActivity> screen=new WeakReference<>(null);
 static WeakReference<LinearLayout> host=new WeakReference<>(null);
 static File apk(Context c){return new File(c.getCacheDir(),"Trace-update.apk");}
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("trace-update-download",0);}
 static String s(String en,String ru){return AccountPanel.s(en,ru);}
 static boolean ready(Context c){try{return apk(c).isFile()&&AppUpdates.compare(prefs(c).getString("version","0.0.0"),c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionName)>0;}catch(Exception e){return false;}}
 static void controls(MainActivity a,LinearLayout parent,boolean newer){
  LinearLayout box=a.column();parent.addView(box);screen=new WeakReference<>(a);host=new WeakReference<>(box);draw(a,box,newer);
 }
 static void draw(MainActivity a,LinearLayout box,boolean newer){
  box.removeAllViews();if(!newer&&!busy&&!ready(a)&&error.isEmpty())return;
  TextView action=a.button(box,busy?s("Downloading… ","Скачивание… ")+percent+"%":ready(a)?s("Install update","Установить обновление"):s("Update Trace","Обновить Trace"),true,()->{if(ready(a))install(a);else start(a);});action.setEnabled(!busy);action.setTag("update-action");
  ProgressBar progress=new ProgressBar(a,null,android.R.attr.progressBarStyleHorizontal);progress.setTag("update-progress");progress.setMax(100);progress.setProgress(percent);progress.setProgressTintList(android.content.res.ColorStateList.valueOf(a.GREEN));box.addView(progress,new LinearLayout.LayoutParams(-1,a.dp(6)));progress.setVisibility(busy?android.view.View.VISIBLE:android.view.View.GONE);
  if(busy)a.link(box,s("Cancel download","Отменить скачивание"),()->cancelled=true);
  if(!error.isEmpty()){a.space(box,8);box.addView(a.text(error,13,0xFFE7C46D));}
 }
 static void refresh(){main.post(()->{MainActivity a=screen.get();LinearLayout box=host.get();if(a!=null&&!a.isDestroyed()&&box!=null&&box.isAttachedToWindow()){if(busy&&box.findViewWithTag("update-progress")!=null&&box.findViewWithTag("update-progress").getVisibility()==android.view.View.VISIBLE){((ProgressBar)box.findViewWithTag("update-progress")).setProgress(percent);((TextView)box.findViewWithTag("update-action")).setText(s("Downloading… ","Скачивание… ")+percent+"%");}else{a.pageScroll.capture();draw(a,box,true);a.pageScroll.restore();}}});}
 static void start(MainActivity a){
  if(busy)return;busy=true;cancelled=false;percent=0;error="";refresh();Context c=a.getApplicationContext();
  new Thread(()->{File part=new File(c.getCacheDir(),"Trace-update.part");try{
   JSONObject release=AppUpdates.fetch();if(AppUpdates.compare(release.optString("version"),c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionName)<=0)throw new IOException(s("You already have the latest version.","У тебя уже последняя версия."));
   c.getSharedPreferences("trace-updates",0).edit().putString("release",release.toString()).putLong("checked",System.currentTimeMillis()).apply();
   download(release,part);verify(c,part,release,true);if(cancelled)throw new InterruptedIOException();
   if(!part.renameTo(apk(c)))throw new IOException("Cannot save update");prefs(c).edit().putString("version",release.optString("version")).putString("release",release.toString()).commit();
  }catch(InterruptedIOException e){if(!cancelled)error=s("Download timed out. Please retry.","Время ожидания истекло. Попробуй ещё раз.");}
  catch(Exception e){android.util.Log.w("TraceUpdate","Update download failed",e);error=s("Could not download or verify the update. Check your connection and retry.","Не удалось скачать или проверить обновление. Проверь интернет и повтори.");}
  finally{part.delete();busy=false;refresh();if(!cancelled&&error.isEmpty()&&ready(c))main.post(()->{MainActivity current=screen.get();if(current!=null&&!current.isDestroyed()&&current.hasWindowFocus()&&"settings".equals(current.page))install(current);});} },"trace-apk-download").start();
 }
 static boolean allowed(URL u){return "https".equals(u.getProtocol())&&(u.getPort()==-1||u.getPort()==443)&&(u.getHost().equals("github.com")||u.getHost().equals("release-assets.githubusercontent.com")||u.getHost().equals("objects.githubusercontent.com"));}
 static void download(JSONObject r,File dest)throws Exception{
  String tag=r.getString("version");AppUpdates.version(tag);String expected=AppUpdates.RELEASES+"/download/"+tag+"/Trace.apk";
  if(!expected.equals(r.optString("apk")))throw new IOException("Unexpected APK URL");long size=r.optLong("size");String hash=r.optString("digest");
  if(size<=0||size>150*1024*1024||!hash.matches("sha256:[a-fA-F0-9]{64}"))throw new IOException("Missing release integrity metadata");
  URL url=new URL(expected);HttpURLConnection conn=null;
  try{for(int hop=0;hop<6;hop++){if(!allowed(url))throw new IOException("Unexpected download host");conn=(HttpURLConnection)url.openConnection();conn.setInstanceFollowRedirects(false);conn.setConnectTimeout(15000);conn.setReadTimeout(20000);conn.setRequestProperty("User-Agent","Trace-Android");int status=conn.getResponseCode();if(status==200)break;if(status!=301&&status!=302&&status!=303&&status!=307&&status!=308)throw new IOException("HTTP "+status);String location=conn.getHeaderField("Location");if(location==null)throw new IOException("Missing redirect");url=new URL(url,location);conn.disconnect();conn=null;}
   if(conn==null||conn.getResponseCode()!=200)throw new IOException("Redirect limit");MessageDigest digest=MessageDigest.getInstance("SHA-256");long total=0;int last=-1;
   try(InputStream in=conn.getInputStream();FileOutputStream out=new FileOutputStream(dest)){byte[] buffer=new byte[65536];int n;while((n=in.read(buffer))!=-1){if(cancelled)throw new InterruptedIOException();total+=n;if(total>size)throw new IOException("APK size mismatch");out.write(buffer,0,n);digest.update(buffer,0,n);int next=(int)(100*total/size);if(next!=last){percent=next;last=next;refresh();}}out.getFD().sync();}
   StringBuilder actual=new StringBuilder();for(byte b:digest.digest())actual.append(String.format(Locale.ROOT,"%02x",b&255));if(total!=size||!hash.substring(7).equalsIgnoreCase(actual.toString()))throw new IOException("APK checksum mismatch");
  }finally{if(conn!=null)conn.disconnect();}
 }
 @SuppressWarnings("deprecation") static void verify(Context c,File file,JSONObject release,boolean newer)throws Exception{
  PackageManager pm=c.getPackageManager();PackageInfo candidate=pm.getPackageArchiveInfo(file.getPath(),PackageManager.GET_SIGNATURES),installed=pm.getPackageInfo(c.getPackageName(),PackageManager.GET_SIGNATURES);
  if(candidate==null||!c.getPackageName().equals(candidate.packageName)||candidate.signatures==null||!new HashSet<>(Arrays.asList(candidate.signatures)).equals(new HashSet<>(Arrays.asList(installed.signatures))))throw new SecurityException("Update signature mismatch");
  if(AppUpdates.compare(candidate.versionName,release.optString("version"))!=0||(newer&&(candidate.versionCode<=installed.versionCode||AppUpdates.compare(candidate.versionName,installed.versionName)<=0)))throw new SecurityException("Update version mismatch");
 }
 static void install(MainActivity a){
  if(busy)return;try{JSONObject release=Engine.obj(prefs(a).getString("release","{}"));verify(a,apk(a),release,true);
   if(!a.getPackageManager().canRequestPackageInstalls()){
    new android.app.AlertDialog.Builder(a).setTitle(s("Allow updates from Trace","Разреши обновления из Trace")).setMessage(s("Android needs permission to open the update installer. Allow this source, then return to Trace.","Android нужно разрешение на установку обновления из Trace. Разреши этот источник и вернись в приложение.")).setNegativeButton(s("Cancel","Отмена"),null).setPositiveButton(s("Open settings","Открыть настройки"),(d,w)->{prefs(a).edit().putBoolean("awaitPermission",true).apply();a.startActivityForResult(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+a.getPackageName())),74);}).show();return;
   }
   Uri uri=Uri.parse("content://"+a.getPackageName()+".updates/Trace.apk");Intent intent=new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);intent.setClipData(ClipData.newRawUri("Trace update",uri));a.startActivity(intent);
  }catch(Exception e){error=s("Could not open the update. Download it again or use GitHub.","Не удалось открыть обновление. Скачай его заново или используй GitHub.");prefs(a).edit().remove("version").apply();refresh();}
 }
 static void permissionReturned(MainActivity a){if(!prefs(a).getBoolean("awaitPermission",false))return;prefs(a).edit().remove("awaitPermission").apply();if(a.getPackageManager().canRequestPackageInstalls()&&ready(a))install(a);}
}
