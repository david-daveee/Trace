package com.podhod.app;
import org.json.*;
/** Stable built-in identity across fresh installs, legacy backups and public snapshots. */
final class PlanIdentity {
 static String routine(JSONObject p){
  String routine=p.optString("routine");if(!routine.isEmpty())return routine;
  if(p.optBoolean("createdLocally")||!("Подготовительный цикл · 4 недели · Б. И. Шейко".equals(p.optString("description"))||"Preparatory cycle · 4 weeks · B. I. Sheiko".equals(p.optString("description"))))return "";
  JSONObject author=p.optJSONObject("author");if(author!=null&&!CommunityPlans.ADMIN.equals(author.optString("uid")))return "";
  JSONArray days=p.optJSONArray("days");int[] rows={6,12,18,23,29,35,41,46,52,58,65,71,77,83,89,95};
  if(days==null||days.length()!=rows.length)return "";
  for(int i=0;i<rows.length;i++){JSONObject day=days.optJSONObject(i);JSONArray groups=day==null?null:day.optJSONArray("groups");JSONObject first=groups==null?null:groups.optJSONObject(0);if(first==null||first.optInt("sourceRow",-1)!=rows[i])return "";}
  return "sheiko-cms-ms";
 }
}
