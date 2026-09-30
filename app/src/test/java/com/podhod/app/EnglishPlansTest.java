package com.podhod.app;
import org.junit.Test;
import org.json.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

public class EnglishPlansTest {
 JSONObject asset(String file)throws Exception{return Engine.obj(new String(Files.readAllBytes(Path.of("src/main/assets/"+file)),java.nio.charset.StandardCharsets.UTF_8));}
 void english(String value){assertFalse(value,value.matches("(?s).*[А-Яа-яЁё].*"));}
 void checkPlan(JSONObject p){
  english(p.optString("name"));english(p.optString("description"));english(p.optString("terminalNote"));
  JSONArray days=p.optJSONArray("days");for(int d=0;d<days.length();d++){JSONObject day=days.optJSONObject(d);english(day.optString("name"));english(day.optString("phase"));english(day.optString("note"));JSONArray groups=day.optJSONArray("groups");for(int g=0;g<groups.length();g++)english(groups.optJSONObject(g).optString("exercise"));}
 }
 @Test public void allShippedContentIsEnglishWithoutAndroidLocale()throws Exception{
  checkPlan(asset("sheiko.json"));checkPlan(asset("sheiko_competition.json"));
  JSONArray home=new JSONArray(new String(Files.readAllBytes(Path.of("src/main/assets/home_plans.json")),java.nio.charset.StandardCharsets.UTF_8));for(int i=0;i<home.length();i++)checkPlan(home.optJSONObject(i));
  for(JSONObject p:new JSONObject[]{Zlat.create(),ZlatIntermediate.create(),ZlatAdvanced.create()}){checkPlan(p);JSONArray days=p.optJSONArray("days");for(int d=0;d<days.length();d++){JSONArray groups=days.optJSONObject(d).optJSONArray("groups");for(int g=0;g<groups.length();g++){english(ZlatIntermediate.cue(groups.optJSONObject(g)));english(ZlatAdvanced.cue(groups.optJSONObject(g)));}}}
 }
 @Test public void legacyMigrationPreservesVotesNotesWeightsAndSessionState()throws Exception{
  JSONObject p=asset("sheiko.json");p.remove("routine");Engine.put(p,"id","legacy-phone-id");Engine.put(p,"description","Подготовительный цикл · 4 недели · Б. И. Шейко");Engine.put(p,"name","Шейко · КМС / МС");Engine.put(p,"nextDay",2);PlanPopularity.identity(p);
  JSONObject day=p.optJSONArray("days").optJSONObject(2);Engine.put(day,"name","Неделя 1 · День 3");JSONObject group=day.optJSONArray("groups").optJSONObject(0);Engine.put(group,"exercise","Жим лёжа");
  Engine.put(p,"exerciseNotes",Engine.obj("{\"Жим лёжа\":\"Моя заметка, оставить 100 кг\"}"));
  JSONObject s=Engine.start(p,12345);JSONObject first=s.optJSONArray("sets").optJSONObject(0);Engine.put(first,"kg",101.25);Engine.put(first,"done",true);Engine.put(first,"manualWeight",true);Engine.put(s,"revision",9);
  JSONObject data=Engine.obj("{\"programs\":[],\"history\":[],\"savedSessions\":{}}");data.optJSONArray("programs").put(p);Engine.put(data,"active",s);Engine.put(data.optJSONObject("savedSessions"),"legacy-phone-id",Engine.copy(s));data.optJSONArray("history").put(Engine.copy(s));
  String vote=PlanPopularity.key(p),sessionId=s.optString("id"),maxima=p.optJSONObject("maxima").toString();
  EnglishPlans.migrate(data);
  assertEquals("Sheiko · Preparation",p.optString("name"));assertEquals("sheiko-cms-ms",PlanIdentity.routine(p));assertEquals(vote,PlanPopularity.key(p));assertEquals(maxima,p.optJSONObject("maxima").toString());assertEquals(2,p.optInt("nextDay"));
  for(JSONObject session:new JSONObject[]{s,data.optJSONObject("savedSessions").optJSONObject("legacy-phone-id"),data.optJSONArray("history").optJSONObject(0)}){
   assertEquals(sessionId,session.optString("id"));assertEquals(9,session.optInt("revision"));assertEquals(12345,session.optLong("started"));assertEquals("Week 1 · Day 3",session.optString("dayName"));JSONObject set=session.optJSONArray("sets").optJSONObject(0);assertEquals("Bench press",set.optString("exercise"));assertEquals(101.25,set.optDouble("kg"),0);assertTrue(set.optBoolean("done"));assertTrue(set.optBoolean("manualWeight"));assertEquals("Моя заметка, оставить 100 кг",ExerciseNotes.get(session,"Bench press"));
  }
  String once=data.toString();EnglishPlans.migrate(data);assertEquals(once,data.toString());
  JSONObject fresh=asset("sheiko.json");assertEquals(1,CommunityCatalog.merge(Arrays.asList(p),Arrays.asList(fresh)).size());
 }
 @Test public void privateCustomPlansStayUntouchedAndNoteCollisionsKeepBoth(){
  JSONObject p=Engine.obj("{\"id\":\"custom\",\"createdLocally\":true,\"name\":\"Мой план\",\"days\":[]}");String before=p.toString();EnglishPlans.plan(p);assertEquals(before,p.toString());
  JSONObject owner=Engine.obj("{\"exerciseNotes\":{\"Жим лёжа\":\"первая\",\"Bench press\":\"вторая\"}}");EnglishPlans.notes(owner);assertEquals(2,owner.optJSONObject("exerciseNotes").length());
 }
 @Test public void muscleAndBarbellDetectionSurviveTranslation()throws Exception{
  for(String file:new String[]{"sheiko.json","sheiko_competition.json"}){JSONObject p=asset(file);JSONArray days=p.optJSONArray("days");for(int d=0;d<days.length();d++){JSONArray groups=days.optJSONObject(d).optJSONArray("groups");for(int g=0;g<groups.length();g++){JSONObject translated=groups.optJSONObject(g),old=Engine.copy(translated);Engine.put(old,"exercise",EnglishPlanText.original(translated.optString("exercise")));assertEquals(Muscles.of(old),Muscles.of(translated));old.remove("barbell");translated.remove("barbell");SheikoBarbells.mark(new JSONArray().put(old).put(translated));assertEquals(old.optBoolean("barbell"),translated.optBoolean("barbell"));}}}
 }
}
