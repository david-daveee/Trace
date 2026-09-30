package com.podhod.app;

import org.json.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Provider-independent merge for completed walks. Active recordings stay on their device. */
final class SyncMerge {
 static JSONArray walks(JSONArray local,JSONArray remote,Set<String> deletedIds){
  Map<String,JSONObject> merged=new TreeMap<>();
  for(JSONArray list:new JSONArray[]{remote,local}){
   if(list==null)continue;
   for(int i=0;i<list.length();i++){
    JSONObject walk=list.optJSONObject(i);
    if(walk==null||!walk.has("ended"))throw new IllegalArgumentException("Only completed walks can be merged");
    String id=walk.optString("id").trim();
    if(id.isEmpty())id="legacy-"+digest(canonical(walk));
    if(deletedIds.contains(id))continue;
    JSONObject existing=merged.get(id);
    if(existing==null||compare(walk,existing)>0){JSONObject copy=Engine.copy(walk);Engine.put(copy,"id",id);merged.put(id,copy);}
   }
  }
  List<JSONObject> sorted=new ArrayList<>(merged.values());
  sorted.sort(Comparator.comparingLong((JSONObject w)->w.optLong("started")).thenComparing(w->w.optString("id")));
  JSONArray result=new JSONArray();for(JSONObject walk:sorted)result.put(walk);return result;
 }
 static int compare(JSONObject a,JSONObject b){
  int c=Long.compare(a.optLong("ended"),b.optLong("ended"));if(c!=0)return c;
  c=Integer.compare(a.optJSONArray("points")==null?0:a.optJSONArray("points").length(),b.optJSONArray("points")==null?0:b.optJSONArray("points").length());
  if(c!=0)return c;c=Long.compare(a.optLong("metadataUpdatedAt"),b.optLong("metadataUpdatedAt"));
  return c!=0?c:canonical(a).compareTo(canonical(b));
 }
 static String canonical(Object value){
  if(value instanceof JSONObject){JSONObject o=(JSONObject)value;List<String> keys=new ArrayList<>();o.keys().forEachRemaining(keys::add);Collections.sort(keys);StringJoiner parts=new StringJoiner(",","{","}");for(String key:keys)parts.add(JSONObject.quote(key)+":"+canonical(o.opt(key)));return parts.toString();}
  if(value instanceof JSONArray){JSONArray a=(JSONArray)value;StringJoiner parts=new StringJoiner(",","[","]");for(int i=0;i<a.length();i++)parts.add(canonical(a.opt(i)));return parts.toString();}
  if(value instanceof String)return JSONObject.quote((String)value);
  return String.valueOf(value);
 }
 static String digest(String value){try{byte[] bytes=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder text=new StringBuilder();for(byte b:bytes)text.append(String.format(Locale.US,"%02x",b&255));return text.toString();}catch(Exception e){throw new IllegalStateException(e);}}
}
