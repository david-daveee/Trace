package com.podhod.app;
import org.junit.Test;import static org.junit.Assert.*;
public class AutoBackupTest {
 @Test public void initialAndOverdueBackupsAreDue(){assertTrue(AutoBackup.due(1000,0));assertTrue(AutoBackup.due(1000+AutoBackup.INTERVAL,1000));assertTrue(AutoBackup.due(1000+AutoBackup.INTERVAL*2,1000));}
 @Test public void noEarlyRepeatOrClockRollbackBackup(){assertFalse(AutoBackup.due(1001,1000));assertFalse(AutoBackup.due(999+AutoBackup.INTERVAL,1000));assertFalse(AutoBackup.due(500,1000));}
}
