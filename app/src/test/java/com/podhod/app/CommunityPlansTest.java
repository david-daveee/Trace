package com.podhod.app;
import org.junit.Test;import org.json.*;import static org.junit.Assert.*;
public class CommunityPlansTest {
 JSONObject plan(){JSONObject p=Zlat.create();Engine.put(p,"exerciseNotes",new JSONObject());Engine.put(p,"mine",true);Engine.put(p,"nextDay",3);Engine.put(p,"author",Engine.put(new JSONObject(),"uid","untrusted"));return p;}
 @Test public void publishedSnapshotExcludesPrivateAndAuthFields(){JSONObject p=plan(),s=CommunityPlans.snapshot(p);assertFalse(s.has("author"));assertFalse(s.has("exerciseNotes"));assertFalse(s.has("nextDay"));assertFalse(s.has("mine"));assertFalse(s.has("id"));assertTrue(p.has("author"));}
 @Test public void submittedDaysAreIndependent(){JSONObject p=plan(),s=CommunityPlans.snapshot(p);Engine.put(s.optJSONArray("days").optJSONObject(0),"name","changed");assertNotEquals("changed",p.optJSONArray("days").optJSONObject(0).optString("name"));}
 @Test public void requestsAreScopedToAuthor(){JSONObject p=plan();assertNotEquals(CommunityPlans.requestId("a",p),CommunityPlans.requestId("b",p));}
}
