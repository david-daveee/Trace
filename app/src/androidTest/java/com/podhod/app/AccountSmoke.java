package com.podhod.app;
import android.app.*;
import android.os.*;
import org.json.*;
/** Read-only validation against the installed app's actual library. No sign-in or uploads. */
public class AccountSmoke extends Instrumentation {
 boolean cloudRead;
 public void onCreate(Bundle b){super.onCreate(b);cloudRead="true".equals(b.getString("cloudRead"));start();}
 public void onStart(){Bundle result=new Bundle();final Throwable[] failure={null};runOnMainSync(()->{try{
  var c=getTargetContext();Lang.init(c);
  if(!AccountSync.configured(c))throw new AssertionError("Firebase missing");
  if(c.getResources().getIdentifier("default_web_client_id","string",c.getPackageName())==0)throw new AssertionError("OAuth missing");
  JSONObject local=SyncLocal.capture(c);SyncLocal.validate(local);
  String before=SyncPayload.fingerprint(local);
  CloudSnapshot encoded=CloudSnapshot.encode(local);
  JSONObject decoded=CloudSnapshot.decode(encoded.chunks,encoded.sha256,encoded.bytes);
  SyncLocal.validate(decoded);
  if(!before.equals(SyncPayload.fingerprint(decoded)))throw new AssertionError("Transfer round trip changed data");
  JSONObject merged=SyncPayload.choose(local,decoded,true);SyncLocal.validate(merged);
  if(!before.equals(SyncPayload.fingerprint(SyncLocal.capture(c))))throw new AssertionError("Validation changed local data");
  android.content.Context isolated=new android.content.ContextWrapper(c){
   public android.content.SharedPreferences getSharedPreferences(String name,int mode){return super.getSharedPreferences("recovery-smoke-"+name,mode);}
   public java.io.File getFilesDir(){java.io.File dir=new java.io.File(super.getCacheDir(),"recovery-smoke");dir.mkdirs();return dir;}
  };
  isolated.getSharedPreferences("podhod",0).edit().clear().putString("state","original").commit();
  isolated.getSharedPreferences("trace-steps",0).edit().clear().putFloat("stepLengthCm",72.5f).putLong("counter",12345678901L).putBoolean("enabled",true).commit();
  SyncRecovery.begin(isolated);
  isolated.getSharedPreferences("podhod",0).edit().putString("state","partial restore").commit();
  isolated.getSharedPreferences("trace-steps",0).edit().clear().commit();
  SyncRecovery.recover(isolated);
  if(!"original".equals(isolated.getSharedPreferences("podhod",0).getString("state",""))||isolated.getSharedPreferences("trace-steps",0).getFloat("stepLengthCm",0)!=72.5f||isolated.getSharedPreferences("trace-steps",0).getLong("counter",0)!=12345678901L||!isolated.getSharedPreferences("trace-steps",0).getBoolean("enabled",false))throw new AssertionError("Interrupted restore recovery failed");
 }catch(Throwable e){failure[0]=e;}});
 String remote="";
 if(failure[0]==null&&cloudRead)try {
  var user=com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
  if(user==null)throw new AssertionError("No signed-in user");
  CloudStore.Revision revision=new CloudStore(user.getUid(),()->true).read();
  if(revision.payload!=null)SyncLocal.validate(revision.payload);
  remote=revision.payload==null?" Cloud read passed: empty account.":" Cloud read and validation passed. Active workout: "+revision.payload.optJSONObject("workouts").has("active")+"; paused: "+(revision.payload.optJSONObject("workouts").optJSONObject("savedSessions")==null?0:revision.payload.optJSONObject("workouts").optJSONObject("savedSessions").length())+".";
 }catch(Throwable e){failure[0]=e;}
 result.putString("stream",failure[0]==null?"PASS: Firebase/OAuth configured, real local snapshot valid, compressed transfer round trip, walk merge valid, no uploads or data changes."+remote+"\n":"FAIL: "+failure[0]);
 finish(failure[0]==null?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);
 }
}
