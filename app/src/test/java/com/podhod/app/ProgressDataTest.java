package com.podhod.app;
import org.junit.Test;
import org.json.*;
import java.time.*;
import static org.junit.Assert.*;
public class ProgressDataTest {
    JSONObject session(String instant,int count){JSONObject s=new JSONObject();Engine.put(s,"ended",Instant.parse(instant).toEpochMilli());JSONArray sets=new JSONArray();for(int i=0;i<count;i++)sets.put(Engine.obj("{\"done\":true}"));Engine.put(s,"sets",sets);return s;}
    @Test public void countsLocalDaysAndMultipleSessions(){JSONArray h=new JSONArray();h.put(session("2026-09-24T22:30:00Z",6));h.put(session("2026-09-25T12:00:00Z",5));h.put(session("2026-09-26T12:00:00Z",3));ProgressData d=new ProgressData(h,LocalDate.parse("2026-09-25"),ZoneId.of("Asia/Jerusalem"));assertEquals(2,d.sessions);assertEquals(1,d.activeDays);assertEquals(11,d.sets);assertEquals(2,d.count(d.today));assertEquals(3,d.level(d.today));}
    @Test public void monthBoundaryAndWeeklyGrace(){JSONArray h=new JSONArray();h.put(session("2026-09-13T12:00:00Z",1));h.put(session("2026-09-20T12:00:00Z",1));h.put(session("2026-08-27T12:00:00Z",1));ProgressData d=new ProgressData(h,LocalDate.parse("2026-09-26"),ZoneOffset.UTC);assertEquals(2,d.last30);assertEquals(2,d.weeklyStreak);assertEquals(0,d.count(d.today));}
    @Test public void emptyHistoryHasNoInventedActivity(){ProgressData d=new ProgressData(new JSONArray(),LocalDate.now(),ZoneOffset.UTC);assertEquals(0,d.sessions);assertEquals(0,d.weeklyStreak);assertTrue(d.days.isEmpty());}
}
