package com.podhod.app;
import org.json.*;
/** Removes private coordinates before serialization, including return visits near endpoints. */
final class WalkSharing {
 static JSONObject payload(JSONObject walk,boolean route,double radius){
  JSONObject result=SocialPayload.walk(walk,route);JSONObject safe=result.optJSONObject("walk");
  JSONArray points=safe.optJSONArray("points");
  if(!route||radius<=0||points.length()==0)return result;
  JSONArray first=points.optJSONArray(0),last=points.optJSONArray(points.length()-1),kept=new JSONArray();boolean gap=true;
  for(int i=0;i<points.length();i++){JSONArray point=points.optJSONArray(i);
   if(near(point,first,radius)||near(point,last,radius)){gap=true;continue;}
   JSONArray copy;try{copy=new JSONArray(point.toString());copy.put(4,gap||point.optBoolean(4));}catch(JSONException e){throw new IllegalArgumentException(e);}
   kept.put(copy);gap=false;
  }
  Engine.put(safe,"points",kept);Engine.put(result,"routeTrimmed",true);return result;
 }
 static boolean near(JSONArray p,JSONArray center,double radius){return WalkData.distance(p.optDouble(0),p.optDouble(1),center.optDouble(0),center.optDouble(1))<=radius;}
 static String item(String walkId){return SocialPayload.itemId("walk",walkId);}
 static String direct(String walkId,String peer){return item(walkId)+"-"+SyncMerge.digest(peer);}
 static boolean belongs(String item,String walkId){String base=item(walkId);return item.equals(base)||item.startsWith(base+"-");}
}
