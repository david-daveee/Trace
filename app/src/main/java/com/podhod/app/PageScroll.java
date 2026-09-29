package com.podhod.app;

import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.ScrollView;

/** Keeps the visible section in place across same-page redraws and recreation. */
final class PageScroll {
 String key="";int index=-1,offset,y;ScrollView scroll;LinearLayout body;
 void capture(){if(scroll==null||body==null||scroll.getHeight()==0||body.getHeight()==0||scroll.isLayoutRequested())return;y=scroll.getScrollY();index=-1;offset=0;for(int i=0;i<body.getChildCount();i++){View child=body.getChildAt(i);if(child.getVisibility()!=View.GONE&&child.getBottom()>y){index=i;offset=y-child.getTop();break;}}}
 void begin(String next){capture();if(!key.equals(next)){index=-1;offset=0;y=0;}key=next;scroll=null;body=null;}
 void bind(ScrollView view,LinearLayout content){scroll=view;body=content;restore();}
 void restore(){if(scroll==null)return;final ScrollView target=scroll;final LinearLayout content=body;final int childIndex=index,relative=offset,fallback=y;target.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){public boolean onPreDraw(){if(target.getViewTreeObserver().isAlive())target.getViewTreeObserver().removeOnPreDrawListener(this);if(scroll!=target)return true;int desired=fallback;if(childIndex>=0&&childIndex<content.getChildCount())desired=content.getChildAt(childIndex).getTop()+relative;target.scrollTo(0,Math.max(0,desired));return true;}});}
 void save(Bundle out){capture();out.putString("scrollPage",key);out.putInt("scrollChild",index);out.putInt("scrollOffset",offset);out.putInt("scrollY",y);}
 void read(Bundle in){if(in==null)return;key=in.getString("scrollPage","");index=in.getInt("scrollChild",-1);offset=in.getInt("scrollOffset",0);y=in.getInt("scrollY",0);}
}
