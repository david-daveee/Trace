package com.podhod.app;
import org.json.*;
/** Deletes a personal plan and its unfinished sessions, retaining completed history. */
final class PlanManagement {
    static boolean canDelete(JSONObject p){return p!=null&&!p.optString("id").isEmpty()&&!p.optString("id").startsWith("builtin-");}
    static boolean delete(JSONObject data,String id){
        JSONArray plans=data.optJSONArray("programs");
        for(int i=0;i<plans.length();i++){
            JSONObject p=plans.optJSONObject(i);
            if(!id.equals(p.optString("id")))continue;
            if(!canDelete(p))return false;
            plans.remove(i);
            JSONObject active=data.optJSONObject("active");
            if(active!=null&&id.equals(active.optString("programId")))data.remove("active");
            JSONObject saved=data.optJSONObject("savedSessions");if(saved!=null)saved.remove(id);
            return true;
        }
        return false;
    }
}
