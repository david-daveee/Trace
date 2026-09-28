package com.podhod.app;
import org.junit.Test;
import static org.junit.Assert.*;
public class SyncDecisionTest {
 @Test public void emptyAccountUploadsPhoneAutomatically(){assertEquals(SyncDecision.Action.PHONE,SyncDecision.decide(false,"","","","x"));}
 @Test public void returningAccountLoadsCloudAutomatically(){assertEquals(SyncDecision.Action.CLOUD,SyncDecision.decide(false,"","remote","","phone"));}
 @Test public void unchangedDoesNothing(){assertEquals(SyncDecision.Action.NOTHING,SyncDecision.decide(true,"a","a","x","x"));}
 @Test public void phoneOnlyUploads(){assertEquals(SyncDecision.Action.PHONE,SyncDecision.decide(true,"a","a","x","y"));}
 @Test public void remoteOnlyDownloads(){assertEquals(SyncDecision.Action.CLOUD,SyncDecision.decide(true,"a","b","x","x"));}
 @Test public void concurrentChangesUseAccountWithLocalBackup(){assertEquals(SyncDecision.Action.CLOUD,SyncDecision.decide(true,"a","b","x","y"));}
}
