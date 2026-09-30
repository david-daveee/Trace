package com.podhod.app;
import android.app.*;
import android.os.*;
import android.graphics.*;
import android.view.*;
import java.io.*;
import org.json.*;
/** Synthetic previews and read-only live feed. Never publishes locations or sends a message. */
final class WalkSocialSmoke {
 static void shot(MainActivity a,View view,String name)throws Exception{Bitmap b=Bitmap.createBitmap(view.getWidth(),view.getHeight(),Bitmap.Config.ARGB_8888);view.draw(new Canvas(b));try(FileOutputStream out=a.openFileOutput(name,0)){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();}
 static void verify(Instrumentation test,MainActivity a){Throwable[] failure={null};AlertDialog[] dialog={null};WalkFeed[] feed={null};JSONObject walk=WalkData.start(1780000000000L);Engine.put(walk,"title","Evening by the water");Engine.put(walk,"id","visual-fixture-only");Engine.put(walk,"ended",1780002100000L);Engine.put(walk,"meters",3100);JSONArray points=new JSONArray();for(int i=0;i<45;i++)points.put(new JSONArray().put((Object)(32.080+i*.00035)).put((Object)(34.767+Math.sin(i*.15)*.003)).put(1780000000000L+i*45000).put(5).put(false));Engine.put(walk,"points",points);
 try{test.runOnMainSync(()->{try{dialog[0]=WalkSocial.result(a,walk,()->{});}catch(Throwable e){failure[0]=e;}});SystemClock.sleep(1800);test.runOnMainSync(()->{try{if(failure[0]!=null)return;shot(a,dialog[0].getWindow().getDecorView(),"walk-result.png");dialog[0].dismiss();dialog[0]=FriendsPanel.viewWalk(a,WalkSharing.payload(walk,true,200));}catch(Throwable e){failure[0]=e;}});SystemClock.sleep(1200);test.runOnMainSync(()->{try{if(failure[0]!=null)return;shot(a,dialog[0].getWindow().getDecorView(),"walk-share-preview.png");dialog[0].dismiss();feed[0]=new WalkFeed(a);}catch(Throwable e){failure[0]=e;}});
 for(int i=0;i<40;i++){SystemClock.sleep(500);boolean[] busy={true};test.runOnMainSync(()->busy[0]=feed[0]!=null&&feed[0].busy);if(!busy[0])break;}
 test.runOnMainSync(()->{try{if(failure[0]!=null)return;if(feed[0].busy)throw new AssertionError("Feed still loading");if(feed[0].status.getText().toString().contains("Could not")||feed[0].status.getText().toString().contains("Не удалось"))throw new AssertionError("Live feed read failed");feed[0].list.removeAllViews();feed[0].status.setText("Shared with friends · preview");WalkFeed.Entry e=new WalkFeed.Entry();e.name="Alex · preview";e.portrait=new java.util.HashMap<>();e.payload=WalkSharing.payload(walk,true,200);feed[0].card(e);}catch(Throwable e){failure[0]=e;}});SystemClock.sleep(400);test.runOnMainSync(()->{try{if(failure[0]==null){shot(a,feed[0].dialog.getWindow().getDecorView(),"walk-feed-preview.png");feed[0].dialog.dismiss();a.page="steps";a.render();}}catch(Throwable e){failure[0]=e;}});
 }catch(Throwable e){failure[0]=e;}Bundle result=new Bundle();result.putString("stream",failure[0]==null?"PASS: result, protected-route preview and feed rendered; live feed read completed. No locations published and no messages sent.":"FAIL: "+failure[0]);test.finish(failure[0]==null?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);}
}
