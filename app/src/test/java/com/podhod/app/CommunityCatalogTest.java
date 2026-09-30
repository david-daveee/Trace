package com.podhod.app;
import org.junit.Test;import org.json.*;import java.util.*;import static org.junit.Assert.*;
public class CommunityCatalogTest {
 JSONObject local(String id){JSONObject p=new JSONObject();Engine.put(p,"id",id);Engine.put(p,"name","Same title");PlanPopularity.identity(p);return p;}
 @Test public void approvedOriginalAppearsOnceAndPreservesLocalState(){JSONObject p=local("original");Engine.put(p,"mine",true);Engine.put(p,"nextDay",4);JSONObject remote=Engine.copy(p);Engine.put(remote,"id","community");Engine.put(remote,"catalogId",PlanPopularity.key(p));Engine.put(remote,"nextDay",0);List<JSONObject> result=CommunityCatalog.merge(Arrays.asList(p),Arrays.asList(remote));assertEquals(1,result.size());assertSame(p,result.get(0));assertEquals(4,p.optInt("nextDay"));assertTrue(p.optBoolean("mine"));}
 @Test public void distinctPlansWithSameTitleStaySeparate(){assertEquals(2,CommunityCatalog.merge(Arrays.asList(local("a")),Arrays.asList(local("b"))).size());}
 @Test public void importedCopyMatchesPublishedIdentity(){JSONObject p=local("source");JSONObject imported=Engine.copy(p);Engine.put(imported,"id","imported");JSONObject remote=Engine.copy(p);Engine.put(remote,"catalogId",PlanPopularity.key(p));assertSame(imported,CommunityCatalog.merge(Arrays.asList(imported),Arrays.asList(remote)).get(0));assertEquals(1,CommunityCatalog.merge(Arrays.asList(imported),Arrays.asList(remote)).size());}
}
