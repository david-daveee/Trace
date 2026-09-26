package com.podhod.app;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.hardware.*;
import android.os.*;
import android.provider.Settings;
import java.time.*;

public final class StepsService extends Service implements SensorEventListener {
    static boolean running;
    private SensorManager sensors;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private long notified;
    private final Runnable refresh=new Runnable(){public void run(){if(!Steps.enabled(StepsService.this)||!Steps.permitted(StepsService.this)){stopSelf();return;}show();handler.postDelayed(this,30000);}};
    @Override public void onCreate(){super.onCreate();sensors=getSystemService(SensorManager.class);}
    @Override public int onStartCommand(Intent i,int flags,int id){
        if(!Steps.enabled(this)||!Steps.permitted(this)||!Steps.available(this)){stopSelf();return START_NOT_STICKY;}
        show();
        if(!running){
            try{running=sensors.registerListener(this,sensors.getDefaultSensor(Sensor.TYPE_STEP_COUNTER),SensorManager.SENSOR_DELAY_NORMAL,0);}
            catch(SecurityException e){running=false;}
            if(!running){stopSelf();return START_NOT_STICKY;}
        }
        handler.removeCallbacks(refresh);handler.postDelayed(refresh,30000);return START_STICKY;
    }
    private void show(){
        NotificationManager manager=getSystemService(NotificationManager.class);
        NotificationChannel channel=new NotificationChannel("steps",Lang.t("Шаги за сегодня"),NotificationManager.IMPORTANCE_LOW);channel.setSound(null,null);manager.createNotificationChannel(channel);
        Intent open=new Intent(this,MainActivity.class).putExtra("steps",true).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pending=PendingIntent.getActivity(this,81,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification n=new Notification.Builder(this,"steps").setSmallIcon(R.drawable.ic_trace_notification).setContentTitle(Lang.t("Шаги за сегодня")+": "+Steps.today(this))
            .setContentText(Steps.distance(this,Steps.today(this))+" · "+Lang.t("Цель на день")+": "+Steps.goal(this)).setContentIntent(pending).setOngoing(true).setOnlyAlertOnce(true).setShowWhen(false).setCategory(Notification.CATEGORY_PROGRESS).build();
        if(Build.VERSION.SDK_INT>=34)startForeground(81,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH);else startForeground(81,n);
        notified=SystemClock.elapsedRealtime();
    }
    @Override public void onSensorChanged(SensorEvent event){
        if(!Steps.enabled(this)||!Steps.permitted(this)){stopSelf();return;}
        long age=Math.max(0,SystemClock.elapsedRealtimeNanos()-event.timestamp);
        long wall=System.currentTimeMillis()-age/1000000L;
        String date=Instant.ofEpochMilli(wall).atZone(ZoneId.systemDefault()).toLocalDate().toString();
        Steps.record(this,(long)event.values[0],Settings.Global.getInt(getContentResolver(),Settings.Global.BOOT_COUNT,0),event.timestamp,date);
        if(SystemClock.elapsedRealtime()-notified>=10000)show();
    }
    @Override public void onAccuracyChanged(Sensor sensor,int accuracy){}
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public void onDestroy(){running=false;handler.removeCallbacksAndMessages(null);if(sensors!=null)sensors.unregisterListener(this);stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();}
}
