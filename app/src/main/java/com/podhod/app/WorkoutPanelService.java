package com.podhod.app;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.media.MediaMetadata;
import android.media.session.*;
import android.os.*;
import org.json.*;

/** User-started workout controls. No audio playback, audio focus or headset commands. */
public class WorkoutPanelService extends Service {
    static WorkoutPanelService running;
    private MediaSession media;
    private final Handler timer=new Handler(Looper.getMainLooper());
    @Override public void onCreate(){super.onCreate();running=this;
        media=new MediaSession(this,"Trace workout");
        media.setSessionActivity(Notices.open(this));
        media.setMediaButtonReceiver(null);
        media.setCallback(new MediaSession.Callback(){
            @Override public boolean onMediaButtonEvent(Intent intent){return false;}
            @Override public void onPlay(){pause(false);}
            @Override public void onPause(){pause(true);}
            @Override public void onCustomAction(String command,Bundle extras){
                // The revision is part of each button: duplicate or stale presses are ignored.
                String[] token=command.split("\\|",3);if(token.length!=3)return;
                JSONObject s=Store.get(WorkoutPanelService.this).active();
                if(s==null||!s.optString("id").equals(token[1])||!String.valueOf(s.optInt("revision")).equals(token[2]))return;
                if(token[0].equals("open")){try{Notices.open(WorkoutPanelService.this).send();}catch(PendingIntent.CanceledException ignored){}return;}
                Store.get(WorkoutPanelService.this).action(token[1],s.optInt("revision"),token[0]);changed();
            }
        },timer);
    }
    private void pause(boolean desired){JSONObject s=Store.get(this).active();if(s!=null&&s.optBoolean("paused")!=desired){Store.get(this).action(s.optString("id"),s.optInt("revision"),"pause");changed();}}
    private void changed(){Notices.update(this);sendBroadcast(new Intent("com.podhod.app.CHANGED").setPackage(getPackageName()));}
    static String command(JSONObject s,String verb){return verb+"|"+s.optString("id")+"|"+s.optInt("revision");}
    private PlaybackState.CustomAction button(JSONObject s,String verb,String label,int icon){return new PlaybackState.CustomAction.Builder(command(s,verb),label,icon).build();}
    MediaSession.Token sync(JSONObject s){
        JSONArray sets=s.optJSONArray("sets");int done=Engine.doneCount(s),cursor=s.optInt("cursor");
        boolean finished=cursor>=sets.length(),paused=s.optBoolean("paused");
        JSONObject set=finished?null:sets.optJSONObject(cursor);
        String title=finished?Lang.t("Тренировка выполнена"):Lang.content(set.optString("exercise"));
        String weight=finished?Lang.t("Сохрани результат"):set.optDouble("kg",-1)<0?Lang.t("Укажи вес в Trace"):Engine.number(set.optDouble("kg"))+Lang.t(" кг × ")+set.optInt("reps");
        String status=finished?Lang.t("Готово"):paused?Lang.t("Пауза"):Lang.t("Подход ")+set.optInt("ordinal")+"/"+set.optInt("sets");
        String subtitle=finished?Lang.t("Готово ")+done+Lang.t(" из ")+sets.length()+Lang.t(" · сохрани результат"):weight+"  ·  "+status;
        MediaMetadata.Builder metadata=new MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE,title).putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE,title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST,subtitle).putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE,subtitle)
            .putString(MediaMetadata.METADATA_KEY_ALBUM,Lang.content(s.optString("dayName"))+Lang.t(" · выполнено ")+done+Lang.t(" из ")+sets.length())
            .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART,Notices.artwork(this,s))
            .putBitmap(MediaMetadata.METADATA_KEY_ART,Notices.artwork(this,s));
        long position=PlaybackState.PLAYBACK_POSITION_UNKNOWN;
        media.setMetadata(metadata.build());
        PlaybackState.Builder state=new PlaybackState.Builder()
            .setState(paused||finished?PlaybackState.STATE_PAUSED:PlaybackState.STATE_PLAYING,position,0f);
        if(!finished)state.setActions(paused?PlaybackState.ACTION_PLAY:PlaybackState.ACTION_PAUSE);
        Bundle slots=new Bundle();slots.putBoolean("android.media.playback.ALWAYS_RESERVE_SPACE_FOR.ACTION_SKIP_TO_PREVIOUS",done==0);state.setExtras(slots);media.setExtras(slots);
        if(done>0)state.addCustomAction(button(s,"undo",Lang.t("Отменить подход"),R.drawable.notice_undo));
        if(finished)state.addCustomAction(button(s,"open",Lang.t("Сохранить тренировку"),R.drawable.notice_check));
        else if(!paused)state.addCustomAction(button(s,set.optDouble("kg",-1)>=0?"next":"open",set.optDouble("kg",-1)>=0?Lang.t("Закончил подход"):Lang.t("Указать вес"),R.drawable.notice_complete));
        if(!finished)state.addCustomAction(button(s,"open",Lang.t("Все подходы · выполнено ")+done+Lang.t(" из ")+sets.length(),R.drawable.notice_list));
        media.setPlaybackState(state.build());media.setActive(true);
        return media.getSessionToken();
    }
    void show(Notification notification){if(Build.VERSION.SDK_INT>=34)startForeground(7,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);else startForeground(7,notification);}
    @Override public int onStartCommand(Intent i,int flags,int id){Notices.update(this);if(Store.get(this).active()==null)stopSelf();return START_STICKY;}
    @Override public IBinder onBind(Intent i){return null;}
    @Override public void onDestroy(){timer.removeCallbacksAndMessages(null);media.setActive(false);media.release();running=null;super.onDestroy();}
}
