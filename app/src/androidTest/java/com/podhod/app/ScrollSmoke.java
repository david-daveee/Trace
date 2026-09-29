package com.podhod.app;
import android.app.*;import android.os.*;import android.content.*;import android.view.*;import android.widget.*;
final class ScrollSmoke {
 static void settle(Instrumentation i){i.waitForIdleSync();SystemClock.sleep(300);i.waitForIdleSync();}
 static View find(View v,String label){if(v instanceof TextView&&label.contentEquals(((TextView)v).getText()))return v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int n=0;n<g.getChildCount();n++){View r=find(g.getChildAt(n),label);if(r!=null)return r;}}return null;}
 static void verify(Instrumentation i,MainActivity a){Bundle result=new Bundle();try{
  i.runOnMainSync(()->{a.page="settings";a.render();});settle(i);
  int[] y={0};i.runOnMainSync(()->{ScrollView scroll=(ScrollView)a.body.getParent();scroll.scrollTo(0,a.dp(450));y[0]=scroll.getScrollY();});if(y[0]<=0)throw new AssertionError("Settings did not scroll");
  i.runOnMainSync(()->a.changed.onReceive(a,new Intent("com.podhod.app.CHANGED")));settle(i);i.runOnMainSync(()->{if(Math.abs(((ScrollView)a.body.getParent()).getScrollY()-y[0])>2)throw new AssertionError("Redraw lost scroll");View label=find(a.body,Lang.t("ОБНОВЛЕНИЯ TRACE"));if(label==null)throw new AssertionError("Updates card missing");AppUpdates.draw(a,(LinearLayout)label.getParent());});settle(i);
  i.runOnMainSync(()->{if(Math.abs(((ScrollView)a.body.getParent()).getScrollY()-y[0])>2)throw new AssertionError("Updates redraw lost scroll");});
  Instrumentation.ActivityMonitor monitor=i.addMonitor(MainActivity.class.getName(),null,false);i.runOnMainSync(a::recreate);MainActivity recreated=(MainActivity)i.waitForMonitorWithTimeout(monitor,10000);i.removeMonitor(monitor);if(recreated==null)throw new AssertionError("Recreation timeout");settle(i);
  i.runOnMainSync(()->{if(!recreated.page.equals("settings")||Math.abs(((ScrollView)recreated.body.getParent()).getScrollY()-y[0])>2)throw new AssertionError("Recreation lost scroll");recreated.page="mine";recreated.render();});settle(i);i.runOnMainSync(()->{if(((ScrollView)recreated.body.getParent()).getScrollY()!=0)throw new AssertionError("Different page inherited scroll");});
  result.putString("stream","PASS: same-page redraw, updates card, Activity recreation and new-page reset.\n");i.finish(Activity.RESULT_OK,result);
 }catch(Throwable e){result.putString("stream","FAIL: "+e+"\n");i.finish(Activity.RESULT_CANCELED,result);}}
}
