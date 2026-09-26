package com.podhod.app;
import org.junit.Test;
import org.json.*;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;
public class PlanShareTest {
 @Test public void sharedPlanImportsWithoutProgressAndRetainsCover()throws Exception{
 JSONObject p=Zlat.create();Zlat.configure(p,20,40,3);Engine.put(p,"coverImage","sample");Engine.put(p,"coverX",.8);Engine.put(p,"coverZoom",2.5);Engine.put(p,"nextDay",5);Engine.put(p,"lastUsed",123);Engine.put(p,"lastProgression","private note");Engine.put(p,"lastProgressedSession","private-session");String original=p.toString();
 JSONObject shared=PlanShare.snapshot(p);assertFalse(shared.has("id"));assertFalse(shared.has("lastUsed"));assertFalse(shared.has("nextDay"));assertFalse(shared.has("lastProgression"));assertFalse(shared.has("lastProgressedSession"));
 JSONObject imported=Importer.parse(shared.toString().getBytes(StandardCharsets.UTF_8),"trace-plan.json");assertEquals(p.optJSONArray("days").toString(),imported.optJSONArray("days").toString());assertEquals(p.optJSONObject("workingWeights").toString(),imported.optJSONObject("workingWeights").toString());assertEquals("sample",imported.optString("coverImage"));assertEquals(.8,imported.optDouble("coverX"),0);assertEquals(2.5,imported.optDouble("coverZoom"),0);assertEquals(original,p.toString());
 }
}
