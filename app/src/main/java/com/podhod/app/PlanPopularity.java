package com.podhod.app;
import org.json.*;
import java.util.*;
final class PlanPopularity {
 static String key(JSONObject p){
  String routine=p.optString("routine");
  if(!routine.isEmpty())return SyncMerge.digest("routine:"+routine);
  String id=p.optString("popularityId",p.optString("id"));
  if(id.isEmpty())throw new IllegalArgumentException("Plan has no identity");
  return SyncMerge.digest("plan:"+id);
 }
 static void identity(JSONObject p){if(p.optString("routine").isEmpty()&&p.optString("popularityId").isEmpty()&&!p.optString("id").isEmpty())Engine.put(p,"popularityId",p.optString("id"));}
 static boolean matches(JSONObject p,String query){
  StringBuilder text=new StringBuilder(p.optString("name")).append(' ').append(Lang.content(p.optString("name"))).append(' ').append(PlanCategory.label(p));
  JSONArray days=p.optJSONArray("days");if(days!=null)for(int d=0;d<days.length();d++){JSONObject day=days.optJSONObject(d);text.append(' ').append(day.optString("name"));JSONArray groups=day.optJSONArray("groups");if(groups!=null)for(int g=0;g<groups.length();g++){String name=groups.optJSONObject(g).optString("exercise");text.append(' ').append(name).append(' ').append(Lang.content(name));}}
  String hay=text.toString().toLowerCase(Locale.ROOT).replace('ё','е');
  for(String word:query.trim().toLowerCase(Locale.ROOT).replace('ё','е').split("\\s+"))if(!hay.contains(word))return false;
  return true;
 }
}
