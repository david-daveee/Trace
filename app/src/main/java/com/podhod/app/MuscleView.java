package com.podhod.app;
import android.app.AlertDialog;
import android.graphics.*;
import android.view.View;
import android.widget.*;
import org.json.*;
import java.util.*;

final class MuscleView {
 static void editor(MainActivity a,LinearLayout box,JSONObject draft){a.link(box,Lang.t("Мышцы упражнения"),()->{Set<String> current=Muscles.of(draft);boolean[] selected=new boolean[Muscles.KEYS.length];String[] labels=new String[selected.length];for(int i=0;i<labels.length;i++){labels[i]=Lang.t(Muscles.LABELS[i]);selected[i]=current.contains(Muscles.KEYS[i]);}new AlertDialog.Builder(a).setTitle(Lang.t("Какие мышцы работают?")).setMultiChoiceItems(labels,selected,(d,i,on)->selected[i]=on).setPositiveButton(Lang.t("Сохранить"),(d,w)->{JSONArray values=new JSONArray();for(int i=0;i<selected.length;i++)if(selected[i])values.put(Muscles.KEYS[i]);Engine.put(draft,"muscles",values);}).setNeutralButton(Lang.t("Определять автоматически"),(d,w)->draft.remove("muscles")).setNegativeButton(Lang.t("Отмена"),null).show();});}
 static void show(MainActivity a,LinearLayout parent,JSONArray sets,String subtitle){LinearLayout card=a.card(parent);a.label(card,Lang.t("КАРТА МЫШЦ"));card.addView(a.title(subtitle,21));a.space(card,8);Map<String,Integer> counts=Muscles.count(sets);card.addView(new Body(a,counts),new LinearLayout.LayoutParams(-1,a.dp(270)));LinearLayout captions=new LinearLayout(a);for(String label:new String[]{"Спереди","Сзади"}){TextView caption=a.text(Lang.t(label),12,a.MUTED);caption.setGravity(android.view.Gravity.CENTER);captions.addView(caption,new LinearLayout.LayoutParams(0,-2,1));}card.addView(captions);a.space(card,16);int max=0;for(int n:counts.values())max=Math.max(max,n);if(max==0)card.addView(a.text(Lang.t("Отмечай подходы — задействованные мышцы подсветятся."),14,a.MUTED));for(int i=0;i<Muscles.KEYS.length;i++){int n=counts.get(Muscles.KEYS[i]);if(n==0)continue;LinearLayout row=new LinearLayout(a);TextView label=a.text(Lang.t(Muscles.LABELS[i]),14,a.TEXT);row.addView(label,new LinearLayout.LayoutParams(0,-2,1));row.addView(a.text(String.valueOf(n),14,a.GREEN));card.addView(row);a.space(card,4);a.progress(card,n,max);a.space(card,10);}card.addView(a.text(Lang.t("Подходы с участием мышцы, включая вспомогательную работу. Это оценка по упражнениям, а не измерение усталости."),12,a.MUTED));if(Muscles.unknown(sets)>0){a.space(card,8);card.addView(a.text(Lang.t("Есть нераспознанные упражнения. Укажи мышцы в редакторе плана."),13,0xFFE7C46D));}}
 static final class Body extends View {
  final Map<String,Integer> counts;final Paint p=new Paint(3);int max=1;
  Body(MainActivity a,Map<String,Integer> counts){super(a);this.counts=counts;for(int n:counts.values())max=Math.max(max,n);setContentDescription(Lang.t("Мышцы спереди и сзади. Подробности перечислены ниже."));}
  int color(String key){int n=counts.getOrDefault(key,0);if(n==0)return 0xFF29394A;float t=.35f+.65f*n/max;return Color.rgb((int)(48+63*t),(int)(113+114*t),(int)(110+93*t));}
  void oval(Canvas c,float l,float t,float r,float b,int color){p.setColor(color);c.drawOval(l,t,r,b,p);}
  void shape(Canvas c,String key,float... xy){Path path=new Path();int n=xy.length;path.moveTo((xy[n-2]+xy[0])/2,(xy[n-1]+xy[1])/2);for(int i=0;i<n;i+=2){int next=(i+2)%n;path.quadTo(xy[i],xy[i+1],(xy[i]+xy[next])/2,(xy[i+1]+xy[next+1])/2);}path.close();p.setColor(color(key));c.drawPath(path,p);}
  protected void onDraw(Canvas c){super.onDraw(c);float scale=Math.min(getWidth()/340f,getHeight()/270f);c.save();c.translate((getWidth()-340*scale)/2,0);c.scale(scale,scale);for(int back=0;back<2;back++){c.save();c.translate(back==0?28:198,8);oval(c,40,0,74,38,0xFF526375);p.setColor(0xFF3B4C5E);c.drawRoundRect(47,31,67,51,5,5,p);
   shape(c,"shoulders",36,46,23,51,14,71,29,77,39,60);shape(c,"shoulders",78,46,91,51,100,71,85,77,75,60);
   if(back==0){shape(c,"chest",39,47,55,51,54,77,32,75,30,64);shape(c,"chest",75,47,59,51,60,77,82,75,84,64);for(int j=0;j<3;j++){p.setColor(color("abs"));c.drawRoundRect(43,82+j*12,55,92+j*12,3,3,p);c.drawRoundRect(59,82+j*12,71,92+j*12,3,3,p);}}
   else{shape(c,"back",38,47,56,52,54,106,41,100,31,75);shape(c,"back",76,47,58,52,60,106,73,100,83,75);shape(c,"lowerback",44,103,70,103,75,121,39,121);}
   shape(c,back==0?"biceps":"triceps",15,76,28,80,23,104,10,103);shape(c,back==0?"biceps":"triceps",99,76,86,80,91,104,104,103);shape(c,"forearms",10,107,23,108,15,141,4,138);shape(c,"forearms",104,107,91,108,99,141,110,138);oval(c,1,137,15,155,0xFF526375);oval(c,99,137,113,155,0xFF526375);
   shape(c,back==0?"abs":"glutes",37,116,55,117,55,144,33,141);shape(c,back==0?"abs":"glutes",77,116,59,117,59,144,81,141);
   shape(c,back==0?"quads":"hamstrings",33,146,54,147,51,189,34,190,29,164);shape(c,back==0?"quads":"hamstrings",81,146,60,147,63,189,80,190,85,164);oval(c,35,193,50,203,0xFF526375);oval(c,64,193,79,203,0xFF526375);
   shape(c,back==0?"":"calves",35,207,50,207,48,226,43,244,34,244,31,226);shape(c,back==0?"":"calves",79,207,64,207,66,226,71,244,80,244,83,226);oval(c,26,240,44,251,0xFF526375);oval(c,70,240,88,251,0xFF526375);c.restore();}c.restore();}
 }
}
