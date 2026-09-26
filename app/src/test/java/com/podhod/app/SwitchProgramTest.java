package com.podhod.app;
import org.junit.Test;
import org.json.*;
import static org.junit.Assert.*;
public class SwitchProgramTest {
    JSONObject plan(String id){JSONObject p=Zlat.create();Engine.put(p,"id",id);Zlat.configure(p,20,40,3);return p;}
    @Test public void switchingPreservesSetsAndResumesExactSession(){JSONObject a=plan("a"),b=plan("b"),data=Engine.obj("{}");Engine.selectProgram(data,a,100);JSONObject old=data.optJSONObject("active");Engine.act(old,"next",200);String sid=old.optString("id");Engine.selectProgram(data,b,1200);assertEquals("b",data.optJSONObject("active").optString("programId"));assertEquals(0,Engine.doneCount(data.optJSONObject("active")));JSONObject parked=data.optJSONObject("savedSessions").optJSONObject("a");assertEquals(1,Engine.doneCount(parked));assertTrue(parked.optBoolean("paused"));assertFalse(parked.has("remaining"));assertFalse(parked.optBoolean("rest"));Engine.selectProgram(data,a,5000);JSONObject resumed=data.optJSONObject("active");assertEquals(sid,resumed.optString("id"));assertEquals(1,resumed.optInt("cursor"));assertEquals(1,Engine.doneCount(resumed));assertTrue(resumed.optInt("revision")>old.optInt("revision"));assertEquals(0,a.optInt("nextDay"));assertNotNull(data.optJSONObject("savedSessions").optJSONObject("b"));String before=data.toString();Engine.selectProgram(data,a,6000);assertEquals(before,data.toString());}
    @Test public void invalidTargetDoesNotParkCurrentSession(){JSONObject a=plan("a"),b=plan("b"),data=Engine.obj("{}");Engine.selectProgram(data,a,1);Engine.put(b,"nextDay",12);String before=data.toString();try{Engine.selectProgram(data,b,2);fail();}catch(IllegalArgumentException expected){}assertEquals(before,data.toString());}
}
