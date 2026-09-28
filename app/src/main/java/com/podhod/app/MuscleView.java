package com.podhod.app;
import android.app.AlertDialog;
import android.graphics.*;
import android.view.View;
import android.widget.*;
import org.json.*;
import java.util.*;

final class MuscleView {
 static void editor(MainActivity a,LinearLayout box,JSONObject draft){a.link(box,Lang.t("Мышцы упражнения"),()->{Set<String> current=Muscles.of(draft);boolean[] selected=new boolean[Muscles.KEYS.length];String[] labels=new String[selected.length];for(int i=0;i<labels.length;i++){labels[i]=Lang.t(Muscles.LABELS[i]);selected[i]=current.contains(Muscles.KEYS[i]);}new AlertDialog.Builder(a).setTitle(Lang.t("Какие мышцы работают?")).setMultiChoiceItems(labels,selected,(d,i,on)->selected[i]=on).setPositiveButton(Lang.t("Сохранить"),(d,w)->{JSONArray values=new JSONArray();for(int i=0;i<selected.length;i++)if(selected[i])values.put(Muscles.KEYS[i]);Engine.put(draft,"muscles",values);}).setNeutralButton(Lang.t("Определять автоматически"),(d,w)->draft.remove("muscles")).setNegativeButton(Lang.t("Отмена"),null).show();});}
 static void show(MainActivity a,LinearLayout parent,JSONArray sets,String subtitle){LinearLayout card=a.card(parent);a.label(card,Lang.t("КАРТА МЫШЦ"));card.addView(a.title(subtitle,21));a.space(card,8);Map<String,Integer> counts=Muscles.count(sets);card.addView(new Body(a,counts),new LinearLayout.LayoutParams(-1,a.dp(350)));LinearLayout captions=new LinearLayout(a);for(String label:new String[]{"Спереди","Сзади"}){TextView caption=a.text(Lang.t(label),12,a.MUTED);caption.setGravity(android.view.Gravity.CENTER);captions.addView(caption,new LinearLayout.LayoutParams(0,-2,1));}card.addView(captions);a.space(card,16);int max=0;for(int n:counts.values())max=Math.max(max,n);if(max==0)card.addView(a.text(Lang.t("Отмечай подходы — задействованные мышцы подсветятся."),14,a.MUTED));for(int i=0;i<Muscles.KEYS.length;i++){int n=counts.get(Muscles.KEYS[i]);if(n==0)continue;LinearLayout row=new LinearLayout(a);TextView label=a.text(Lang.t(Muscles.LABELS[i]),14,a.TEXT);row.addView(label,new LinearLayout.LayoutParams(0,-2,1));row.addView(a.text(String.valueOf(n),14,a.GREEN));card.addView(row);a.space(card,4);a.progress(card,n,max);a.space(card,10);}card.addView(a.text(Lang.t("Подходы с участием мышцы, включая вспомогательную работу. Это оценка по упражнениям, а не измерение усталости."),12,a.MUTED));if(Muscles.unknown(sets)>0){a.space(card,8);card.addView(a.text(Lang.t("Есть нераспознанные упражнения. Укажи мышцы в редакторе плана."),13,0xFFE7C46D));}}
 static final class Body extends View {
  final java.util.List<android.graphics.drawable.Drawable> regions=new ArrayList<>();
  final java.util.List<Boolean> backs=new ArrayList<>();
  final Map<String,Integer> counts;int max=1;
  Body(MainActivity a,Map<String,Integer> counts){super(a);this.counts=counts;for(int n:counts.values())max=Math.max(max,n);
   setContentDescription(Lang.t("Мышцы спереди и сзади. Подробности перечислены ниже."));
   add(a, R.drawable.anatomy_front_chest, "chest", false);
   add(a, R.drawable.anatomy_front_obliques, "abs", false);
   add(a, R.drawable.anatomy_front_abs, "abs", false);
   add(a, R.drawable.anatomy_front_biceps, "biceps", false);
   add(a, R.drawable.anatomy_front_triceps, "triceps", false);
   add(a, R.drawable.anatomy_front_neck, "", false);
   add(a, R.drawable.anatomy_front_trapezius, "back", false);
   add(a, R.drawable.anatomy_front_deltoids, "shoulders", false);
   add(a, R.drawable.anatomy_front_adductors, "", false);
   add(a, R.drawable.anatomy_front_quadriceps, "quads", false);
   add(a, R.drawable.anatomy_front_knees, "", false);
   add(a, R.drawable.anatomy_front_tibialis, "", false);
   add(a, R.drawable.anatomy_front_calves, "calves", false);
   add(a, R.drawable.anatomy_front_forearm, "forearms", false);
   add(a, R.drawable.anatomy_front_hands, "", false);
   add(a, R.drawable.anatomy_front_ankles, "", false);
   add(a, R.drawable.anatomy_front_feet, "", false);
   add(a, R.drawable.anatomy_front_head, "", false);
   add(a, R.drawable.anatomy_front_hair, "", false);
   add(a, R.drawable.anatomy_back_neck, "", true);
   add(a, R.drawable.anatomy_back_trapezius, "back", true);
   add(a, R.drawable.anatomy_back_deltoids, "shoulders", true);
   add(a, R.drawable.anatomy_back_upper_back, "back", true);
   add(a, R.drawable.anatomy_back_triceps, "triceps", true);
   add(a, R.drawable.anatomy_back_lower_back, "lowerback", true);
   add(a, R.drawable.anatomy_back_forearm, "forearms", true);
   add(a, R.drawable.anatomy_back_gluteal, "glutes", true);
   add(a, R.drawable.anatomy_back_adductors, "", true);
   add(a, R.drawable.anatomy_back_hamstring, "hamstrings", true);
   add(a, R.drawable.anatomy_back_calves, "calves", true);
   add(a, R.drawable.anatomy_back_ankles, "", true);
   add(a, R.drawable.anatomy_back_feet, "", true);
   add(a, R.drawable.anatomy_back_hands, "", true);
   add(a, R.drawable.anatomy_back_head, "", true);
   add(a, R.drawable.anatomy_back_hair, "", true);
  }
  void add(MainActivity a,int resource,String muscle,boolean back){
   android.graphics.drawable.Drawable drawable=a.getDrawable(resource).mutate();
   int n=counts.getOrDefault(muscle,0);float t=(float)n/max;
   drawable.setTint(n==0?(muscle.isEmpty()?0xFF455365:0xFF536578):Color.rgb((int)(65+46*t),(int)(165+62*t),(int)(143+60*t)));
   regions.add(drawable);backs.add(back);
  }
  protected void onDraw(Canvas c){super.onDraw(c);
   int width=Math.min(getWidth()/2,getHeight()/2),height=width*2;
   int top=(getHeight()-height)/2;
   for(int i=0;i<regions.size();i++){
    int left=(backs.get(i)?3:1)*getWidth()/4-width/2;
    android.graphics.drawable.Drawable drawable=regions.get(i);
    drawable.setBounds(left,top,left+width,top+height);drawable.draw(c);
   }
  }
 }
}
