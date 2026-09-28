package com.podhod.app;
import org.junit.Test;
import static org.junit.Assert.*;
public class DipBeltTest {
 @Test public void addedWeightIsNotSplitAndHasNoBar(){assertArrayEquals(new int[]{1,0,1,0,1},DipBelt.plates(26.25));assertEquals(26.25,DipBelt.assembled(DipBelt.plates(26.25)),.001);assertArrayEquals(new int[]{2,0,0,0,0},DipBelt.plates(40));assertEquals(0,DipBelt.assembled(DipBelt.plates(0)),.001);assertEquals(20,DipBelt.assembled(DipBelt.plates(20.5)),.001);}
 @Test public void allZlatLevelsAndOptOut(){for(String key:new String[]{"zlatLift","intermediateLift","advancedLift"})assertTrue(DipBelt.enabled(Engine.obj("{\""+key+"\":\"dip\"}")));assertFalse(DipBelt.enabled(Engine.obj("{\"zlatLift\":\"dip\",\"dipBelt\":false}")));assertFalse(DipBelt.enabled(Engine.obj("{\"barbell\":true,\"zlatLift\":\"dip\"}")));assertTrue(DipBelt.enabled(Engine.obj("{\"dipBelt\":true}")));}
}
