package com.podhod.app;
import android.content.*;
import android.net.Uri;
import android.widget.*;
import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.Date;

/** Public releases with verified in-app download and Android installation confirmation. */
final class AppUpdates {
 static final String API="https://api.github.com/repos/david-daveee/Trace/releases/latest";
 static final String RELEASES="https://github.com/david-daveee/Trace/releases";
 static boolean checking;
 static int compare(String a,String b){String[] x=version(a),y=version(b);for(int i=0;i<3;i++){int c=Integer.compare(Integer.parseInt(x[i]),Integer.parseInt(y[i]));if(c!=0)return c;}return 0;}
 static String[] version(String s){String v=s.startsWith("v")?s.substring(1):s;if(!v.matches("[0-9]{1,6}\\.[0-9]{1,6}\\.[0-9]{1,6}"))throw new IllegalArgumentException("Invalid release version");return v.split("\\.");}
 static JSONObject parse(String raw){JSONObject r=Engine.obj(raw);version(r.optString("tag_name"));if(r.optBoolean("draft")||r.optBoolean("prerelease"))throw new IllegalArgumentException("Not a stable release");String tag=r.optString("tag_name"),prefix=RELEASES+"/download/"+tag+"/";JSONArray assets=r.optJSONArray("assets");String apk="",digest="";long size=0;if(assets!=null)for(int i=0;i<assets.length();i++){JSONObject asset=assets.optJSONObject(i);String url=asset.optString("browser_download_url");if("Trace.apk".equals(asset.optString("name"))&&url.equals(prefix+"Trace.apk")&&asset.optLong("size")>0){apk=url;size=asset.optLong("size");digest=asset.optString("digest");}}if(apk.isEmpty())throw new IllegalArgumentException("Release APK unavailable");JSONObject result=new JSONObject();Engine.put(result,"version",tag);Engine.put(result,"apk",apk);Engine.put(result,"size",size);Engine.put(result,"digest",digest);String notes=r.optString("body");int end=notes.indexOf("## Download");if(end>=0)notes=notes.substring(0,end);Engine.put(result,"notes",notes.trim());return result;}
 static JSONObject fetch()throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(API).openConnection();try{c.setConnectTimeout(12000);c.setReadTimeout(12000);c.setRequestProperty("Accept","application/vnd.github+json");c.setRequestProperty("User-Agent","Trace-Android");if(c.getResponseCode()!=200)throw new IOException("HTTP "+c.getResponseCode());try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>1024*1024)throw new IOException("Response too large");out.write(b,0,n);}return parse(out.toString(StandardCharsets.UTF_8.name()));}}finally{c.disconnect();}}
 static String installed(MainActivity a){try{return a.getPackageManager().getPackageInfo(a.getPackageName(),0).versionName;}catch(Exception e){return "0.0.0";}}
 static String plain(String value){return value.replaceAll("(?m)^#{1,6}\\s*", "").replace("**","").replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)","$1").trim();}
 static void open(MainActivity a,String url){try{a.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(ActivityNotFoundException e){a.error(new IllegalArgumentException(Lang.t("Не найден браузер для скачивания")));}}
 static void show(MainActivity a){LinearLayout card=a.card(a.body);draw(a,card);}
 static void draw(MainActivity a,LinearLayout card){a.pageScroll.capture();card.removeAllViews();a.label(card,Lang.t("ОБНОВЛЕНИЯ TRACE"));card.addView(a.title(Lang.t("Установлена версия ")+installed(a),20));SharedPreferences prefs=a.getSharedPreferences("trace-updates",0);String cached=prefs.getString("release","");JSONObject release=null;try{if(!cached.isEmpty()){release=Engine.obj(cached);version(release.optString("version"));}}catch(Exception ignored){}a.space(card,8);
  if(release!=null){boolean newer=compare(release.optString("version"),installed(a))>0;card.addView(a.text(newer?Lang.t("Доступна версия ")+release.optString("version"):Lang.t("У тебя актуальная версия"),16,newer?a.GREEN:a.MUTED));long checked=prefs.getLong("checked",0);card.addView(a.text(Lang.t("Проверено: ")+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT,Lang.locale()).format(new Date(checked)),12,a.MUTED));String notes=plain(release.optString("notes"));if(!notes.isEmpty())a.hint(card,Lang.t("Что нового"),notes);if(newer){card.addView(a.text(AccountPanel.s("Download here, then confirm the Android update. Your data stays in place.","Скачай здесь и подтверди обновление Android. Твои данные сохранятся."),13,a.MUTED));}}
  else card.addView(a.text(Lang.t("Проверь, появилась ли новая версия на GitHub."),14,a.MUTED));
  UpdateDownload.controls(a,card,release!=null&&compare(release.optString("version"),installed(a))>0);
  if(prefs.getBoolean("error",false))card.addView(a.text(Lang.t("Не удалось проверить обновление. Проверь интернет и попробуй ещё раз."),14,a.MUTED));
  TextView check=a.button(card,checking?Lang.t("Проверяем обновления…"):Lang.t("Проверить обновления"),false,()->check(a,card));check.setEnabled(!checking&&!UpdateDownload.busy);a.link(card,Lang.t("Все версии на GitHub"),()->open(a,RELEASES));a.pageScroll.restore();
 }
 static void check(MainActivity a,LinearLayout card){if(checking)return;checking=true;draw(a,card);Context context=a.getApplicationContext();new Thread(()->{try{JSONObject release=fetch();context.getSharedPreferences("trace-updates",0).edit().putString("release",release.toString()).putLong("checked",System.currentTimeMillis()).putBoolean("error",false).commit();}catch(Exception e){context.getSharedPreferences("trace-updates",0).edit().putBoolean("error",true).commit();}finally{a.runOnUiThread(()->{checking=false;context.sendBroadcast(new Intent("com.podhod.app.CHANGED").setPackage(context.getPackageName()),"com.podhod.app.INTERNAL");});}},"trace-updates").start();}
}
