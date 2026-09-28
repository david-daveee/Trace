package com.podhod.app;
import org.json.*;import org.junit.Test;import java.util.*;import static org.junit.Assert.*;
public class SyncMergeTest {
 JSONObject walk(String id,long started,long ended){JSONObject w=Engine.obj("{\"points\":[],\"meters\":0}");Engine.put(w,"id",id);Engine.put(w,"started",started);Engine.put(w,"ended",ended);return w;}
 @Test public void mergesWithoutDuplicatesAndDoesNotMutateSources(){JSONArray a=new JSONArray().put(walk("a",1,2)),b=new JSONArray().put(walk("a",1,2)).put(walk("b",3,4));String original=a.toString();JSONArray merged=SyncMerge.walks(a,b,Collections.emptySet());assertEquals(2,merged.length());assertEquals(original,a.toString());assertEquals(merged.toString(),SyncMerge.walks(b,a,Collections.emptySet()).toString());assertEquals(merged.toString(),SyncMerge.walks(merged,b,Collections.emptySet()).toString());}
 @Test public void deletedWalkDoesNotReappear(){JSONArray a=new JSONArray().put(walk("a",1,2));assertEquals(0,SyncMerge.walks(a,a,Collections.singleton("a")).length());}
 @Test public void moreRecentCompletionWins(){JSONArray a=new JSONArray().put(walk("a",1,2)),b=new JSONArray().put(walk("a",1,3));assertEquals(3,SyncMerge.walks(a,b,Collections.emptySet()).optJSONObject(0).optLong("ended"));}
 @Test public void legacyWalkHasStableIdentity(){JSONObject w=walk("",1,2);w.remove("id");JSONArray a=new JSONArray().put(w);JSONArray merged=SyncMerge.walks(a,a,Collections.emptySet());assertEquals(1,merged.length());assertTrue(merged.optJSONObject(0).optString("id").startsWith("legacy-"));assertEquals(1,SyncMerge.walks(merged,a,Collections.emptySet()).length());}
 @Test(expected=IllegalArgumentException.class) public void liveRecordingIsNotTransferred(){JSONObject w=walk("a",1,2);w.remove("ended");SyncMerge.walks(new JSONArray().put(w),null,Collections.emptySet());}
}
