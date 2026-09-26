package com.podhod.app;

import android.app.AlertDialog;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;

/** A separate daily walking view; its live updates do not rebuild the activity. */
final class StepsPage {
    final MainActivity a;
    final Ring ring;
    WalkPanel walk;
    final WalkingProgress rhythm;
    final LinearLayout stepsContent,walkContent;
    final TextView stepsTab,walkTab,heading;
    final TextView remaining,status,date,distance;
    StepsPage(MainActivity a){
        this.a=a;
        a.badge(a.body,Lang.t("ДВИЖЕНИЕ КАЖДЫЙ ДЕНЬ"),0xFFFFC88C);a.space(a.body,12);
        LinearLayout tabs=new LinearLayout(a);tabs.setPadding(a.dp(4),a.dp(4),a.dp(4),a.dp(4));tabs.setBackground(a.shape(a.CARD,18));a.body.addView(tabs,new LinearLayout.LayoutParams(-1,-2));
        stepsTab=a.title(Lang.t("Шаги"),16);walkTab=a.title(Lang.t("Прогулка"),16);
        for(TextView tab:new TextView[]{stepsTab,walkTab}){tab.setGravity(Gravity.CENTER);tabs.addView(tab,new LinearLayout.LayoutParams(0,a.dp(48),1));}
        a.clickable(stepsTab,()->select(false));a.clickable(walkTab,()->select(true));a.space(a.body,20);
        heading=a.title(Lang.t("Шаги"),30);a.body.addView(heading);a.space(a.body,6);date=a.text("",15,a.MUTED);a.body.addView(date);a.space(a.body,22);
        stepsContent=a.column();a.body.addView(stepsContent);walkContent=a.column();a.body.addView(walkContent);
        LinearLayout hero=a.card(stepsContent);hero.setBackground(a.shape(0xFF29251F,28));
        status=a.text("",13,0xFFFFC88C);status.setGravity(Gravity.CENTER);hero.addView(status);
        ring=new Ring(a);hero.addView(ring,new LinearLayout.LayoutParams(-1,a.dp(245)));
        distance=a.title("",30);distance.setTextColor(0xFFFFC88C);distance.setGravity(Gravity.CENTER);hero.addView(distance);a.space(hero,6);
        TextView estimate=a.text(Lang.t("Примерное расстояние"),13,a.MUTED);estimate.setGravity(Gravity.CENTER);hero.addView(estimate);a.space(hero,18);
        remaining=a.text("",16,a.TEXT);remaining.setGravity(Gravity.CENTER);hero.addView(remaining);a.space(hero,12);
        TextView goal=a.text(Lang.t("Цель на день")+" · "+format(Steps.goal(a)),14,a.MUTED);goal.setGravity(Gravity.CENTER);hero.addView(goal);
        a.button(hero,Lang.t("Изменить цель"),false,()->{
            EditText input=new EditText(a);input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);input.setText(String.valueOf(Steps.goal(a)));input.setSelectAllOnFocus(true);input.setTextColor(a.TEXT);input.setPadding(a.dp(24),a.dp(16),a.dp(24),a.dp(16));
            AlertDialog dialog=new AlertDialog.Builder(a).setTitle(Lang.t("Цель на день")).setView(input).setPositiveButton(Lang.t("Сохранить"),null).setNegativeButton(Lang.t("Отмена"),null).create();
            dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w->{try{Steps.goal(a,Integer.parseInt(input.getText().toString()));dialog.dismiss();a.render();}catch(IllegalArgumentException e){input.setError(Lang.t("Цель — от 100 до 100 000 шагов"));}}));dialog.show();
        });
        a.link(hero,Lang.t("Длина шага")+" · "+NumberFormat.getNumberInstance(Lang.locale()).format(Steps.stepLength(a))+Lang.t(" см"),()->{
            LinearLayout form=a.form();form.addView(a.text(Lang.t("Расстояние = шаги × длина шага. По умолчанию 70 см — настрой под себя. Для калибровки раздели известное расстояние в сантиметрах на число шагов."),14,a.MUTED));
            EditText input=a.input(form,Lang.t("Длина шага, см"),NumberFormat.getNumberInstance(Lang.locale()).format(Steps.stepLength(a)),true);
            a.formDialog(Lang.t("Длина шага"),form,Lang.t("Сохранить"),()->{double cm;try{cm=Double.parseDouble(input.getText().toString().trim().replace(',','.'));}catch(NumberFormatException e){throw new IllegalArgumentException(Lang.t("Длина шага — от 20 до 200 см"));}Steps.stepLength(a,cm);a.render();});
        });
        rhythm=new WalkingProgress(a,stepsContent);
        LinearLayout controls=a.card(stepsContent);
        if(!Steps.available(a)){
            controls.addView(a.title(Lang.t("Датчик шагов недоступен"),19));a.space(controls,8);controls.addView(a.text(Lang.t("Этот телефон не предоставляет встроенный счётчик шагов."),14,a.MUTED));
        }else{
            boolean active=Steps.enabled(a)&&Steps.permitted(a)&&StepsService.running;
            controls.addView(a.title(Lang.t("Считает твой телефон"),19));a.space(controls,8);
            controls.addView(a.text(Lang.t("Бери телефон с собой — шаги записываются и при закрытом экране."),14,a.MUTED));
            a.button(controls,active?Lang.t("Выключить подсчёт"):Lang.t("Включить подсчёт"),!active,()->{if(active){Steps.disable(a);a.render();}else a.enableSteps();});
        }
        a.hint(stepsContent,Lang.t("Как работает подсчёт"),Lang.t("Подсчёт начинается после включения. Прошлые шаги за сегодня недоступны. После перезагрузки или принудительной остановки открой Trace снова. Система может прерывать фоновую работу; пропущенные шаги не всегда восстановятся. При смене дня показания относятся к дате события датчика. Шаги и настройки входят в полную резервную копию в настройках Trace."));
        select(a.walkTab);refresh();
    }
    void select(boolean walking){
        for(View panel:new View[]{stepsContent,walkContent}){panel.animate().cancel();panel.setTranslationX(0);panel.setAlpha(1);}
        a.walkTab=walking;if(walking&&walk==null)walk=new WalkPanel(a,walkContent);
        stepsContent.setVisibility(walking?View.GONE:View.VISIBLE);walkContent.setVisibility(walking?View.VISIBLE:View.GONE);
        heading.setText(Lang.t(walking?"Прогулка":"Шаги"));
        TextView[] tabs={stepsTab,walkTab};for(int i=0;i<tabs.length;i++){boolean selected=walking==(i==1);tabs[i].setSelected(selected);tabs[i].setTextColor(selected?a.BG:a.MUTED);tabs[i].setBackground(a.shape(selected?a.GREEN:Color.TRANSPARENT,14));}
        if(walking)walk.refresh();
    }
    static String format(long n){return NumberFormat.getIntegerInstance(Lang.locale()).format(n);}
    void refresh(){rhythm.refresh();if(a.walkTab&&walk!=null)walk.refresh();date.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM",Lang.locale())));long count=Steps.today(a);int goal=Steps.goal(a);ring.count=count;ring.goal=goal;ring.invalidate();ring.setContentDescription(format(count)+" / "+format(goal)+" · "+Lang.t("Шаги"));
        distance.setText(Steps.distance(a,count));
        remaining.setText(count>=goal?Lang.t("Цель достигнута"):Lang.t("До цели: ")+format(goal-count));
        status.setText(!Steps.available(a)?Lang.t("Датчик шагов недоступен"):Steps.enabled(a)&&Steps.permitted(a)&&StepsService.running?Lang.t("ПОДСЧЁТ ВКЛЮЧЁН"):Lang.t("ПОДСЧЁТ ВЫКЛЮЧЕН"));
    }
    static final class Ring extends View {
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);long count;int goal;
        Ring(MainActivity a){super(a);}
        @Override protected void onDraw(Canvas c){float density=getResources().getDisplayMetrics().density,w=getWidth(),h=getHeight(),side=Math.min(w,h)-32*density;RectF r=new RectF((w-side)/2,(h-side)/2,(w+side)/2,(h+side)/2);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(10*density);p.setStrokeCap(Paint.Cap.ROUND);p.setColor(0xFF443B30);c.drawOval(r,p);p.setColor(0xFFFFC88C);c.drawArc(r,-90,360*Math.min(1f,(float)count/Math.max(1,goal)),false,p);
            p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));p.setTextSize(42*density);p.setColor(Color.WHITE);String value=format(count);while(p.measureText(value)>side*.76f)p.setTextSize(p.getTextSize()-density);c.drawText(value,w/2,h/2+2*density,p);
            p.setTextSize(13*density);p.setColor(0xFFD3C3B1);c.drawText(Lang.t("шагов сегодня"),w/2,h/2+29*density,p);
        }
    }
}
