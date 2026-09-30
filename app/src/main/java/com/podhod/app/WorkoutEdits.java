package com.podhod.app;
import org.json.*;
final class WorkoutEdits {
 static void apply(JSONObject active,String sessionId,int index,String exercise,int group,int ordinal,double kg,int reps,boolean all,JSONObject plan){
  if(active==null||!sessionId.equals(active.optString("id")))throw new IllegalArgumentException(Lang.t("Этот подход уже выполнен"));
  JSONArray sets=active.optJSONArray("sets");JSONObject selected=sets==null?null:sets.optJSONObject(index);
  if(selected==null||!exercise.equals(selected.optString("exercise"))||group!=selected.optInt("group")||ordinal!=selected.optInt("ordinal"))throw new IllegalArgumentException(AccountPanel.s("This workout changed. Reopen the set.","Тренировка изменилась. Открой подход заново."));
  for(int n=index;n<sets.length();n++){JSONObject x=sets.optJSONObject(n);if(n==index||(all&&!x.optBoolean("done")&&x.optInt("group")==group)){
   Engine.put(x,"kg",kg);Engine.put(x,"reps",reps);
   Engine.put(x,"manualWeight",!x.has("factor")||plan==null||Math.abs(kg-Engine.weight(x,plan.optJSONObject("maxima"),plan.optDouble("step",2.5)))>.00001);
  }}
  Engine.put(active,"revision",active.optInt("revision")+1);
 }
}
