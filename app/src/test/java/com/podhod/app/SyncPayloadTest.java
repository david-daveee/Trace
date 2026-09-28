package com.podhod.app;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class SyncPayloadTest {
    @Test public void newPhoneReceivesActiveAndPausedSessionsFromSelectedCloud() throws Exception {
        JSONObject source=backup("cloud","a"),phone=backup("phone","b");
        JSONObject workouts=source.getJSONObject("workouts");
        workouts.getJSONObject("active").put("cursor",3).put("exerciseNotes",new JSONObject().put("dip","keep weight"));
        workouts.getJSONObject("savedSessions").put("paused",new JSONObject().put("cursor",7));
        JSONObject portable=SyncPayload.portable(source);
        CloudSnapshot encoded=CloudSnapshot.encode(portable);
        JSONObject transferred=CloudSnapshot.decode(encoded.chunks,encoded.sha256,encoded.bytes);
        JSONObject restored=SyncPayload.choose(SyncPayload.portable(phone),transferred,false);
        assertEquals(3,restored.getJSONObject("workouts").getJSONObject("active").getInt("cursor"));
        assertEquals("keep weight",restored.getJSONObject("workouts").getJSONObject("active").getJSONObject("exerciseNotes").getString("dip"));
        assertEquals(7,restored.getJSONObject("workouts").getJSONObject("savedSessions").getJSONObject("paused").getInt("cursor"));
        assertEquals(2,restored.getJSONObject("walks").getJSONArray("history").length());
        restored.getJSONObject("workouts").getJSONObject("active").put("cursor",99);
        assertEquals(3,workouts.getJSONObject("active").getInt("cursor"));
    }
    @Test public void setProgressChangesFingerprintAndPhoneChoiceKeepsIt() throws Exception {
        JSONObject phone=SyncPayload.portable(backup("phone","a"));
        JSONObject cloud=Engine.copy(phone);String original=SyncPayload.fingerprint(phone);
        phone.getJSONObject("workouts").getJSONObject("active").put("cursor",2);
        assertNotEquals(original,SyncPayload.fingerprint(phone));
        assertEquals(2,SyncPayload.choose(phone,cloud,true).getJSONObject("workouts").getJSONObject("active").getInt("cursor"));
        assertFalse(SyncPayload.choose(phone,cloud,false).getJSONObject("workouts").getJSONObject("active").has("cursor"));
    }
    private JSONObject backup(String name, String walk) throws Exception {
        return new JSONObject("{\"createdAt\":1,\"workouts\":{\"programs\":[\""+name+"\"],\"active\":{},\"savedSessions\":{}},\"steps\":{\"days\":{\"2026-09-28\":100},\"enabled\":true},\"settings\":{\"language\":\"en\",\"lastBackupAt\":1},\"walks\":{\"active\":{},\"history\":[{\"id\":\""+walk+"\",\"started\":1,\"ended\":2,\"points\":[]}]}}");
    }
    @Test public void portableExcludesDeviceStateWithoutMutatingBackup() throws Exception {
        JSONObject original=backup("phone","a"), p=SyncPayload.portable(original);
        assertFalse(p.has("createdAt"));
        assertTrue(p.getJSONObject("workouts").has("active"));
        assertTrue(p.getJSONObject("workouts").has("savedSessions"));
        assertFalse(p.getJSONObject("walks").has("active"));
        assertFalse(p.getJSONObject("steps").has("enabled"));
        assertFalse(p.getJSONObject("settings").has("lastBackupAt"));
        assertTrue(original.getJSONObject("workouts").has("active"));
    }
    @Test public void eitherChoiceMergesWalksButDoesNotSumSteps() throws Exception {
        JSONObject phone=SyncPayload.portable(backup("phone","a")), cloud=SyncPayload.portable(backup("cloud","b"));
        cloud.getJSONObject("steps").getJSONObject("days").put("2026-09-28",200);
        for(boolean usePhone:new boolean[]{false,true}) {
            JSONObject merged=SyncPayload.choose(phone,cloud,usePhone);
            assertEquals(usePhone?"phone":"cloud",merged.getJSONObject("workouts").getJSONArray("programs").getString(0));
            assertEquals(usePhone?100:200,merged.getJSONObject("steps").getJSONObject("days").getInt("2026-09-28"));
            assertEquals(2,merged.getJSONObject("walks").getJSONArray("history").length());
            assertEquals(SyncPayload.fingerprint(merged),SyncPayload.fingerprint(SyncPayload.choose(merged,cloud,true)));
        }
    }
    @Test public void deletedWalkCannotReturnFromOfflinePhone() throws Exception {
        JSONObject phone=SyncPayload.portable(backup("phone","a")), cloud=SyncPayload.portable(backup("cloud","b"));
        cloud.getJSONObject("walks").put("deletedIds",new JSONArray().put("a"));
        JSONObject merged=SyncPayload.choose(phone,cloud,true);
        assertEquals(1,merged.getJSONObject("walks").getJSONArray("history").length());
        assertEquals("b",merged.getJSONObject("walks").getJSONArray("history").getJSONObject(0).getString("id"));
    }
}
