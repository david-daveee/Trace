package com.podhod.app;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import org.json.*;
public final class Notices {
    static PendingIntent action(Context c,JSONObject s,String verb) {
        Intent i=new Intent(c,WorkoutReceiver.class).setAction(verb);
        i.setData(android.net.Uri.parse("podhod://"+s.optString("id")+"/"+s.optInt("revision")+"/"+verb));
        i.putExtra("session",s.optString("id")).putExtra("revision",s.optInt("revision"));
        return PendingIntent.getBroadcast(c,0,i,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
    }
    private static final android.util.SparseArray<android.graphics.Bitmap> covers=new android.util.SparseArray<>();
    static PendingIntent open(Context c) {
        return PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class).putExtra("workout",true)
            .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
    }
    static android.widget.RemoteViews player(Context c,JSONObject s,boolean expanded) {
        android.widget.RemoteViews v=new android.widget.RemoteViews(c.getPackageName(),expanded?R.layout.notice_large:R.layout.notice_small);
        JSONArray sets=s.optJSONArray("sets");int cursor=s.optInt("cursor"),done=Engine.doneCount(s);
        boolean finished=cursor>=sets.length(),paused=s.optBoolean("paused");
        JSONObject set=finished?null:sets.optJSONObject(cursor);
        boolean missing=!finished&&set.optDouble("kg",-1)<0;
        String weight=finished?Lang.t("Сохрани результат занятия"):missing?Lang.t("Укажи вес в приложении"):Engine.number(set.optDouble("kg"))+Lang.t(" кг  ×  ")+set.optInt("reps")+Lang.t(" повт.");
        v.setTextViewText(R.id.notice_title,finished?Lang.t("Тренировка выполнена"):Lang.content(set.optString("exercise")));
        v.setTextViewText(R.id.notice_weight,weight);
        v.setImageViewBitmap(R.id.notice_art,artwork(c,s));
        boolean zlat=finished&&Zlat.is(Store.get(c).program(s.optString("programId")));
        String nextLabel=finished?(zlat?Lang.t("Указать повторы и сохранить"):Lang.t("Сохранить тренировку")):paused?Lang.t("Продолжить тренировку"):missing?Lang.t("Указать рабочий вес"):Lang.t("Подход выполнен");
        PendingIntent next=finished?(zlat?open(c):action(c,s,"finish")):paused?action(c,s,"pause"):missing?open(c):action(c,s,"next");
        v.setImageViewResource(R.id.notice_next,finished?R.drawable.notice_check:paused?R.drawable.notice_play:R.drawable.notice_complete);
        v.setContentDescription(R.id.notice_next,nextLabel);v.setOnClickPendingIntent(R.id.notice_next,next);
        if(expanded) {
            String status=finished?Lang.t("ГОТОВО"):paused?Lang.t("ПАУЗА"):(Lang.t("ПОДХОД ")+set.optInt("ordinal")+" / "+set.optInt("sets"));
            v.setTextViewText(R.id.notice_status,status+"  ·  "+Lang.content(s.optString("dayName")));
            v.setTextViewText(R.id.notice_count,done+Lang.t(" из ")+sets.length()+Lang.t(" подходов"));
            v.setProgressBar(R.id.notice_progress,sets.length(),done,false);
            v.setContentDescription(R.id.notice_progress,Lang.t("Выполнено ")+done+Lang.t(" из ")+sets.length()+Lang.t(" подходов"));
            v.setContentDescription(R.id.notice_undo,Lang.t("Отменить последний выполненный подход"));
            v.setBoolean(R.id.notice_undo,"setEnabled",done>0);v.setFloat(R.id.notice_undo,"setAlpha",done>0?1f:0.3f);
            if(done>0)v.setOnClickPendingIntent(R.id.notice_undo,action(c,s,"undo"));
            v.setViewVisibility(R.id.notice_pause,finished?android.view.View.GONE:android.view.View.VISIBLE);
            v.setImageViewResource(R.id.notice_pause,paused?R.drawable.notice_play:R.drawable.notice_pause);
            v.setContentDescription(R.id.notice_pause,paused?Lang.t("Продолжить"):Lang.english()?"Pause":Lang.t("Пауза"));v.setOnClickPendingIntent(R.id.notice_pause,action(c,s,"pause"));
        }
        return v;
    }
    static android.graphics.Bitmap artwork(Context c,JSONObject session) {
        JSONObject p=Store.get(c).program(session.optString("programId"));
        int resource=Zlat.is(p)?R.drawable.street_cover:R.drawable.strength_cover;
        android.graphics.Bitmap image=covers.get(resource);
        if(image==null){android.graphics.BitmapFactory.Options o=new android.graphics.BitmapFactory.Options();o.inSampleSize=2;image=android.graphics.BitmapFactory.decodeResource(c.getResources(),resource,o);covers.put(resource,image);}return image;
    }
    public static void update(Context c) {
        NotificationManager nm=c.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("workout",Lang.t("Текущая тренировка"),NotificationManager.IMPORTANCE_LOW));
        JSONObject s=Store.get(c).active();if(s==null){c.stopService(new Intent(c,WorkoutPanelService.class));nm.cancel(7);return;}
        if(Build.VERSION.SDK_INT>=33 && c.checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=PackageManager.PERMISSION_GRANTED)return;
        JSONArray sets=s.optJSONArray("sets");int cursor=s.optInt("cursor");boolean finished=cursor>=sets.length();JSONObject set=finished?null:sets.optJSONObject(cursor);
        String title=finished?Lang.t("Тренировка выполнена"):Lang.content(set.optString("exercise"));
        String line=finished?Lang.t("Сохрани тренировку в историю"):(set.optDouble("kg",-1)<0?Lang.t("Укажи вес в приложении"):Engine.number(set.optDouble("kg"))+Lang.t(" кг × ")+set.optInt("reps"))+Lang.t(" · готово ")+Engine.doneCount(s)+"/"+sets.length();
        Notification.Builder b=new Notification.Builder(c,"workout").setSmallIcon(R.drawable.ic_trace_notification).setContentTitle(title).setContentText(line)
            .setStyle(new Notification.DecoratedCustomViewStyle()).setCustomContentView(player(c,s,false)).setCustomBigContentView(player(c,s,true))
            .setColor(0xFF23856B).setShowWhen(false).setOnlyAlertOnce(true).setOngoing(true).setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(open(c));
        WorkoutPanelService service=WorkoutPanelService.running;
        if(service!=null){
            b.setCustomContentView(null).setCustomBigContentView(null).setLargeIcon(artwork(c,s));
            b.addAction(new Notification.Action.Builder(R.drawable.notice_undo,Lang.t("Отменить"),action(c,s,"undo")).build());
            b.addAction(new Notification.Action.Builder(s.optBoolean("paused")?R.drawable.notice_play:R.drawable.notice_pause,s.optBoolean("paused")?Lang.t("Продолжить"):Lang.english()?"Pause":Lang.t("Пауза"),action(c,s,"pause")).build());
            b.addAction(new Notification.Action.Builder(R.drawable.notice_complete,finished?Lang.t("Сохранить"):Lang.t("Подход выполнен"),finished?open(c):s.optBoolean("paused")?action(c,s,"pause"):set.optDouble("kg",-1)<0?open(c):action(c,s,"next")).build());
            b.setStyle(new Notification.MediaStyle().setMediaSession(service.sync(s)).setShowActionsInCompactView(0,1,2));
            service.show(b.build());
        }else{
            nm.notify(7,b.build());
            try{c.startForegroundService(new Intent(c,WorkoutPanelService.class));}
            catch(IllegalStateException e){android.util.Log.w("Trace","Panel will start when Trace is opened",e);}
        }
    }
}
