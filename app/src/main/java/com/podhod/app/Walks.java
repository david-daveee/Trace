package com.podhod.app;
import android.content.*;import org.json.*;
final class Walks {
 static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("trace-walks",0);}
 static synchronized JSONObject data(Context c){return Engine.obj(prefs(c).getString("data","{\"history\":[]}"));}
 static synchronized void save(Context c,JSONObject d){if(!prefs(c).edit().putString("data",d.toString()).commit())throw new IllegalStateException(Lang.t("Не удалось сохранить тренировку"));}
 static synchronized void begin(Context c){JSONObject d=data(c);if(d.optJSONObject("active")==null){Engine.put(d,"active",WalkData.start(System.currentTimeMillis()));save(c,d);}}
 static synchronized void point(Context c,android.location.Location l){JSONObject d=data(c),w=d.optJSONObject("active");if(w!=null&&WalkData.add(w,l.getLatitude(),l.getLongitude(),l.getTime(),l.getAccuracy()))save(c,d);}
 static synchronized void finish(Context c,boolean interrupted){JSONObject d=data(c),w=d.optJSONObject("active");if(w==null)return;Engine.put(w,"ended",interrupted?w.optLong("lastFix",w.optLong("started")):System.currentTimeMillis());Engine.put(w,"interrupted",interrupted);d.optJSONArray("history").put(w);d.remove("active");save(c,d);}
 static synchronized void deleteHistory(Context c,String id){JSONObject d=data(c);if(WalkData.deleteHistory(d,id)){JSONArray deleted=d.optJSONArray("deletedIds");if(deleted==null){deleted=new JSONArray();Engine.put(d,"deletedIds",deleted);}boolean found=false;for(int i=0;i<deleted.length();i++)if(id.equals(deleted.optString(i)))found=true;if(!found)deleted.put(id);save(c,d);}}
 static String summary(JSONObject w){double km=w.optDouble("meters")/1000;long end=w.optLong("ended",System.currentTimeMillis());long minutes=Math.max(0,(end-w.optLong("started"))/60000);return String.format(Lang.locale(),"%.2f",km)+Lang.t(" км")+" · "+minutes+Lang.t(" мин");}
}
