package com.podhod.app;
import org.json.*;
final class WalkData {
 static JSONObject start(long now){JSONObject w=new JSONObject();Engine.put(w,"id",Engine.id());Engine.put(w,"started",now);Engine.put(w,"points",new JSONArray());Engine.put(w,"meters",0);return w;}
 static double distance(double lat,double lon,double lat2,double lon2){double a=Math.sin(Math.toRadians(lat2-lat)/2),b=Math.sin(Math.toRadians(lon2-lon)/2);double h=a*a+Math.cos(Math.toRadians(lat))*Math.cos(Math.toRadians(lat2))*b*b;return 6371000*2*Math.asin(Math.sqrt(Math.min(1,h)));}
 static boolean add(JSONObject w,double lat,double lon,long time,float accuracy){
  if(w.has("ended")||!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180||!Float.isFinite(accuracy)||accuracy<0||accuracy>50||time<w.optLong("started"))return false;
  JSONArray points=w.optJSONArray("points");if(points.length()>=10000)return false;double d=0;boolean gap=false;
  if(points.length()>0){JSONArray last=points.optJSONArray(points.length()-1);long dt=time-last.optLong(2);if(dt<=0)return false;d=distance(last.optDouble(0),last.optDouble(1),lat,lon);if(d<Math.max(5,accuracy*.5))return false;gap=dt>120000;if(!gap&&d/(dt/1000.0)>12)return false;}
  JSONArray point=new JSONArray();point.put((Object)lat);point.put((Object)lon);point.put(time);point.put((Object)accuracy);point.put(gap);points.put(point);if(!gap)Engine.put(w,"meters",w.optDouble("meters")+d);Engine.put(w,"lastFix",time);return true;
 }
 static void validate(JSONObject data){if(data.optJSONArray("history")==null)throw new IllegalArgumentException();JSONArray all=Engine.copy(data).optJSONArray("history");if(data.optJSONObject("active")!=null)all.put(data.optJSONObject("active"));for(int i=0;i<all.length();i++){JSONObject w=all.optJSONObject(i);if(w==null||w.optLong("started")<=0||!Double.isFinite(w.optDouble("meters"))||w.optDouble("meters")<0)throw new IllegalArgumentException();JSONArray pts=w.optJSONArray("points");if(pts==null||pts.length()>10000)throw new IllegalArgumentException();for(int j=0;j<pts.length();j++){JSONArray p=pts.optJSONArray(j);if(p==null||!Double.isFinite(p.optDouble(0))||Math.abs(p.optDouble(0))>90||!Double.isFinite(p.optDouble(1))||Math.abs(p.optDouble(1))>180)throw new IllegalArgumentException();}}}
}
