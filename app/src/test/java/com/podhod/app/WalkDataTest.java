package com.podhod.app;
import org.junit.Test;import org.json.*;import static org.junit.Assert.*;
public class WalkDataTest {
 @Test public void validRouteAccumulatesDistance(){JSONObject w=WalkData.start(1000);assertTrue(WalkData.add(w,50,30,1000,5));assertTrue(WalkData.add(w,50.0001,30,11000,5));assertEquals(11.12,w.optDouble("meters"),.1);}
 @Test public void rejectsStaleNoisyAndImpossiblePoints(){JSONObject w=WalkData.start(1000);assertFalse(WalkData.add(w,50,30,900,5));assertFalse(WalkData.add(w,50,30,1000,100));assertFalse(WalkData.add(w,Double.NaN,30,1000,5));assertTrue(WalkData.add(w,50,30,1000,5));assertFalse(WalkData.add(w,50,30,1000,5));assertFalse(WalkData.add(w,51,30,2000,5));assertFalse(WalkData.add(w,50.000001,30,6000,5));assertEquals(1,w.optJSONArray("points").length());}
 @Test public void signalGapDoesNotInventDistance(){JSONObject w=WalkData.start(1000);WalkData.add(w,50,30,1000,5);assertTrue(WalkData.add(w,51,31,200000,5));assertTrue(w.optJSONArray("points").optJSONArray(1).optBoolean(4));assertEquals(0,w.optDouble("meters"),0);}
 @Test public void endedWalkCannotCollectMore(){JSONObject w=WalkData.start(1000);Engine.put(w,"ended",2000);assertFalse(WalkData.add(w,50,30,3000,5));}
 @Test public void rejectsInvalidBackupCoordinates(){JSONObject data=Engine.obj("{\"history\":[]}");JSONObject w=WalkData.start(1000);WalkData.add(w,50,30,1000,5);data.optJSONArray("history").put(w);WalkData.validate(data);Engine.put(w,"points",Engine.obj("{\"p\":[[999,30,1000,5,false]]}").optJSONArray("p"));try{WalkData.validate(data);fail();}catch(IllegalArgumentException expected){}}
}
