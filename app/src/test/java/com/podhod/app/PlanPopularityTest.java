package com.podhod.app;
import org.junit.Test;
import org.json.*;
import static org.junit.Assert.*;
public class PlanPopularityTest {
 @Test public void builtinVotesIgnoreLocalIdAndWeights()throws Exception{JSONObject a=new JSONObject().put("routine","sheiko-cms-ms").put("id","a"),b=Engine.copy(a);b.put("id","b").put("maxima",new JSONObject().put("bench",100));assertEquals(PlanPopularity.key(a),PlanPopularity.key(b));}
 @Test public void sharedCustomKeepsIdentity()throws Exception{JSONObject a=new JSONObject().put("id","original");PlanPopularity.identity(a);JSONObject b=PlanShare.snapshot(a);b.put("id","imported");assertEquals(PlanPopularity.key(a),PlanPopularity.key(b));}
 @Test public void separateCustomPlansDoNotShareVotes()throws Exception{assertNotEquals(PlanPopularity.key(new JSONObject().put("id","a")),PlanPopularity.key(new JSONObject().put("id","b")));}
}
