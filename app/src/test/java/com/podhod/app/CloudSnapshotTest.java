package com.podhod.app;
import org.json.*;import org.junit.Test;import java.util.*;import static org.junit.Assert.*;
public class CloudSnapshotTest {
 @Test public void unicodeRoundTrip()throws Exception{JSONObject input=Engine.obj("{\"name\":\"Прогулка · Trace\",\"walks\":[]}");CloudSnapshot s=CloudSnapshot.encode(input);assertEquals(input.toString(),CloudSnapshot.decode(s.chunks,s.sha256,s.bytes).toString());}
 @Test public void largeIncompressiblePayloadUsesMultipleChunks()throws Exception{byte[] random=new byte[700000];new Random(123).nextBytes(random);JSONObject input=new JSONObject().put("cover",Base64.getEncoder().encodeToString(random));CloudSnapshot s=CloudSnapshot.encode(input);assertTrue(s.chunks.size()>1);assertEquals(input.toString(),CloudSnapshot.decode(s.chunks,s.sha256,s.bytes).toString());}
 @Test(expected=java.io.IOException.class) public void rejectsChangedPayload()throws Exception{CloudSnapshot s=CloudSnapshot.encode(Engine.obj("{\"a\":1}"));s.chunks.get(0)[0]^=1;CloudSnapshot.decode(s.chunks,s.sha256,s.bytes);}
 @Test(expected=java.io.IOException.class) public void rejectsMissingChunk()throws Exception{CloudSnapshot s=CloudSnapshot.encode(Engine.obj("{\"a\":1}"));CloudSnapshot.decode(Collections.emptyList(),s.sha256,s.bytes);}
 @Test(expected=java.io.IOException.class) public void rejectsOversizedManifest()throws Exception{CloudSnapshot.decode(Collections.emptyList(),"",CloudSnapshot.LIMIT+1);}
}
