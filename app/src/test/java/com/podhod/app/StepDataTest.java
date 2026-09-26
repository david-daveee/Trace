package com.podhod.app;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class StepDataTest {
    @Test public void walkingSummaryUsesSevenCalendarDaysAndExcludesFuture(){JSONObject d=Engine.obj("{\"days\":{\"2025-12-25\":100,\"2025-12-26\":10,\"2025-12-31\":20,\"2026-01-01\":30,\"2026-01-02\":999,\"invalid\":88}}");java.time.LocalDate day=java.time.LocalDate.of(2026,1,1);assertEquals(60,StepData.week(d,day));assertEquals(160,StepData.total(d,day));assertEquals(0,StepData.week(new JSONObject(),day));}
    @Test public void firstReadingDoesNotImportPastSteps(){JSONObject d=new JSONObject();StepData.accept(d,42000,1,10,"2026-09-26");assertEquals(0,StepData.count(d,"2026-09-26"));StepData.accept(d,42017,1,20,"2026-09-26");assertEquals(17,StepData.count(d,"2026-09-26"));}
    @Test public void duplicateAndOlderEventsDoNotDoubleCount(){JSONObject d=new JSONObject();StepData.accept(d,100,1,10,"2026-09-26");StepData.accept(d,120,1,20,"2026-09-26");assertFalse(StepData.accept(d,120,1,20,"2026-09-26"));assertFalse(StepData.accept(d,110,1,15,"2026-09-26"));assertEquals(20,StepData.count(d,"2026-09-26"));}
    @Test public void midnightUsesEventDateWithoutCarryingYesterday(){JSONObject d=new JSONObject();StepData.accept(d,100,1,10,"2026-09-26");StepData.accept(d,130,1,20,"2026-09-26");assertEquals(0,StepData.count(d,"2026-09-27"));StepData.accept(d,137,1,30,"2026-09-27");assertEquals(7,StepData.count(d,"2026-09-27"));assertEquals(30,StepData.count(d,"2026-09-26"));}
    @Test public void rebootAndCounterResetDoNotAddLifetimeTotals(){JSONObject d=new JSONObject();StepData.accept(d,100,1,100,"2026-09-26");StepData.accept(d,130,1,200,"2026-09-26");StepData.accept(d,500,2,10,"2026-09-26");StepData.accept(d,504,2,20,"2026-09-26");assertEquals(34,StepData.count(d,"2026-09-26"));StepData.accept(d,1,2,30,"2026-09-26");StepData.accept(d,3,2,40,"2026-09-26");assertEquals(36,StepData.count(d,"2026-09-26"));}
    @Test public void disableGapIsExcludedAndSavedStateContinues(){JSONObject d=new JSONObject();StepData.accept(d,100,1,10,"2026-09-26");StepData.accept(d,120,1,20,"2026-09-26");d=Engine.copy(d);StepData.resetBaseline(d);StepData.accept(d,800,1,30,"2026-09-26");StepData.accept(d,805,1,40,"2026-09-26");assertEquals(25,StepData.count(d,"2026-09-26"));}
    @Test public void invalidReadingsDoNotChangeData(){JSONObject d=new JSONObject();assertFalse(StepData.accept(d,-1,1,100,"2026-09-26"));assertFalse(StepData.accept(d,100,1,-1,"2026-09-26"));assertEquals("{}",d.toString());}
}
