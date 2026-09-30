package com.podhod.app;
import org.junit.Test;
import static org.junit.Assert.*;
import org.json.*;
import java.nio.file.*;
public class EngineTest {
    JSONObject source()throws Exception{return Engine.obj(new String(Files.readAllBytes(Paths.get("src/main/assets/sheiko.json")),java.nio.charset.StandardCharsets.UTF_8));}
    @Test public void sourceFormulasAndCompleteness()throws Exception{
        JSONObject p=source();Engine.validateProgram(p);assertEquals(16,p.getJSONArray("days").length());int n=0,groups=0,formulas=0;
        for(int d=0;d<16;d++)for(int i=0;i<p.getJSONArray("days").getJSONObject(d).getJSONArray("groups").length();i++){
            JSONObject g=p.getJSONArray("days").getJSONObject(d).getJSONArray("groups").getJSONObject(i);groups++;n+=g.getInt("sets");
            if(g.has("factor")){formulas++;assertEquals(g.getDouble("kg"),Engine.weight(g,p.getJSONObject("maxima"),2.5),.00001);}
        }
        assertEquals(565,n);assertEquals(217,groups);assertEquals(178,formulas);
    }
    @Test public void snapshotKeepsHistoricalWeightAndRepeatedBlocks()throws Exception{
        JSONObject p=source();JSONObject s=Engine.start(p,1000);assertEquals(36,s.getJSONArray("sets").length());
        double before=s.getJSONArray("sets").getJSONObject(0).getDouble("kg");p.getJSONObject("maxima").put("squat",200);
        assertEquals(before,s.getJSONArray("sets").getJSONObject(0).getDouble("kg"),0);
        assertEquals(100,Engine.start(p,2000).getJSONArray("sets").getJSONObject(0).getDouble("kg"),0);
        assertEquals("Squat",s.getJSONArray("sets").getJSONObject(0).getString("exercise"));
    }
    @Test public void eachTapCompletesOneSetWithoutRestAndUndoWorks()throws Exception{
        JSONObject s=Engine.start(source(),1000);assertTrue(Engine.act(s,"next",2000));assertEquals(1,s.getInt("cursor"));assertFalse(s.optBoolean("rest"));
        assertTrue(Engine.act(s,"next",3000));assertEquals(2,s.getInt("cursor"));assertEquals(2,Engine.doneCount(s));assertFalse(s.has("restUntil"));
        Engine.act(s,"undo",4000);assertEquals(1,s.getInt("cursor"));assertEquals(1,Engine.doneCount(s));
    }
    @Test public void manualPauseRejectsCompletionAndResumeKeepsCursor()throws Exception{
        JSONObject s=Engine.start(source(),1000);Engine.act(s,"next",2000);Engine.act(s,"pause",3000);
        assertFalse(Engine.act(s,"next",4000));assertEquals(1,s.getInt("cursor"));Engine.act(s,"pause",5000);
        assertTrue(Engine.act(s,"next",6000));assertEquals(2,s.getInt("cursor"));assertFalse(s.has("restUntil"));
    }
    @Test public void oldRestIsRemovedWithoutLosingMarksOrManualPause()throws Exception{
        JSONObject s=Engine.start(source(),1000);Engine.act(s,"next",2000);Engine.put(s,"rest",true);Engine.put(s,"restUntil",50000);Engine.put(s,"remaining",12000);Engine.put(s,"paused",true);
        int revision=s.optInt("revision");Engine.clearRest(s);assertEquals(1,Engine.doneCount(s));assertEquals(1,s.optInt("cursor"));assertTrue(s.optBoolean("paused"));assertFalse(s.optBoolean("rest"));assertFalse(s.has("remaining"));assertFalse(s.has("restUntil"));assertEquals(revision+1,s.optInt("revision"));
        Engine.clearRest(s);assertEquals(revision+1,s.optInt("revision"));
    }
    @Test public void fullDayStopsAtEndAndUndoRemainsAvailable()throws Exception{
        JSONObject s=Engine.start(source(),0);JSONArray sets=s.getJSONArray("sets");for(int i=0;i<sets.length();i++)if(sets.getJSONObject(i).getDouble("kg")<0)sets.getJSONObject(i).put("kg",0);
        for(int i=0;i<sets.length();i++){if(s.optBoolean("rest"))Engine.act(s,"next",1000+i);assertTrue(Engine.act(s,"next",2000+i));}
        assertEquals(sets.length(),s.getInt("cursor"));assertFalse(Engine.act(s,"next",9999));assertTrue(Engine.act(s,"undo",10000));assertEquals(sets.length()-1,s.getInt("cursor"));
    }
    @Test public void missingAccessoryWeightMustBeChosen()throws Exception{
        JSONObject s=Engine.start(source(),0);s.getJSONArray("sets").getJSONObject(0).put("kg",-1);assertFalse(Engine.act(s,"next",1));assertEquals(0,s.getInt("cursor"));
        s.getJSONArray("sets").getJSONObject(0).put("kg",0);assertTrue(Engine.act(s,"next",2));
    }
    @Test public void csvRoundtripAndValidation()throws Exception{
        JSONObject p=Importer.parse("день;упражнение;подходы;повторения;вес\n1;Присед;3;5;77,5\n2;Пресс;3;10;\n".getBytes(java.nio.charset.StandardCharsets.UTF_8),"test.csv");
        assertEquals(2,p.getJSONArray("days").length());assertEquals(77.5,Engine.start(p,0).getJSONArray("sets").getJSONObject(0).getDouble("kg"),0);
        assertThrows(IllegalArgumentException.class,()->Importer.parse("day;exercise;sets;reps;kg\n1;Squat;0;5;60".getBytes(),"bad.csv"));
    }
    @Test public void maximumEditRecalculatesAllDaysAndRemainingActiveSets()throws Exception{
        JSONObject p=source();p.put("id","program");JSONObject s=Engine.start(p,0);
        Engine.act(s,"next",1);String done=s.getJSONArray("sets").getJSONObject(0).toString();
        Engine.updateMaxima(p,Engine.obj("{\"squat\":200,\"bench\":150,\"deadlift\":220}"),s);
        assertEquals(done,s.getJSONArray("sets").getJSONObject(0).toString());
        assertEquals(120,s.getJSONArray("sets").getJSONObject(1).getDouble("kg"),0);
        assertEquals(75,s.getJSONArray("sets").getJSONObject(11).getDouble("kg"),0);
        for(int d=0;d<16;d++){
            p.put("nextDay",d);JSONObject next=Engine.start(p,10);
            JSONArray sets=next.getJSONArray("sets");
            for(int i=0;i<sets.length();i++){JSONObject g=sets.getJSONObject(i);if(g.has("factor"))assertEquals(Engine.weight(g,p.getJSONObject("maxima"),2.5),g.getDouble("kg"),.00001);}
        }
        p.put("nextDay",1);assertEquals(110,Engine.start(p,10).getJSONArray("sets").getJSONObject(0).getDouble("kg"),0);
        assertEquals(2,s.getInt("revision"));
    }
    @Test public void maximaKeepOverridesAndOtherActiveProgramsUntouched()throws Exception{
        JSONObject p=source();p.put("id","program");JSONObject s=Engine.start(p,0);
        s.getJSONArray("sets").getJSONObject(0).put("kg",85).put("manualWeight",true);
        s.getJSONArray("sets").getJSONObject(1).put("kg",101); // v0.1 override without flag
        Engine.updateMaxima(p,Engine.obj("{\"squat\":200,\"bench\":150,\"deadlift\":220}"),s);
        assertEquals(85,s.getJSONArray("sets").getJSONObject(0).getDouble("kg"),0);
        assertEquals(101,s.getJSONArray("sets").getJSONObject(1).getDouble("kg"),0);
        assertTrue(s.getJSONArray("sets").getJSONObject(1).getBoolean("manualWeight"));
        assertEquals(120,s.getJSONArray("sets").getJSONObject(2).getDouble("kg"),0);
        s.put("programId","other");String before=s.toString();Engine.updateMaxima(p,Engine.obj("{\"squat\":220,\"bench\":160,\"deadlift\":230}"),s);assertEquals(before,s.toString());
    }
    @Test public void invalidMaximaDoNotPartiallyMutateProgramOrSession()throws Exception{
        JSONObject p=source();JSONObject s=Engine.start(p,0);String before=p.toString(),session=s.toString();
        assertThrows(IllegalArgumentException.class,()->Engine.updateMaxima(p,Engine.obj("{\"squat\":0,\"bench\":150,\"deadlift\":220}"),s));
        assertEquals(before,p.toString());assertEquals(session,s.toString());
    }
    @Test public void independentMarksPreserveOthersAndNextSkipsCompleted()throws Exception{
        JSONObject s=Engine.start(source(),0);JSONArray sets=s.getJSONArray("sets");
        assertTrue(Engine.setDone(s,2,true,10));assertEquals(0,s.getInt("cursor"));assertEquals(1,Engine.doneCount(s));
        Engine.act(s,"next",11);Engine.act(s,"next",12);
        assertEquals(3,s.getInt("cursor"));assertEquals(3,Engine.doneCount(s));
        String second=sets.getJSONObject(1).toString(),third=sets.getJSONObject(2).toString();
        Engine.setDone(s,0,false,20);assertEquals(0,s.getInt("cursor"));assertEquals(2,Engine.doneCount(s));
        assertEquals(second,sets.getJSONObject(1).toString());assertEquals(third,sets.getJSONObject(2).toString());assertFalse(s.getBoolean("rest"));
        Engine.setDone(s,0,true,21);assertEquals(3,s.getInt("cursor"));assertEquals(3,Engine.doneCount(s));
        JSONObject restored=Engine.copy(s);assertEquals(3,Engine.doneCount(restored));assertEquals(3,Engine.firstUndone(restored));
    }
    @Test public void undoUsesCompletionOrderNotPosition()throws Exception{
        JSONObject s=Engine.start(source(),0);Engine.setDone(s,5,true,10);Engine.setDone(s,1,true,10);
        Engine.act(s,"undo",11);assertFalse(s.getJSONArray("sets").getJSONObject(1).getBoolean("done"));assertTrue(s.getJSONArray("sets").getJSONObject(5).getBoolean("done"));
        Engine.act(s,"undo",12);assertEquals(0,Engine.doneCount(s));assertFalse(Engine.act(s,"undo",13));
    }
    @Test public void anySetCanBeReopenedAfterAllSetsCompleted()throws Exception{
        JSONObject s=Engine.start(source(),0);JSONArray a=s.getJSONArray("sets");
        for(int i=a.length()-1;i>=0;i--){if(a.getJSONObject(i).getDouble("kg")<0)a.getJSONObject(i).put("kg",0);assertTrue(Engine.setDone(s,i,true,i));}
        assertEquals(a.length(),s.getInt("cursor"));Engine.setDone(s,4,false,100);assertEquals(4,s.getInt("cursor"));assertEquals(a.length()-1,Engine.doneCount(s));assertTrue(a.getJSONObject(5).getBoolean("done"));
        Engine.act(s,"next",101);assertEquals(a.length(),s.getInt("cursor"));assertEquals(a.length(),Engine.doneCount(s));
        int revision=s.getInt("revision");assertFalse(Engine.setDone(s,4,true,102));assertEquals(revision,s.getInt("revision"));
    }
    @Test public void recalculationPreservesCompletedSetsAheadOfCurrent()throws Exception{
        JSONObject p=source();p.put("id","p");JSONObject s=Engine.start(p,0);Engine.setDone(s,4,true,10);double actual=s.getJSONArray("sets").getJSONObject(4).getDouble("kg");
        Engine.updateMaxima(p,Engine.obj("{\"squat\":200,\"bench\":150,\"deadlift\":220}"),s);
        assertEquals(actual,s.getJSONArray("sets").getJSONObject(4).getDouble("kg"),0);assertEquals(100,s.getJSONArray("sets").getJSONObject(0).getDouble("kg"),0);
    }
}
