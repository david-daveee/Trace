package com.podhod.app;
import org.json.*;
import java.util.*;

/** In-place content migration: IDs, load formulas and training records stay intact. */
final class EnglishPlans {
 static void plan(JSONObject p){
  if(p==null||!CommunityPlans.builtin(p))return;
  fields(p,"name","description","terminalNote","source","categoryName");
  JSONArray days=p.optJSONArray("days");
  if(days!=null)for(int i=0;i<days.length();i++){JSONObject day=days.optJSONObject(i);fields(day,"name","phase");if("Проходка: в исходном плане есть подходы до 105% от заданных максимумов.".equals(day.optString("note")))fields(day,"note");groups(day.optJSONArray("groups"));}
  notes(p);
 }
 static void fields(JSONObject o,String... fields){if(o==null)return;for(String key:fields)if(o.opt(key) instanceof String)Engine.put(o,key,EnglishPlanText.text(o.optString(key)));}
 static void groups(JSONArray groups){if(groups!=null)for(int i=0;i<groups.length();i++)fields(groups.optJSONObject(i),"exercise");}
 static void notes(JSONObject owner){
  JSONObject notes=owner.optJSONObject("exerciseNotes");if(notes==null)return;
  List<String> keys=new ArrayList<>();notes.keys().forEachRemaining(keys::add);
  for(String key:keys){if("Удерживай 15–30 секунд. 1 повтор в журнале = одно удержание.".equals(notes.optString(key)))Engine.put(notes,key,EnglishPlanText.text(notes.optString(key)));String translated=EnglishPlanText.text(key);if(key.equals(translated))continue;
   // Keep both entries on a collision; never overwrite a user's note.
   if(!notes.has(translated)){Engine.put(notes,translated,notes.opt(key));notes.remove(key);}
  }
 }
 static void session(JSONObject s,Set<String> ids){if(s==null||!ids.contains(s.optString("programId")))return;fields(s,"programName","dayName");groups(s.optJSONArray("sets"));notes(s);}
 static void migrate(JSONObject data){
  Set<String> ids=new HashSet<>();JSONArray plans=data.optJSONArray("programs");
  if(plans!=null)for(int i=0;i<plans.length();i++){JSONObject p=plans.optJSONObject(i);if(p!=null&&CommunityPlans.builtin(p)){ids.add(p.optString("id"));plan(p);}}
  session(data.optJSONObject("active"),ids);
  JSONObject saved=data.optJSONObject("savedSessions");if(saved!=null){Iterator<String> keys=saved.keys();while(keys.hasNext())session(saved.optJSONObject(keys.next()),ids);}
  JSONArray history=data.optJSONArray("history");if(history!=null)for(int i=0;i<history.length();i++)session(history.optJSONObject(i),ids);
 }
}
