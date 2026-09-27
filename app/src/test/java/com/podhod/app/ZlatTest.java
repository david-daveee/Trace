package com.podhod.app;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ZlatTest {
    JSONObject plan(){JSONObject p=Zlat.create();Zlat.configure(p,20,40,3);return p;}
    @Test public void migrationDoesNotDuplicateOrChangeExistingData(){JSONObject data=Engine.obj("{\"programs\":[{\"id\":\"mine\",\"name\":\"Existing\"}],\"history\":[{\"id\":\"old\"}]}");assertTrue(Zlat.ensure(data));String once=data.toString();assertFalse(Zlat.ensure(data));assertEquals(once,data.toString());assertEquals("mine",data.optJSONArray("programs").optJSONObject(0).optString("id"));}
    @Test public void validScheduleAndFourSetConfiguration(){JSONObject p=plan();Engine.validateProgram(p);assertEquals(12,p.optJSONArray("days").length());Zlat.configure(p,12.5,35,4);JSONObject s=Engine.start(p,1);assertEquals(8,s.optJSONArray("sets").length());assertEquals(12.5,Zlat.last(s,"pullup").optDouble("kg"),0);assertEquals(35,Zlat.last(s,"dip").optDouble("kg"),0);}
    @Test public void incrementsCoverAllBoundaries(){assertEquals(0,Zlat.increment(4),0);assertEquals(0,Zlat.increment(5),0);assertEquals(1.25,Zlat.increment(6),0);assertEquals(2.5,Zlat.increment(7),0);assertEquals(5,Zlat.increment(8),0);assertEquals(5,Zlat.increment(10),0);}
    @Test public void progressUsesLastPlannedSetAndPreservesHistory(){JSONObject p=plan(),s=Engine.start(p,1);JSONArray sets=s.optJSONArray("sets");for(int i=sets.length()-1;i>=0;i--)Engine.setDone(s,i,true,i);Engine.put(Zlat.last(s,"pullup"),"reps",6);Engine.put(Zlat.last(s,"dip"),"reps",4);Engine.put(Zlat.last(s,"dip"),"kg",45);String snapshot=s.toString();Zlat.advance(p,s);assertEquals(snapshot,s.toString());JSONObject w=p.optJSONObject("workingWeights");assertEquals(21.25,w.optDouble("pullup"),0);assertEquals(45,w.optDouble("dip"),0);assertEquals(20,p.optJSONArray("days").optJSONObject(0).optJSONArray("groups").optJSONObject(0).optDouble("kg"),0);Engine.put(p,"nextDay",1);assertEquals(21.25,Zlat.last(Engine.start(p,3),"pullup").optDouble("kg"),0);String after=p.toString();Zlat.advance(p,s);assertEquals(after,p.toString());}
    @Test public void fiveRepsDoNotRequireMicroplates(){JSONObject p=plan(),s=Engine.start(p,1);for(int i=0;i<s.optJSONArray("sets").length();i++)Engine.setDone(s,i,true,i);Zlat.advance(p,s);assertEquals(20,p.optJSONObject("workingWeights").optDouble("pullup"),0);assertEquals(40,p.optJSONObject("workingWeights").optDouble("dip"),0);}
    @Test public void invalidSetupIsAtomic(){JSONObject p=plan();String before=p.toString();try{Zlat.configure(p,25,45,2);fail("Must reject two sets");}catch(IllegalArgumentException expected){}assertEquals(before,p.toString());}
}
