package com.podhod.app;

import org.json.*;
import org.junit.Test;
import java.nio.file.*;
import static org.junit.Assert.*;

public class SheikoCycleTest {
    JSONObject plan() throws Exception {
        Path path=Path.of("src/main/assets/sheiko_competition.json");
        if(!Files.exists(path))path=Path.of("app").resolve(path);
        return Engine.obj(new String(Files.readAllBytes(path),java.nio.charset.StandardCharsets.UTF_8));
    }
    @Test public void completeCyclePreservesDetailedCellsAndRounding() throws Exception {
        JSONObject p=plan();Engine.validateProgram(p);
        assertEquals(35,p.getJSONArray("days").length());
        int sets=0,weighted=0;
        for(int d=0;d<35;d++){
            JSONObject day=p.getJSONArray("days").getJSONObject(d);
            assertEquals(d+1,day.getInt("sourceNumber"));
            Engine.put(p,"nextDay",d);
            JSONArray expanded=Engine.start(p,1).getJSONArray("sets");sets+=expanded.length();
            for(int i=0;i<expanded.length();i++){
                JSONObject s=expanded.getJSONObject(i);
                if(s.has("factor")){
                    weighted++;
                    assertEquals(.5,s.getDouble("round"),0);
                    assertEquals(s.getDouble("kg"),Engine.weight(s,p.getJSONObject("maxima"),2.5),.000001);
                }else assertEquals(-1,s.getDouble("kg"),0);
            }
        }
        assertEquals(1251,sets);assertEquals(832,weighted);
        JSONObject first=p.getJSONArray("days").getJSONObject(0).getJSONArray("groups").getJSONObject(0);
        assertEquals(75,first.getDouble("kg"),0);
        assertEquals(50.5,Engine.weight(first,Engine.obj("{\"bench\":101}"),2.5),0);
        assertTrue(p.getString("terminalNote").contains("#36"));
        Engine.put(p,"nextDay",35);assertThrows(IllegalArgumentException.class,()->Engine.start(p,1));
        boolean found=false;
        JSONArray g=p.getJSONArray("days").getJSONObject(33).getJSONArray("groups");
        for(int i=0;i<g.length();i++){JSONObject x=g.getJSONObject(i);if(x.optString("lift").equals("deadlift")&&x.optDouble("factor")==.6){found=true;assertEquals(2,x.getInt("reps"));assertEquals(2,x.getInt("sets"));}}
        assertTrue(found);
    }
    @Test public void addingCatalogEntryPreservesActiveHistoryAndUserEdits() throws Exception {
        JSONObject data=Engine.obj("{\"programs\":[],\"history\":[{\"id\":\"existing\"}],\"active\":{\"id\":\"current\"}}");
        assertTrue(BuiltinPlans.ensure(data,plan()));
        JSONObject p=data.getJSONArray("programs").getJSONObject(0);
        assertFalse(p.getBoolean("mine"));
        Engine.put(p,"mine",true);Engine.put(p,"nextDay",9);Engine.put(p,"maxima",Engine.obj("{\"squat\":100,\"bench\":80,\"deadlift\":120}"));
        String before=data.toString();assertFalse(BuiltinPlans.ensure(data,plan()));assertEquals(before,data.toString());
        assertEquals("current",data.getJSONObject("active").getString("id"));assertEquals("existing",data.getJSONArray("history").getJSONObject(0).getString("id"));
        Engine.put(p,"nextDay",0);JSONObject s=Engine.start(p,1);
        Engine.setDone(s,0,true,2);Engine.updateMaxima(p,Engine.obj("{\"squat\":150,\"bench\":101,\"deadlift\":190}"),s);
        assertEquals(40,s.getJSONArray("sets").getJSONObject(0).getDouble("kg"),0);
        assertEquals(60.5,s.getJSONArray("sets").getJSONObject(1).getDouble("kg"),0);
    }
}
