package com.podhod.app;
import org.json.*;
/** Targeted undo records; restoring never rolls back unrelated workout activity. */
final class DeletedItems {
 static JSONArray items(JSONObject data){JSONArray a=data.optJSONArray("deletedItems");if(a==null){a=new JSONArray();Engine.put(data,"deletedItems",a);}return a;}
 static JSONObject last(JSONObject data){JSONArray a=data.optJSONArray("deletedItems");return a==null?null:a.optJSONObject(a.length()-1);}
 static void plan(JSONObject data,JSONObject plan){
  if(!PlanManagement.canDelete(plan))return;JSONObject item=new JSONObject();String id=plan.optString("id");Engine.put(item,"kind","plan");Engine.put(item,"name",plan.optString("name"));Engine.put(item,"plan",Engine.copy(plan));
  JSONObject active=data.optJSONObject("active"),saved=data.optJSONObject("savedSessions");JSONObject session=active!=null&&id.equals(active.optString("programId"))?active:saved==null?null:saved.optJSONObject(id);if(session!=null)Engine.put(item,"session",Engine.copy(session));
  if(PlanManagement.delete(data,id))items(data).put(item);
 }
 static void exercise(JSONObject data,JSONObject plan,int day,int index){
  JSONArray groups=plan.optJSONArray("days").optJSONObject(day).optJSONArray("groups");if(groups.length()<=1)throw new IllegalArgumentException(Lang.t("В дне должно остаться хотя бы одно упражнение"));
  JSONObject group=groups.optJSONObject(index);if(group==null)return;JSONObject item=new JSONObject();Engine.put(item,"kind","exercise");Engine.put(item,"name",group.optString("exercise"));Engine.put(item,"programId",plan.optString("id"));Engine.put(item,"day",day);Engine.put(item,"index",index);Engine.put(item,"group",Engine.copy(group));groups.remove(index);items(data).put(item);
 }
 static boolean undo(JSONObject data){JSONObject item=last(data);if(item==null)return false;JSONArray plans=data.optJSONArray("programs");
  if("plan".equals(item.optString("kind"))){JSONObject plan=Engine.copy(item.optJSONObject("plan"));String id=plan.optString("id");for(int i=0;i<plans.length();i++)if(id.equals(plans.optJSONObject(i).optString("id")))return false;plans.put(plan);
   JSONObject session=item.optJSONObject("session");if(session!=null){JSONObject saved=data.optJSONObject("savedSessions");if(saved==null){saved=new JSONObject();Engine.put(data,"savedSessions",saved);}JSONObject restored=Engine.copy(session);Engine.put(restored,"paused",true);Engine.put(saved,id,restored);}
  }else{JSONObject plan=null;for(int i=0;i<plans.length();i++)if(item.optString("programId").equals(plans.optJSONObject(i).optString("id")))plan=plans.optJSONObject(i);if(plan==null)return false;JSONObject day=plan.optJSONArray("days").optJSONObject(item.optInt("day"));if(day==null)return false;JSONArray old=day.optJSONArray("groups"),next=new JSONArray();int at=Math.min(item.optInt("index"),old.length());for(int i=0;i<=old.length();i++){if(i==at)next.put(Engine.copy(item.optJSONObject("group")));if(i<old.length())next.put(old.optJSONObject(i));}Engine.put(day,"groups",next);}
  JSONArray a=items(data);a.remove(a.length()-1);return true;
 }
}
