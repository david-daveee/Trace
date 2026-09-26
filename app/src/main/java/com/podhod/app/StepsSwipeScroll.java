package com.podhod.app;
import android.graphics.Rect;
import android.view.*;
import android.widget.ScrollView;
/** Horizontal section navigation; vertical scrolling and map gestures keep ownership. */
final class StepsSwipeScroll extends ScrollView {
 final MainActivity a;final int slop;float startX,startY;boolean eligible,swiping;StepsPage startingPage;
 StepsSwipeScroll(MainActivity a){super(a);this.a=a;slop=ViewConfiguration.get(a).getScaledTouchSlop();}
 @Override public boolean dispatchTouchEvent(MotionEvent e){
  int action=e.getActionMasked();
  if(action==MotionEvent.ACTION_DOWN){
   startX=e.getX();startY=e.getY();swiping=false;startingPage=a.stepsView;eligible="steps".equals(a.page)&&startingPage!=null;
   if(eligible&&a.walkTab&&startingPage.walk!=null){Rect map=new Rect();if(startingPage.walk.map.getGlobalVisibleRect(map)&&map.contains((int)e.getRawX(),(int)e.getRawY()))eligible=false;}
  }
  if(swiping){if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)swiping=false;return true;}
  if(action==MotionEvent.ACTION_POINTER_DOWN||action==MotionEvent.ACTION_CANCEL||a.stepsView!=startingPage)eligible=false;
  if(eligible&&action==MotionEvent.ACTION_MOVE){
   float dx=e.getX()-startX,dy=e.getY()-startY;
   if(Math.abs(dy)>slop&&Math.abs(dy)>=Math.abs(dx))eligible=false;
   else if(Math.abs(dx)>a.dp(48)&&Math.abs(dx)>Math.abs(dy)*1.6f&&((dx<0&&!a.walkTab)||(dx>0&&a.walkTab))){
    MotionEvent cancel=MotionEvent.obtain(e);cancel.setAction(MotionEvent.ACTION_CANCEL);super.dispatchTouchEvent(cancel);cancel.recycle();
    swiping=true;eligible=false;a.stepsView.select(dx<0);scrollTo(0,0);
    View panel=a.walkTab?a.stepsView.walkContent:a.stepsView.stepsContent;panel.setTranslationX(a.dp(dx<0?18:-18));panel.setAlpha(.75f);panel.animate().translationX(0).alpha(1).setDuration(160).start();return true;
   }
  }
  if(action==MotionEvent.ACTION_UP)eligible=false;
  return super.dispatchTouchEvent(e);
 }
}
