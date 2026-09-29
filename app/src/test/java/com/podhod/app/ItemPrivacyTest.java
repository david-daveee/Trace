package com.podhod.app;
import org.junit.Test;import java.util.*;import static org.junit.Assert.*;
public class ItemPrivacyTest {
 Map<String,Object> item(boolean visible,String audience,boolean individual){Map<String,Object> m=new HashMap<>();m.put("kind","walk");m.put("visible",visible);m.put("audience",audience);m.put("individualPrivacy",individual);return m;}
 @Test public void newWalkIsPrivate(){assertEquals(0,ItemPrivacy.visibility(null,Map.of("walksVisible",true)));}
 @Test public void offIsAlwaysPrivate(){assertEquals(0,ItemPrivacy.visibility(item(false,"friends",true),Map.of("walksVisible",true)));}
 @Test public void explicitEyeOpensOnlyThisItemDespiteLegacyCategory(){assertEquals(1,ItemPrivacy.visibility(item(true,"friends",true),Map.of("walksVisible",false)));assertEquals(0,ItemPrivacy.visibility(item(true,"friends",false),Map.of("walksVisible",false)));}
 @Test public void legacyDirectIsNotMisrepresentedAsAllFriends(){assertEquals(2,ItemPrivacy.visibility(item(true,"direct",false),Map.of("walksVisible",true)));}
 @Test public void legacyHiddenCategoryStaysPrivate(){assertEquals(0,ItemPrivacy.visibility(item(true,"friends",false),Collections.emptyMap()));}
}
