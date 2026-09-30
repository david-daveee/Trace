package com.podhod.app;
import android.graphics.*;
import android.view.View;
import android.widget.*;
import org.json.JSONObject;

/** Added load only: one stack of plates, no bar and no body-weight contribution. */
final class DipBelt {
 static boolean enabled(JSONObject g){if(g.has("dipBelt"))return g.optBoolean("dipBelt");if(g.optBoolean("barbell"))return false;for(String key:new String[]{"zlatLift","intermediateLift","advancedLift"})if(g.optString(key).equals("pullup")||g.optString(key).equals("dip"))return true;return false;}
 static int[] plates(double kg){int[] counts=new int[5];if(!Double.isFinite(kg)||kg<0||kg>2000)return counts;int units=(int)Math.floor(kg/1.25+1e-8);int[] sizes={16,8,4,2,1};for(int i=0;i<5;i++){counts[i]=units/sizes[i];units%=sizes[i];}return counts;}
 static double assembled(int[] counts){double sum=0;for(int i=0;i<5;i++)sum+=counts[i]*Barbell.PLATES[i];return sum;}
 static CheckBox checkbox(MainActivity a,LinearLayout parent,boolean checked,CheckBox barbell){CheckBox box=new CheckBox(a);box.setText(Lang.t("Пояс с блинами · дополнительный вес"));box.setTextColor(a.TEXT);box.setButtonTintList(android.content.res.ColorStateList.valueOf(a.GREEN));box.setChecked(checked&&!barbell.isChecked());parent.addView(box);box.setOnCheckedChangeListener((v,on)->{if(on)barbell.setChecked(false);});barbell.setOnCheckedChangeListener((v,on)->{if(on)box.setChecked(false);});return box;}
 static void show(MainActivity a,LinearLayout parent,double kg){if(kg<0||!Double.isFinite(kg))return;LinearLayout card=a.column();card.setPadding(a.dp(14),a.dp(14),a.dp(14),a.dp(14));card.setBackground(a.shape(0xE018212E,18));a.space(parent,12);parent.addView(card);a.label(card,Lang.t("БЛИНЫ НА ПОЯС"));int[] counts=plates(kg);double loaded=assembled(counts);card.addView(new Drawing(a,counts),new LinearLayout.LayoutParams(-1,a.dp(150)));card.addView(a.text(Barbell.summary(counts),15,a.TEXT));card.addView(a.text(loaded==0?Lang.t("Только собственный вес"):Lang.t("Дополнительный вес: +")+Engine.number(loaded)+Lang.t(" кг"),13,a.MUTED));if(Math.abs(loaded-kg)>.001){a.space(card,8);card.addView(a.text(Lang.t("Этот вес нельзя собрать точно. На рисунке ")+Engine.number(loaded)+Lang.t(" кг. Вес подхода не изменён."),14,0xFFE7C46D));}}
 static final class Drawing extends View {
  final int[] counts;final Paint paint=new Paint(3);
  Drawing(MainActivity a,int[] counts){super(a);this.counts=counts;setContentDescription(Lang.t("БЛИНЫ НА ПОЯС")+": "+Barbell.summary(counts));setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);}
  void rect(Canvas c,float x,float y,float w,float h,int color){paint.setStyle(Paint.Style.FILL);paint.setColor(color);c.drawRoundRect(x,y,x+w,y+h,3,3,paint);}
  @Override protected void onDraw(Canvas c){super.onDraw(c);c.save();float scale=Math.min(getWidth()/340f,getHeight()/150f);c.translate((getWidth()-340*scale)/2,(getHeight()-150*scale)/2);c.scale(scale,scale);
   // Flat side-view equipment drawing, using the same palette and plate shapes as Barbell.
   Path belt=new Path();belt.moveTo(101,18);belt.quadTo(170,-1,239,18);belt.lineTo(235,33);belt.quadTo(233,37,228,35);belt.quadTo(170,20,112,35);belt.quadTo(107,37,105,33);belt.close();paint.setColor(0xFF66788D);paint.setStyle(Paint.Style.FILL);c.drawPath(belt,paint);
   Path edge=new Path();edge.moveTo(109,20);edge.quadTo(170,5,231,20);paint.setStyle(Paint.Style.STROKE);paint.setColor(0xFFA6B5C6);paint.setStrokeWidth(1.2f);c.drawPath(edge,paint);
   // Angled D-rings sit behind short webbing tabs, rather than floating oval loops.
   for(int side:new int[]{-1,1}){c.save();c.translate(side<0?110:230,31);c.rotate(side<0?-32:32);Path ring=new Path();ring.moveTo(-5,-2);ring.lineTo(5,-2);ring.lineTo(5,2);ring.cubicTo(5,9,-5,9,-5,2);ring.close();paint.setStyle(Paint.Style.STROKE);paint.setColor(0xFFD5DFEB);paint.setStrokeWidth(2);c.drawPath(ring,paint);rect(c,-6,-7,12,7,0xFF46586B);paint.setColor(0xFF92A4B9);c.drawCircle(-3,-4,1,paint);c.drawCircle(3,-4,1,paint);c.restore();}
   Path chain=new Path();chain.moveTo(113,38);chain.quadTo(133,60,170,77);chain.quadTo(207,60,227,38);paint.setStyle(Paint.Style.STROKE);paint.setColor(0xFF8E9AAE);paint.setStrokeWidth(2.5f);paint.setPathEffect(new DashPathEffect(new float[]{4,3},0));c.drawPath(chain,paint);paint.setPathEffect(null);paint.setStyle(Paint.Style.FILL);
   int count=0;for(int n:counts)count+=n;boolean compact=count>8;int shown=count;if(compact){shown=0;for(int n:counts)if(n>0)shown++;}float width=Math.min(27,244f/Math.max(1,shown)),start=170-shown*width/2;int index=0;
   for(int i=0;i<5;i++){int repeat=compact?(counts[i]>0?1:0):counts[i];for(int n=0;n<repeat;n++){float height=72-i*10,x=start+index*width;rect(c,x,102-height/2,width-3,height,Barbell.COLORS[i]);paint.setColor(0xFF142130);paint.setTextAlign(Paint.Align.CENTER);paint.setTypeface(Typeface.DEFAULT);String label=compact?counts[i]+"×":Engine.number(Barbell.PLATES[i]);paint.setTextSize(Math.min(10,(width-5)*1.6f/label.length()));c.drawText(label,x+(width-3)/2,105,paint);index++;}}
   c.restore();
  }
 }
}
