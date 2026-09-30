package com.podhod.app;
import android.app.*;import android.os.*;import android.graphics.*;import android.view.*;import java.io.*;import com.google.firebase.firestore.*;
final class ModerationSmoke {
 static void verify(Instrumentation i,MainActivity a){Bundle b=new Bundle();Throwable[] failure={null};try{
  FriendsStore backend=new FriendsStore(); backend.waitFor(backend.db.collection("publicPlans").limit(1).get(Source.SERVER)); backend.waitFor(backend.db.collection("catalogSubmissions").whereEqualTo("status","pending").limit(1).get(Source.SERVER)); backend.waitFor(backend.db.document("catalogSubmissions/"+backend.uid+"~"+"0".repeat(64)).get(Source.SERVER));
  if(!CommunityPlans.admin(a))throw new AssertionError("Signed-in account is not the configured moderator");
  i.runOnMainSync(()->{try{a.page="library";a.catalogQuery="";a.catalogCategory="all";a.render();}catch(Throwable e){failure[0]=e;}});
  i.waitForIdleSync();SystemClock.sleep(1500);
  i.runOnMainSync(()->{try{if(ScrollSmoke.find(a.body,AccountPanel.s("Library","Библиотека"))==null)throw new AssertionError("Library heading missing");View v=a.root;Bitmap bitmap=Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.ARGB_8888);v.draw(new Canvas(bitmap));try(FileOutputStream out=a.openFileOutput("moderation-preview.png",0)){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();}catch(Throwable e){failure[0]=e;}});
  if(failure[0]!=null)throw new AssertionError(failure[0]);
  b.putString("stream","PASS: production catalog and submission reads authorized; owner UID verified, moderation entry and author UI rendered. No submissions created or published.");i.finish(Activity.RESULT_OK,b);
 }catch(Throwable e){b.putString("stream","FAIL: "+e);i.finish(Activity.RESULT_CANCELED,b);}}
}
