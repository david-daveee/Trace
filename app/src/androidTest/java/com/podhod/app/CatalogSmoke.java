package com.podhod.app;
import android.app.*;import android.os.*;import android.view.*;import android.widget.*;import android.graphics.*;import java.io.*;
final class CatalogSmoke {
 static EditText search(View v){if(v instanceof EditText)return (EditText)v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int n=0;n<g.getChildCount();n++){EditText e=search(g.getChildAt(n));if(e!=null)return e;}}return null;}
 static void verify(Instrumentation i,MainActivity a){Bundle result=new Bundle();final Throwable[] failure={null};try{
  i.runOnMainSync(()->{try{a.page="library";a.catalogCategory="all";a.catalogQuery="";a.render();}catch(Throwable e){failure[0]=e;}});i.waitForIdleSync();SystemClock.sleep(5000);
  i.runOnMainSync(()->{try{EditText input=search(a.body);if(input==null)throw new AssertionError("Search missing");input.requestFocus();input.setText("unlikely-no-match-123");if(ScrollSmoke.find(a.body,AccountPanel.s("No plans found","Планы не найдены"))==null)throw new AssertionError("Filter failed");if(!input.hasFocus())throw new AssertionError("Search lost focus");input.setText("zlat");if(ScrollSmoke.find(a.body,AccountPanel.s("No plans found","Планы не найдены"))!=null)throw new AssertionError("Name search failed");input.setText("");}catch(Throwable e){failure[0]=e;}});
  i.waitForIdleSync();
  i.runOnMainSync(()->{PlanLikes.Entry e=PlanLikes.get(a).entries.values().iterator().next();for(var ref:e.buttons){PlanLikeButton button=ref.get();if(button!=null&&button.isAttachedToWindow()){android.graphics.Rect r=new android.graphics.Rect(0,0,button.getWidth(),button.getHeight());a.body.offsetDescendantRectToMyCoords(button,r);((ScrollView)a.body.getParent()).scrollTo(0,Math.max(0,r.top-a.dp(230)));break;}}});
  i.waitForIdleSync();SystemClock.sleep(300);
  i.runOnMainSync(()->{try{for(PlanLikes.Entry e:PlanLikes.get(a).entries.values())if(!e.ready)throw new AssertionError("Likes server read failed");View v=a.root;Bitmap b=Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.ARGB_8888);v.draw(new Canvas(b));try(FileOutputStream out=a.openFileOutput("catalog-likes.png",0)){b.compress(Bitmap.CompressFormat.PNG,100,out);}catch(Exception e){throw new RuntimeException(e);}b.recycle();}catch(Throwable e){failure[0]=e;}});
  if(failure[0]!=null)throw new AssertionError(failure[0]);
  result.putString("stream","PASS: live count reads, search, empty results and focus. No votes sent.\n");i.finish(Activity.RESULT_OK,result);
 }catch(Throwable e){result.putString("stream","FAIL: "+e);i.finish(Activity.RESULT_CANCELED,result);}}
}
