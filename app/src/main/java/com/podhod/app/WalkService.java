package com.podhod.app;
import android.app.*;import android.content.*;import android.content.pm.*;import android.location.*;import android.os.*;import org.json.*;
public final class WalkService extends Service implements LocationListener {
 static boolean running;private LocationManager locations;private final Handler handler=new Handler(Looper.getMainLooper());
 final Runnable tick=new Runnable(){public void run(){JSONObject w=Walks.data(WalkService.this).optJSONObject("active");if(w==null||!permitted(WalkService.this)){Walks.finish(WalkService.this,true);stopSelf();return;}if(w.optJSONArray("points").length()>=10000||System.currentTimeMillis()-w.optLong("started")>=24*3600000L){Walks.finish(WalkService.this,false);stopSelf();return;}show();handler.postDelayed(this,10000);}};
 static boolean permitted(Context c){return c.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;}
 public int onStartCommand(Intent intent,int flags,int id){
  if(intent!=null&&"stop".equals(intent.getAction())){Walks.finish(this,false);stopSelf();return START_NOT_STICKY;}
  if(intent==null||!"start".equals(intent.getAction())||checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){stopSelf();return START_NOT_STICKY;}
  if(running)return START_NOT_STICKY;
  locations=getSystemService(LocationManager.class);try{
   if(!locations.isProviderEnabled(LocationManager.GPS_PROVIDER)){stopSelf();return START_NOT_STICKY;}
   Walks.finish(this,true);Walks.begin(this);show();locations.requestLocationUpdates(LocationManager.GPS_PROVIDER,5000,5,this,Looper.getMainLooper());running=true;handler.postDelayed(tick,10000);
  }catch(RuntimeException e){Walks.finish(this,true);stopSelf();}return START_NOT_STICKY;
 }
 void show(){NotificationManager m=getSystemService(NotificationManager.class);m.createNotificationChannel(new NotificationChannel("walks",Lang.t("Прогулка"),NotificationManager.IMPORTANCE_LOW));Intent open=new Intent(this,MainActivity.class).putExtra("steps",true).putExtra("walk",true).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);PendingIntent content=PendingIntent.getActivity(this,91,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);PendingIntent stop=PendingIntent.getService(this,92,new Intent(this,WalkService.class).setAction("stop"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);JSONObject w=Walks.data(this).optJSONObject("active");Notification n=new Notification.Builder(this,"walks").setSmallIcon(R.drawable.ic_trace_notification).setContentTitle(Lang.t("Запись прогулки")).setContentText(w==null?Lang.t("Ищем GPS…"):Walks.summary(w)).setContentIntent(content).addAction(new Notification.Action.Builder(null,Lang.t("Завершить прогулку"),stop).build()).setOnlyAlertOnce(true).setOngoing(true).build();if(Build.VERSION.SDK_INT>=29)startForeground(91,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);else startForeground(91,n);}
 public void onLocationChanged(Location l){long age=SystemClock.elapsedRealtimeNanos()-l.getElapsedRealtimeNanos();if(age<0||age>30000000000L||!l.hasAccuracy())return;if(!permitted(this)){Walks.finish(this,true);stopSelf();return;}Walks.point(this,l);}
 public void onProviderDisabled(String p){}public void onProviderEnabled(String p){}public void onStatusChanged(String p,int s,Bundle b){}
 public IBinder onBind(Intent i){return null;}
 public void onDestroy(){running=false;handler.removeCallbacksAndMessages(null);if(locations!=null)locations.removeUpdates(this);Walks.finish(this,true);stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();}
}
