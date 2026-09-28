package com.podhod.app;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class SheikoBarbellsTest {
 @Test public void migrationPreservesWeightsHistoryAndExplicitOptOut(){JSONObject p=Engine.obj("{\"id\":\"sheiko\",\"routine\":\"sheiko-cms-ms\",\"days\":[{\"groups\":[{\"exercise\":\"Жим лёжа\",\"kg\":100},{\"exercise\":\"Присед\",\"barbell\":false},{\"exercise\":\"Брусья\"},{\"exercise\":\"Жим ногами\"}]}]}");JSONObject data=Engine.obj("{\"programs\":[],\"history\":[{\"id\":\"old\"}],\"active\":{\"programId\":\"sheiko\",\"sets\":[{\"exercise\":\"Жим лёжа\",\"kg\":100,\"done\":true}]}}");data.optJSONArray("programs").put(p);String history=data.optJSONArray("history").toString();SheikoBarbells.migrate(data);JSONArray gs=p.optJSONArray("days").optJSONObject(0).optJSONArray("groups");assertTrue(gs.optJSONObject(0).optBoolean("barbell"));assertFalse(gs.optJSONObject(1).optBoolean("barbell"));assertFalse(gs.optJSONObject(2).has("barbell"));assertFalse(gs.optJSONObject(3).has("barbell"));JSONObject set=data.optJSONObject("active").optJSONArray("sets").optJSONObject(0);assertTrue(set.optBoolean("barbell"));assertTrue(set.optBoolean("done"));assertEquals(100,set.optDouble("kg"),.001);assertEquals(history,data.optJSONArray("history").toString());String once=data.toString();SheikoBarbells.migrate(data);assertEquals(once,data.toString());}
}
