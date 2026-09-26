package com.podhod.app;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONObject;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Live walking summary, independent of saved strength workouts. */
final class WalkingProgress {
    final MainActivity a;
    final int amber=0xFFFFC88C;
    final TextView steps,distance,weekly,total,detail,caption;
    final LinearLayout chart;
    LocalDate selected=LocalDate.now(),shownToday=LocalDate.now();
    String signature="";
    WalkingProgress(MainActivity a,LinearLayout parent){
        this.a=a;
        LinearLayout card=a.card(parent);
        GradientDrawable background=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xFF322B22,0xFF1D2429});background.setCornerRadius(a.dp(26));background.setStroke(a.dp(1),0xFF564333);card.setBackground(background);
        LinearLayout heading=new LinearLayout(a);heading.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icon=new ImageView(a);icon.setImageDrawable(new LineIcon("steps",amber));heading.addView(icon,new LinearLayout.LayoutParams(a.dp(25),a.dp(25)));
        TextView title=a.title(Lang.t("Твой ритм"),21);title.setPadding(a.dp(10),0,0,0);heading.addView(title,new LinearLayout.LayoutParams(0,-2,1));TextView today=a.text(Lang.t("Сегодня"),12,amber);heading.addView(today);card.addView(heading);a.space(card,20);
        LinearLayout metrics=new LinearLayout(a);metrics.setGravity(Gravity.BOTTOM);
        LinearLayout left=a.column();steps=a.title("",42);steps.setTextColor(amber);steps.setAutoSizeTextTypeUniformWithConfiguration(24,42,1,android.util.TypedValue.COMPLEX_UNIT_SP);steps.setSingleLine(true);left.addView(steps,new LinearLayout.LayoutParams(-1,a.dp(52)));left.addView(a.text(Lang.t("шагов сегодня"),13,a.MUTED));metrics.addView(left,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout right=a.column();right.setGravity(Gravity.RIGHT);distance=a.title("",24);distance.setTextColor(amber);right.addView(distance);a.space(right,5);right.addView(a.text(Lang.t("Примерное расстояние"),11,a.MUTED));metrics.addView(right,new LinearLayout.LayoutParams(0,-2,1));card.addView(metrics);a.space(card,20);
        caption=a.text(Lang.t("ПОСЛЕДНИЕ 7 ДНЕЙ"),11,a.MUTED);card.addView(caption);a.space(card,12);
        chart=new LinearLayout(a);chart.setGravity(Gravity.BOTTOM);card.addView(chart,new LinearLayout.LayoutParams(-1,a.dp(112)));
        a.space(card,12);detail=a.text("",14,a.TEXT);detail.setMinHeight(a.dp(40));card.addView(detail);a.divider(card);
        weekly=a.title("",17);card.addView(weekly);a.space(card,8);total=a.text("",12,a.MUTED);card.addView(total);
        a.hint(card,Lang.t("О данных ходьбы"),Lang.t("График показывает записанные шаги за последние 7 дней. Нажми на день, чтобы увидеть итог. Прочерк означает, что записей нет. Километры приблизительные и рассчитаны по текущей длине шага. Ходьба не добавляет тренировки в календарь."));
        refresh();
    }
    void refresh(){
        LocalDate now=LocalDate.now();if(!shownToday.equals(now)){if(selected.equals(shownToday)||selected.isBefore(now.minusDays(6)))selected=now;shownToday=now;}
        JSONObject data=Steps.data(a);long count=StepData.count(data,now.toString());
        steps.setText(StepsPage.format(count));distance.setText(Steps.distance(a,count));
        long sum=StepData.week(data,now);weekly.setText(Lang.t("За 7 дней: ")+StepsPage.format(sum)+Lang.t(" шагов")+" · "+Steps.distance(a,sum));
        long all=StepData.total(data,now);total.setText(Lang.t("Всего записано: ")+StepsPage.format(all)+Lang.t(" шагов")+" · "+Steps.distance(a,all));
        String next=data.optJSONObject("days")+now.toString()+selected.toString()+Lang.english();
        if(!next.equals(signature)){signature=next;drawChart(data,now);}
        JSONObject days=data.optJSONObject("days");String date=selected.equals(now)?Lang.t("Сегодня"):selected.format(DateTimeFormatter.ofPattern("d MMM",Lang.locale()));
        detail.setText(days!=null&&days.has(selected.toString())?date+" · "+StepsPage.format(StepData.count(data,selected.toString()))+Lang.t(" шагов")+" · "+Steps.distance(a,StepData.count(data,selected.toString())):date+" · "+Lang.t("Нет записей шагов"));
    }
    void drawChart(JSONObject data,LocalDate now){
        chart.removeAllViews();JSONObject days=data.optJSONObject("days");long max=1;for(int i=0;i<7;i++)max=Math.max(max,StepData.count(data,now.minusDays(i).toString()));
        for(int i=6;i>=0;i--){LocalDate day=now.minusDays(i);long count=StepData.count(data,day.toString());boolean recorded=days!=null&&days.has(day.toString()),chosen=selected.equals(day);
            LinearLayout column=a.column();column.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);column.setPadding(a.dp(3),a.dp(4),a.dp(3),a.dp(5));column.setBackground(a.shape(chosen?0xFF493B2D:Color.TRANSPARENT,12));
            FrameLayout track=new FrameLayout(a);View bar=new View(a);bar.setBackground(a.shape(recorded?(chosen?amber:0xFFA87D53):0xFF3B4147,5));int height=count==0?2:Math.max(5,(int)(66.0*count/max));FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(a.dp(17),a.dp(height),Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);track.addView(bar,bp);column.addView(track,new LinearLayout.LayoutParams(-1,a.dp(72)));
            if(!recorded){bar.setVisibility(View.INVISIBLE);TextView missing=a.text("—",13,a.MUTED);missing.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);track.addView(missing,new FrameLayout.LayoutParams(-1,-1));}
            a.space(column,7);String name=day.format(DateTimeFormatter.ofPattern("EE",Lang.locale()));TextView label=a.text(name,11,chosen?amber:a.MUTED);label.setGravity(Gravity.CENTER);column.addView(label);
            column.setContentDescription(day.toString()+" · "+(recorded?StepsPage.format(count)+Lang.t(" шагов"):Lang.t("Нет записей шагов")));column.setSelected(chosen);a.clickable(column,()->{selected=day;signature="";refresh();});chart.addView(column,new LinearLayout.LayoutParams(0,-1,1));
        }
    }
}
