package com.podhod.app;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class SocialPayloadTest {
 @Test public void planKeepsTrainingButRemovesPersonalState(){
  JSONObject p=Zlat.create();Zlat.configure(p,20,40,3);
  String[] privateKeys={"exerciseNotes","active","history","savedSessions","nextDay","lastUsed","lastProgression","confirmedRevision","deletedItems"};
  for(String key:privateKeys)Engine.put(p,key,"private");String before=p.toString();
  JSONObject exported=SocialPayload.plan(p).optJSONObject("plan");
  for(String key:privateKeys)assertFalse(key,exported.has(key));
  assertEquals(p.optJSONArray("days").toString(),exported.optJSONArray("days").toString());assertEquals(before,p.toString());
 }
 JSONObject walk(){JSONObject w=WalkData.start(1000);WalkData.add(w,32,34,2000,5);Engine.put(w,"ended",5000);Engine.put(w,"privateField","secret");return w;}
 @Test public void statsNeverContainCoordinatesOrPrivateFields(){
  JSONObject w=walk(),payload=SocialPayload.walk(w,false),copy=payload.optJSONObject("walk");
  assertEquals(0,copy.optJSONArray("points").length());assertFalse(copy.has("lastFix"));assertFalse(copy.has("privateField"));assertFalse(payload.optBoolean("routeShared"));SocialPayload.validate(payload,"walk");assertEquals(1,w.optJSONArray("points").length());
 }
 @Test public void routeIsExplicitAndDetached(){JSONObject w=walk(),payload=SocialPayload.walk(w,true);assertTrue(payload.optBoolean("routeShared"));assertEquals(1,payload.optJSONObject("walk").optJSONArray("points").length());w.optJSONArray("points").remove(0);assertEquals(1,payload.optJSONObject("walk").optJSONArray("points").length());SocialPayload.validate(payload,"walk");}
 @Test(expected=IllegalArgumentException.class) public void activeWalkCannotBeShared(){SocialPayload.walk(WalkData.start(1000),true);}
 @Test(expected=IllegalArgumentException.class) public void hiddenRouteCannotBeSmuggled(){JSONObject p=SocialPayload.walk(walk(),true);Engine.put(p,"routeShared",false);SocialPayload.validate(p,"walk");}
 @Test public void friendCodeFormatting(){assertEquals("ABCDEF123456",SocialPayload.code("abcd-ef12-3456 "));assertEquals("ABCD-EF12-3456",SocialPayload.displayCode("abcdef123456"));}
}
