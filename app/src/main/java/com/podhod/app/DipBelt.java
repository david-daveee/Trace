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
  @Override protected void onDraw(Canvas c){super.onDraw(c);c.save();c.scale(getWidth()/340f,getHeight()/150f);
   Path belt=new Path();belt.moveTo(93,17);belt.quadTo(170,0,247,17);belt.lineTo(242,44);belt.quadTo(170,30,98,44);belt.close();paint.setColor(0xFF526578);paint.setStyle(Paint.Style.FILL);c.drawPath(belt,paint);paint.setColor(0xFF859AA7);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.5f);c.drawPath(belt,paint);paint.setColor(0xFFDAE6EE);paint.setStrokeWidth(4);c.drawRoundRect(159,13,181,33,3,3,paint);
   Path chain=new Path();chain.moveTo(103,43);chain.quadTo(119,86,170,100);chain.quadTo(221,86,237,43);paint.setColor(0xFFA6B6C8);paint.setStrokeWidth(3);paint.setPathEffect(new DashPathEffect(new float[]{5,3},0));c.drawPath(chain,paint);paint.setPathEffect(null);paint.setStyle(Paint.Style.FILL);
   int number=0;for(int n:counts)number+=n;boolean compact=number>5;int shown=number;if(compact){shown=0;for(int n:counts)if(n>0)shown++;}float step=Math.min(66,300f/Math.max(1,shown)),start=170-(shown-1)*step/2;int index=0;
   for(int i=0;i<5;i++){int repeat=compact?(counts[i]>0?1:0):counts[i];for(int n=0;n<repeat;n++){float x=start+index*step,r=Math.min(step*.46f,31-i*3);paint.setColor(Barbell.COLORS[i]);c.drawCircle(x,106,r,paint);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);paint.setColor(0x660D131C);c.drawCircle(x,106,r-4,paint);paint.setStyle(Paint.Style.FILL);paint.setColor(0xFF18212E);c.drawCircle(x,98,3,paint);paint.setTextAlign(Paint.Align.CENTER);String label=(compact?counts[i]+"×":"")+Engine.number(Barbell.PLATES[i]);paint.setTextSize(Math.min(14,r*2*1.4f/label.length()));c.drawText(label,x,116,paint);index++;}}
   c.restore();
  }
 }
}
