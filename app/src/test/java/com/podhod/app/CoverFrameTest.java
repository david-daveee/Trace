package com.podhod.app;
import org.junit.Test;
import static org.junit.Assert.*;
public class CoverFrameTest {
 @Test public void dragStopsAtEdgesWithoutExposingBlankSpace(){CoverFrame f=new CoverFrame(.5f,.5f,2);f.drag(9999,-9999,1000,500,400,200);float[] b=f.bounds(1000,500,400,200);assertEquals(0,b[0],.001);assertEquals(-200,b[1],.001);assertTrue(b[0]+b[2]>=400);assertTrue(b[1]+b[3]>=200);}
 @Test public void pinchKeepsImagePointUnderFinger(){CoverFrame f=new CoverFrame(.5f,.5f,1);f.scaleTo(2,100,60,800,400,400,200);float[] b=f.bounds(800,400,400,200);assertEquals(100,b[0]+.25f*b[2],.001);assertEquals(60,b[1]+.3f*b[3],.001);}
 @Test public void differentAspectRatiosRemainCovered(){CoverFrame f=new CoverFrame(.9f,.1f,3);for(float h:new float[]{100,200,500}){float[] b=f.bounds(600,900,400,h);assertTrue(b[0]<=0&&b[1]<=0);assertTrue(b[0]+b[2]>=399.999);assertTrue(b[1]+b[3]>=h-.001);}}
 @Test public void invalidAndExcessiveStateIsClamped(){CoverFrame f=new CoverFrame(Float.NaN,8,99);assertEquals(0,f.x,0);assertEquals(1,f.y,0);assertEquals(4,f.zoom,0);f.scaleTo(.1f,100,50,800,400,400,200);assertEquals(1,f.zoom,0);}
}
