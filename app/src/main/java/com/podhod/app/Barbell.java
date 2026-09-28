package com.podhod.app;
import android.graphics.*;
import android.view.View;
import android.widget.*;
import java.util.*;

final class Barbell {
 static final double[] PLATES={20,10,5,2.5,1.25};
 static final int[] COLORS={0xFF77A9EF,0xFF78D5AF,0xFFE7C46D,0xFFBABFCB,0xFFDE9778};
 static int[] plates(double total){int[] result=new int[5];if(!Double.isFinite(total)||total<20||total>2000)return result;int units=(int)Math.floor((total-20)/2/1.25+1e-8);int[] sizes={16,8,4,2,1};for(int i=0;i<5;i++){result[i]=units/sizes[i];units%=sizes[i];}return result;}
 static double assembled(int[] counts){double sum=20;for(int i=0;i<5;i++)sum+=2*counts[i]*PLATES[i];return sum;}
 static CheckBox checkbox(MainActivity a,LinearLayout parent,boolean checked){CheckBox box=new CheckBox(a);box.setText(Lang.t("Упражнение со штангой · гриф 20 кг"));box.setTextColor(a.TEXT);box.setButtonTintList(android.content.res.ColorStateList.valueOf(a.GREEN));box.setChecked(checked);parent.addView(box);return box;}
 static String summary(int[] counts){StringBuilder b=new StringBuilder();for(int i=0;i<5;i++)if(counts[i]>0){if(b.length()>0)b.append(" · ");b.append(counts[i]).append(" × ").append(Engine.number(PLATES[i])).append(Lang.t(" кг"));}return b.length()==0?Lang.t("Без блинов"):b.toString();}
 static void show(MainActivity a,LinearLayout parent,double total){LinearLayout card=a.column();card.setPadding(a.dp(14),a.dp(14),a.dp(14),a.dp(14));card.setBackground(a.shape(0xE018212E,18));a.space(parent,12);parent.addView(card);a.label(card,Lang.t("КАК СОБРАТЬ ШТАНГУ"));if(total<20||!Double.isFinite(total)){card.addView(a.text(Lang.t("Гриф весит 20 кг. Укажи общий вес не меньше 20 кг."),14,a.MUTED));return;}int[] counts=plates(total);double loaded=assembled(counts);card.addView(new Drawing(a,counts),new LinearLayout.LayoutParams(-1,a.dp(106)));card.addView(a.text(Lang.t("С каждой стороны: ")+summary(counts),15,a.TEXT));card.addView(a.text(Lang.t("Гриф 20 кг · всего ")+Engine.number(loaded)+Lang.t(" кг"),13,a.MUTED));if(Math.abs(total-loaded)>.001){a.space(card,8);card.addView(a.text(Lang.t("Этот вес нельзя собрать точно. На рисунке ")+Engine.number(loaded)+Lang.t(" кг. Вес подхода не изменён."),14,0xFFE7C46D));}}
 static final class Drawing extends View {
  final int[] counts;final Paint paint=new Paint(3);
  Drawing(MainActivity a,int[] counts){super(a);this.counts=counts;setContentDescription(Lang.t("С каждой стороны: ")+summary(counts));setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);}
  void rect(Canvas c,float x,float y,float w,float h,int color){paint.setColor(color);c.drawRoundRect(x,y,x+w,y+h,3,3,paint);}
  @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);canvas.save();canvas.scale(getWidth()/340f,getHeight()/106f);rect(canvas,4,48,332,10,0xFF8E9AAE);rect(canvas,119,49,102,8,0xFFD5DFEB);for(int side:new int[]{-1,1}){rect(canvas,side<0?111:222,41,7,24,0xFFEDF3F8);int count=0;for(int n:counts)count+=n;boolean compact=count>8;int shown=compact?0:count;if(compact)for(int n:counts)if(n>0)shown++;float width=Math.min(19,94f/Math.max(1,shown));int index=0;for(int i=0;i<5;i++){int repeat=compact?(counts[i]>0?1:0):counts[i];for(int n=0;n<repeat;n++){float height=82-i*12,x=side<0?110-(index+1)*width:230+index*width;rect(canvas,x,53-height/2,width-2,height,COLORS[i]);paint.setColor(0xFF142130);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(Math.min(10,width-3));String label=compact?counts[i]+"×":Engine.number(PLATES[i]);paint.setTextSize(Math.min(10,(width-4)*1.6f/label.length()));canvas.drawText(label,x+(width-2)/2,56,paint);index++;}}}canvas.restore();}
 }
}
