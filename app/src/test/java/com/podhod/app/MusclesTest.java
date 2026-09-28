package com.podhod.app;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class MusclesTest {
 @Test public void completedSetsOnlyAndManualOverrides(){JSONArray sets=Engine.obj("{\"sets\":[{\"exercise\":\"Жим лёжа\",\"done\":true},{\"exercise\":\"Жим лёжа\",\"done\":false},{\"exercise\":\"Жим лёжа\",\"done\":true,\"muscles\":[\"back\"]},{\"exercise\":\"Unknown\",\"done\":true}]}").optJSONArray("sets");assertEquals(Integer.valueOf(1),Muscles.count(sets).get("chest"));assertEquals(Integer.valueOf(1),Muscles.count(sets).get("back"));assertEquals(1,Muscles.unknown(sets));}
 @Test public void distinguishRowsAndGoodMornings(){assertTrue(Muscles.of(Engine.obj("{\"exercise\":\"Тяга в наклоне\"}")).contains("back"));assertFalse(Muscles.of(Engine.obj("{\"exercise\":\"Тяга в наклоне\"}")).contains("hamstrings"));assertTrue(Muscles.of(Engine.obj("{\"exercise\":\"Наклоны со штангой стоя\"}")).contains("hamstrings"));assertTrue(Muscles.of(Engine.obj("{\"exercise\":\"Подтягивания с весом\"}")).contains("biceps"));assertTrue(Muscles.of(Engine.obj("{\"exercise\":\"custom\",\"muscles\":[]}")).isEmpty());}
 @Test public void paceUsesElapsedTimeAndHandlesMissingFix(){JSONObject w=Engine.obj("{\"started\":1000,\"ended\":601000,\"meters\":1000}");assertEquals("10:00",WalkVisuals.pace(w,900000));assertEquals("10:00",WalkVisuals.time(w,900000));Engine.put(w,"meters",0);assertEquals("—",WalkVisuals.pace(w,900000));}
}
