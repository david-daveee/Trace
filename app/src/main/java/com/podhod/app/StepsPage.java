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
    final TextView remaining,status,date;
    StepsPage(MainActivity a){
        this.a=a;
        a.badge(a.body,Lang.t("ДВИЖЕНИЕ КАЖДЫЙ ДЕНЬ"),0xFFFFC88C);a.space(a.body,12);
        a.body.addView(a.title(Lang.t("Шаги"),30));a.space(a.body,6);date=a.text("",15,a.MUTED);a.body.addView(date);a.space(a.body,22);
        LinearLayout hero=a.card(a.body);hero.setBackground(a.shape(0xFF29251F,28));
        status=a.text("",13,0xFFFFC88C);status.setGravity(Gravity.CENTER);hero.addView(status);
        ring=new Ring(a);hero.addView(ring,new LinearLayout.LayoutParams(-1,a.dp(245)));
        remaining=a.text("",16,a.TEXT);remaining.setGravity(Gravity.CENTER);hero.addView(remaining);a.space(hero,12);
        TextView goal=a.text(Lang.t("Цель на день")+" · "+format(Steps.goal(a)),14,a.MUTED);goal.setGravity(Gravity.CENTER);hero.addView(goal);
        a.button(hero,Lang.t("Изменить цель"),false,()->{
            EditText input=new EditText(a);input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);input.setText(String.valueOf(Steps.goal(a)));input.setSelectAllOnFocus(true);input.setTextColor(a.TEXT);input.setPadding(a.dp(24),a.dp(16),a.dp(24),a.dp(16));
            AlertDialog dialog=new AlertDialog.Builder(a).setTitle(Lang.t("Цель на день")).setView(input).setPositiveButton(Lang.t("Сохранить"),null).setNegativeButton(Lang.t("Отмена"),null).create();
            dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w->{try{Steps.goal(a,Integer.parseInt(input.getText().toString()));dialog.dismiss();a.render();}catch(IllegalArgumentException e){input.setError(Lang.t("Цель — от 100 до 100 000 шагов"));}}));dialog.show();
        });
        LinearLayout controls=a.card(a.body);
        if(!Steps.available(a)){
            controls.addView(a.title(Lang.t("Датчик шагов недоступен"),19));a.space(controls,8);controls.addView(a.text(Lang.t("Этот телефон не предоставляет встроенный счётчик шагов."),14,a.MUTED));
        }else{
            boolean active=Steps.enabled(a)&&Steps.permitted(a)&&StepsService.running;
            controls.addView(a.title(Lang.t("Считает твой телефон"),19));a.space(controls,8);
            controls.addView(a.text(Lang.t("Бери телефон с собой — шаги записываются и при закрытом экране."),14,a.MUTED));
            a.button(controls,active?Lang.t("Выключить подсчёт"):Lang.t("Включить подсчёт"),!active,()->{if(active){Steps.disable(a);a.render();}else a.enableSteps();});
        }
        a.hint(a.body,Lang.t("Как работает подсчёт"),Lang.t("Подсчёт начинается после включения. Прошлые шаги за сегодня недоступны. После перезагрузки или принудительной остановки открой Trace снова. Система может прерывать фоновую работу; пропущенные шаги не всегда восстановятся. При смене дня показания относятся к дате события датчика. Шаги хранятся отдельно на этом телефоне и пока не входят в резервную копию тренировок."));
        refresh();
    }
    static String format(long n){return NumberFormat.getIntegerInstance(Lang.locale()).format(n);}
    void refresh(){date.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM",Lang.locale())));long count=Steps.today(a);int goal=Steps.goal(a);ring.count=count;ring.goal=goal;ring.invalidate();ring.setContentDescription(format(count)+" / "+format(goal)+" · "+Lang.t("Шаги"));
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
