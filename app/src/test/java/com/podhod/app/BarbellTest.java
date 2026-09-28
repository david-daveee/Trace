package com.podhod.app;
import org.junit.Test;
import static org.junit.Assert.*;
public class BarbellTest {
 @Test public void hundredUsesTwoTwentiesPerSide(){assertArrayEquals(new int[]{2,0,0,0,0},Barbell.plates(100));assertEquals(100,Barbell.assembled(Barbell.plates(100)),.001);}
 @Test public void mixedPlatesAndEmptyBar(){assertArrayEquals(new int[]{1,1,1,1,1},Barbell.plates(97.5));assertArrayEquals(new int[5],Barbell.plates(20));assertEquals(100,Barbell.assembled(Barbell.plates(101.25)),.001);}
}
