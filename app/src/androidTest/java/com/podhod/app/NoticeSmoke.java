package com.podhod.app;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import org.json.*;
public class NoticeSmoke extends Instrumentation {
    public void onCreate(Bundle b){super.onCreate(b);start();}
    void main(Runnable work){final Throwable[] error={null};runOnMainSync(()->{try{work.run();}catch(Throwable e){error[0]=e;}});if(error[0]!=null)throw new AssertionError(error[0]);}
    void check(boolean b,String text){if(!b)throw new AssertionError(text);}
    void capture(JSONObject s,boolean expanded,String file){final Throwable[] error={null};runOnMainSync(()->{try{
        Context c=getTargetContext();View v=Notices.player(c,s,expanded).apply(c,new FrameLayout(c));
        float d=c.getResources().getDisplayMetrics().density;int width=(int)(340*d);
        v.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(Math.round((expanded?200:48)*d),View.MeasureSpec.EXACTLY));v.layout(0,0,width,v.getMeasuredHeight());
        check(v.getHeight()<=Math.round((expanded?252:48)*d),"Notification exceeds Android height limit");
        check(v.findViewById(R.id.notice_next).getHeight()>=Math.round(48*d),"Touch target too small");
        Bitmap b=Bitmap.createBitmap(width,v.getHeight(),Bitmap.Config.ARGB_8888);v.draw(new Canvas(b));
        try(FileOutputStream out=c.openFileOutput(file,0)){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();
    }catch(Throwable e){error[0]=e;}});if(error[0]!=null)throw new AssertionError(error[0]);}
    void click(JSONObject s,String verb)throws Exception{Notices.action(getTargetContext(),s,verb).send();waitForIdleSync();Thread.sleep(200);}
    public void onStart(){Store st=Store.get(getTargetContext());
        JSONObject original=Engine.copy(st.data);Bundle report=new Bundle();int code=Activity.RESULT_CANCELED;
        try{
            try(FileOutputStream backup=getTargetContext().openFileOutput("notice-original.json",0)){backup.write(original.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}
            startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            JSONObject p=Zlat.create();Engine.put(p,"id","notice-smoke");Zlat.configure(p,20,40,3);
            main(()->{st.data.optJSONArray("programs").put(p);st.start(p);Notices.update(getTargetContext());});
            JSONObject s=st.active();capture(s,false,"notice-small.png");capture(s,true,"notice-large.png");
            waitForIdleSync();Thread.sleep(500);
            android.media.session.MediaSession.Token token=null;
            for(var n:getTargetContext().getSystemService(NotificationManager.class).getActiveNotifications())if(n.getId()==7)token=n.getNotification().extras.getParcelable(Notification.EXTRA_MEDIA_SESSION);
            check(token!=null,"Missing system media panel token");
            android.media.session.MediaController controller=new android.media.session.MediaController(getTargetContext(),token);
            String next=WorkoutPanelService.command(s,"next");
            controller.getTransportControls().sendCustomAction(next,null);waitForIdleSync();Thread.sleep(200);
            check(Engine.doneCount(s)==1&&!s.optBoolean("rest"),"Media panel next failed");
            controller.getTransportControls().sendCustomAction(next,null);waitForIdleSync();Thread.sleep(200);
            check(Engine.doneCount(s)==1&&!s.optBoolean("rest"),"Media stale command changed workout");
            controller.getTransportControls().pause();waitForIdleSync();Thread.sleep(200);check(s.optBoolean("paused"),"Media pause failed");
            controller.getTransportControls().pause();waitForIdleSync();Thread.sleep(100);check(s.optBoolean("paused"),"Repeated pause resumed workout");
            controller.getTransportControls().play();waitForIdleSync();Thread.sleep(200);check(!s.optBoolean("paused"),"Media resume failed");
            int before=Engine.doneCount(s);
            controller.dispatchMediaButtonEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_MEDIA_NEXT));
            controller.dispatchMediaButtonEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_MEDIA_NEXT));
            waitForIdleSync();Thread.sleep(100);check(Engine.doneCount(s)==before,"Headset button changed workout");
            controller.getTransportControls().sendCustomAction(WorkoutPanelService.command(s,"undo"),null);waitForIdleSync();Thread.sleep(200);check(Engine.doneCount(s)==0,"Media undo failed");
            PendingIntent stale=Notices.action(getTargetContext(),s,"next");click(s,"next");check(Engine.doneCount(s)==1&&!s.optBoolean("rest"),"Next receiver failed");
            stale.send();waitForIdleSync();Thread.sleep(200);check(Engine.doneCount(s)==1&&!s.optBoolean("rest"),"Double click not deduplicated");
            click(s,"next");check(Engine.doneCount(s)==2&&s.optInt("cursor")==2,"Second tap must complete next set immediately");
            click(s,"undo");check(Engine.doneCount(s)==1,"Undo second set failed");
            capture(s,true,"notice-next.png");click(s,"pause");check(s.optBoolean("paused"),"Pause failed");capture(s,true,"notice-paused.png");
            click(s,"pause");check(!s.optBoolean("paused"),"Resume failed");click(s,"undo");check(Engine.doneCount(s)==0&&!s.optBoolean("rest"),"Undo failed");
            main(()->{for(int n=0;n<s.optJSONArray("sets").length();n++)st.setDone(s.optString("id"),s.optInt("revision"),n,true);Notices.update(getTargetContext());});
            capture(s,true,"notice-finished.png");click(s,"finish");check(st.active()!=null,"Zlat confirmation bypassed");
            report.putString("stream","PASS: system MediaSession next, stale command guard, idempotent pause/resume, undo, ignored headset buttons; fallback layouts and finish confirmation. Original data restored.\n");code=Activity.RESULT_OK;
        }catch(Throwable e){report.putString("stream","FAIL: "+e+"\n");}
        finally{main(()->{st.data=original;st.save();Notices.update(getTargetContext());});}finish(code,report);
    }
}

