package com.podhod.app;
import org.junit.Test;import org.json.*;import static org.junit.Assert.*;
public class WalkSpeedTest {
 JSONArray point(double lat,long t,boolean gap){return new JSONArray().put((Object)lat).put(0).put(t).put(5).put(gap);}
 @Test public void speedUsesDistanceAndTimeAndSmoothsChanges(){JSONArray p=new JSONArray().put(point(0,1000,false)).put(point(.0001,11000,false)).put(point(.0004,21000,false));double[] s=WalkSpeed.speeds(p);assertEquals(1.112,s[1],.002);assertTrue(s[2]>s[1]);assertTrue(s[2]<3.336);}
 @Test public void gapResetsSmoothing(){JSONArray p=new JSONArray().put(point(0,1000,false)).put(point(.0001,11000,false)).put(point(1,200000,true)).put(point(1.0003,210000,false));double[] s=WalkSpeed.speeds(p);assertTrue(Double.isNaN(s[2]));assertEquals(3.336,s[3],.002);}
 @Test public void missingTimeAndLongGapsStayUnknown(){JSONArray p=new JSONArray().put(point(0,0,false)).put(point(.0001,10000,false)).put(point(.0002,200000,false));for(double v:WalkSpeed.speeds(p))assertTrue(Double.isNaN(v));assertEquals(WalkSpeed.UNKNOWN,WalkSpeed.color(Double.NaN));}
 @Test public void colorsUseStableScale(){assertEquals(0xFF27B9CD,WalkSpeed.color(0));assertEquals(0xFFF2BD56,WalkSpeed.color(6/3.6));assertEquals(0xFFF06462,WalkSpeed.color(12/3.6));assertEquals(WalkSpeed.color(12/3.6),WalkSpeed.color(10));}
}
