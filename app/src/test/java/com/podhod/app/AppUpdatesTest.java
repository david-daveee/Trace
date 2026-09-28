package com.podhod.app;
import org.junit.Test;
import static org.junit.Assert.*;
public class AppUpdatesTest {
 @Test public void versionsAreComparedNumerically(){assertTrue(AppUpdates.compare("v0.24.0","0.23.2")>0);assertTrue(AppUpdates.compare("0.9.0","0.10.0")<0);assertEquals(0,AppUpdates.compare("v1.2.3","1.2.3"));}
 @Test public void stableReleaseRequiresOfficialApk(){String raw="{\"tag_name\":\"v0.24.0\",\"body\":\"## Update\\nNew feature\\n## Download\\nInstall\",\"assets\":[{\"name\":\"Trace.apk\",\"size\":123,\"browser_download_url\":\"https://github.com/david-daveee/Trace/releases/download/v0.24.0/Trace.apk\"}]}";assertEquals("## Update\nNew feature",AppUpdates.parse(raw).optString("notes"));try{AppUpdates.parse(raw.replace("github.com/","example.com/"));fail();}catch(IllegalArgumentException expected){}try{AppUpdates.parse(raw.replace("\"body\"","\"prerelease\":true,\"body\""));fail();}catch(IllegalArgumentException expected){}}
}
