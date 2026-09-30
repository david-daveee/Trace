package com.podhod.app;
import org.junit.Test;import org.json.*;import static org.junit.Assert.*;
public class WorkoutEditsTest {
 @Test public void editsFreshSessionAfterSyncReplacedTheObject(){JSONObject old=new JSONObject();Engine.put(old,"id","session");JSONObject set=new JSONObject();Engine.put(set,"exercise","Pullup");Engine.put(set,"group",0);Engine.put(set,"ordinal",1);Engine.put(set,"kg",100);Engine.put(set,"reps",5);Engine.put(old,"sets",new JSONArray().put(set));JSONObject current=Engine.copy(old);
 WorkoutEdits.apply(current,"session",0,"Pullup",0,1,10,5,false,null);
 assertEquals(10,current.optJSONArray("sets").optJSONObject(0).optDouble("kg"),0);assertEquals(100,set.optDouble("kg"),0);assertEquals(1,current.optInt("revision"));}
}
