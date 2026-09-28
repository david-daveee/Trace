package com.podhod.app;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class SyncSessionsTest {
 JSONObject fixture()throws Exception {
  JSONObject p=new JSONObject("{\"id\":\"p\",\"name\":\"Plan\",\"nextDay\":0,\"days\":[{\"name\":\"Day\",\"groups\":[{\"exercise\":\"Squat\",\"sets\":3,\"reps\":5,\"kg\":100}]}]}");
  JSONObject workouts=new JSONObject().put("programs",new JSONArray().put(p)).put("history",new JSONArray()).put("active",Engine.start(p,1000));
  return new JSONObject().put("format","trace-backup").put("version",2).put("workouts",workouts).put("steps",new JSONObject().put("days",new JSONObject()).put("goal",8000).put("stepLengthCm",70)).put("walks",new JSONObject().put("history",new JSONArray())).put("settings",new JSONObject().put("language","en"));
 }
 @Test public void resumableSessionValidatesAndKeepsDoneSets()throws Exception {
  JSONObject p=fixture(),s=p.getJSONObject("workouts").getJSONObject("active");
  Engine.setDone(s,0,true,2000);SyncLocal.validate(p);
  JSONObject copy=SyncPayload.choose(fixture(),p,false);
  assertTrue(copy.getJSONObject("workouts").getJSONObject("active").getJSONArray("sets").getJSONObject(0).getBoolean("done"));
  assertEquals(1,Engine.firstUndone(copy.getJSONObject("workouts").getJSONObject("active")));
 }
 @Test(expected=IllegalArgumentException.class) public void missingPlanRejected()throws Exception {
  JSONObject p=fixture();p.getJSONObject("workouts").getJSONObject("active").put("programId","missing");SyncLocal.validate(p);
 }
 @Test(expected=IllegalArgumentException.class) public void duplicateActiveAndPausedRejected()throws Exception {
  JSONObject p=fixture(),w=p.getJSONObject("workouts");w.put("savedSessions",new JSONObject().put("p",Engine.copy(w.getJSONObject("active"))));SyncLocal.validate(p);
 }
 @Test(expected=IllegalArgumentException.class) public void completedSessionCannotBeActive()throws Exception {
  JSONObject p=fixture();p.getJSONObject("workouts").getJSONObject("active").put("ended",2000);SyncLocal.validate(p);
 }
}
