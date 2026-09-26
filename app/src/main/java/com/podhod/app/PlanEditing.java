package com.podhod.app;
import org.json.*;
final class PlanEditing {
 static void move(JSONArray groups,int from,int to){if(from<0||to<0||from>=groups.length()||to>=groups.length()||from==to)return;Object item=groups.remove(from);try{for(int i=groups.length();i>to;i--)groups.put(i,groups.get(i-1));groups.put(to,item);}catch(JSONException e){throw new IllegalArgumentException(e);}}
 static void copyDay(JSONObject plan,int index,String name){JSONObject day=Engine.copy(plan.optJSONArray("days").optJSONObject(index));Engine.put(day,"name",name);plan.optJSONArray("days").put(day);}
 static void sets(JSONObject group,int delta){int value=group.optInt("sets")+delta;if(value>=1&&value<=100)Engine.put(group,"sets",value);}
}
