package com.podhod.app;
import android.content.Context;
import android.graphics.*;
import android.view.View;
/** Compact workout completion indicator, also described to screen readers. */
final class WorkoutRing extends View {
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private final int done,total;
 WorkoutRing(Context c,int done,int total){super(c);this.done=done;this.total=total;setContentDescription(Lang.t("Выполнено ")+done+Lang.t(" из ")+total+Lang.t(" подходов"));}
 @Override protected void onDraw(Canvas c){super.onDraw(c);float d=getResources().getDisplayMetrics().density,w=getWidth(),h=getHeight(),size=Math.min(w,h)-12*d;RectF r=new RectF((w-size)/2,(h-size)/2,(w+size)/2,(h+size)/2);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(5*d);paint.setStrokeCap(Paint.Cap.ROUND);paint.setColor(0xFF2C4941);c.drawOval(r,paint);paint.setColor(0xFF6FE3CB);c.drawArc(r,-90,total==0?0:360f*done/total,false,paint);paint.setStyle(Paint.Style.FILL);paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(18*d);paint.setColor(Color.WHITE);c.drawText((total==0?0:Math.round(100f*done/total))+"%",w/2,h/2-(paint.ascent()+paint.descent())/2,paint);}
}

