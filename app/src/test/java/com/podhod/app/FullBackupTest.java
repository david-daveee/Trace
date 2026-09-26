package com.podhod.app;
import org.junit.Test;import org.json.*;import static org.junit.Assert.*;
public class FullBackupTest {
 JSONObject fixture(){return Engine.obj("{\"format\":\"trace-backup\",\"version\":2,\"workouts\":{\"programs\":[],\"history\":[]},\"steps\":{\"days\":{\"2026-09-26\":1234},\"goal\":8000,\"stepLengthCm\":70,\"enabled\":true},\"settings\":{\"language\":\"en\"}}");}
 @Test public void oldBackupRemainsCompatible(){JSONObject old=Engine.obj("{\"programs\":[],\"history\":[]}");assertSame(old,FullBackup.workouts(old));}
 @Test public void fullBackupIncludesWorkoutState(){JSONObject b=fixture();assertSame(b.optJSONObject("workouts"),FullBackup.workouts(b));}
 @Test public void invalidStepsRejected(){JSONObject b=fixture();Engine.put(b.optJSONObject("steps").optJSONObject("days"),"2026-09-26",-1);try{FullBackup.workouts(b);fail();}catch(IllegalArgumentException expected){}}
 @Test public void unsupportedVersionRejected(){JSONObject b=fixture();Engine.put(b,"version",99);try{FullBackup.workouts(b);fail();}catch(IllegalArgumentException expected){}}
}
