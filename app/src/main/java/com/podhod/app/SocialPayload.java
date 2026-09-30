package com.podhod.app;

import java.util.*;
import org.json.*;

/** Explicit social exports are separate from account backups. */
final class SocialPayload {
    static String itemId(String kind,String id){return kind+"-"+SyncMerge.digest(id);}
    static JSONObject plan(JSONObject source){
        JSONObject copy=PlanShare.snapshot(source);
        for(String key:new String[]{"exerciseNotes","deleted","deletedItems","confirmedRevision"})copy.remove(key);
        Engine.validateProgram(copy);return Engine.put(new JSONObject(),"plan",copy);
    }
    static JSONObject walk(JSONObject source,boolean route){
        if(source.optLong("ended")<=0)throw new IllegalArgumentException("Only completed walks can be shared");
        JSONObject copy=new JSONObject();
        for(String key:new String[]{"id","started","ended","meters","interrupted","title","coverImage","coverX","coverY","coverZoom"})if(source.has(key))Engine.put(copy,key,source.opt(key));
        Engine.put(copy,"points",route?Engine.obj("{\"points\":"+source.optJSONArray("points")+"}").optJSONArray("points"):new JSONArray());
        metadata(copy);WalkData.validate(Engine.put(new JSONObject(),"history",new JSONArray().put(copy)));
        return Engine.put(Engine.put(new JSONObject(),"walk",copy),"routeShared",route);
    }
    static void validate(JSONObject payload,String kind){
        if(kind.equals("plan")){JSONObject p=payload.optJSONObject("plan");if(p==null)throw new IllegalArgumentException("Invalid shared plan");Engine.validateProgram(p);}
        else if(kind.equals("walk")){JSONObject w=payload.optJSONObject("walk");if(w==null||w.optLong("ended")<=0)throw new IllegalArgumentException("Invalid shared walk");metadata(w);WalkData.validate(Engine.put(new JSONObject(),"history",new JSONArray().put(w)));if(!payload.optBoolean("routeShared")&&w.optJSONArray("points").length()>0)throw new IllegalArgumentException("Unexpected route");}
        else throw new IllegalArgumentException("Unknown shared item");
    }
    static void metadata(JSONObject w){
        for(String key:new String[]{"title","coverImage"})if(w.has(key)&&(!(w.opt(key) instanceof String)||w.optString(key).length()>(key.equals("title")?100:2000000)))throw new IllegalArgumentException("Invalid walk image or title");
        for(String key:new String[]{"coverX","coverY","coverZoom"})if(w.has(key)&&(!Double.isFinite(w.optDouble(key))||w.optDouble(key)<(key.equals("coverZoom")?1:0)||w.optDouble(key)>(key.equals("coverZoom")?4:1)))throw new IllegalArgumentException("Invalid photo position");
    }
    static String code(String input){return input.toUpperCase(Locale.ROOT).replaceAll("[\\s-]","");}
    static String displayCode(String input){String c=code(input);return c.length()==12?c.substring(0,4)+"-"+c.substring(4,8)+"-"+c.substring(8):c;}
}
