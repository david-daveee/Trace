package com.podhod.app;
import org.junit.Test;
import org.json.*;
import static org.junit.Assert.*;
public class PlanManagementTest {
    JSONObject fixture(){return Engine.obj("{\"programs\":[{\"id\":\"custom\"},{\"id\":\"other\"},{\"id\":\"builtin-test\"}],\"history\":[{\"programId\":\"custom\",\"ended\":123}],\"active\":{\"programId\":\"custom\"},\"savedSessions\":{\"custom\":{\"programId\":\"custom\"},\"other\":{\"programId\":\"other\"}}}");}
    @Test public void deleteClearsOnlyMatchingPlanAndUnfinishedSessions(){JSONObject d=fixture();String history=d.optJSONArray("history").toString();assertTrue(PlanManagement.delete(d,"custom"));assertEquals(2,d.optJSONArray("programs").length());assertFalse(d.has("active"));assertFalse(d.optJSONObject("savedSessions").has("custom"));assertTrue(d.optJSONObject("savedSessions").has("other"));assertEquals(history,d.optJSONArray("history").toString());}
    @Test public void deletingAnotherPlanPreservesActiveSession(){JSONObject d=fixture();String active=d.optJSONObject("active").toString();assertTrue(PlanManagement.delete(d,"other"));assertEquals(active,d.optJSONObject("active").toString());}
    @Test public void builtinsAndMissingPlansCannotBeDeleted(){JSONObject d=fixture();String before=d.toString();assertFalse(PlanManagement.delete(d,"builtin-test"));assertFalse(PlanManagement.delete(d,"missing"));assertEquals(before,d.toString());}
}
