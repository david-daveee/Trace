package com.podhod.app;
import android.app.*;
import android.os.*;
import org.json.*;
public class DeviceSmoke extends Instrumentation {
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    Notification.Action find(String text){
        for(var n:getTargetContext().getSystemService(NotificationManager.class).getActiveNotifications())
            if(n.getId()==7)for(Notification.Action a:n.getNotification().actions)if(a.title.toString().contains(text))return a;
        throw new AssertionError("Missing notification action: "+text);
    }
    int cursor(Store store){synchronized(store){return store.active().optInt("cursor");}}
    @Override public void onStart(){
        Bundle result=new Bundle();Store store=Store.get(getTargetContext());JSONObject original=store.active()==null?null:Engine.copy(store.active());
        int code=Activity.RESULT_CANCELED;
        try{
            check(original!=null,"Open a workout first");check(original.optInt("cursor")==0,"Test expects first uncompleted approach");
            runOnMainSync(()->Notices.update(getTargetContext()));
            Notification.Action done=find("Готово");done.actionIntent.send();Thread.sleep(400);check(cursor(store)==1,"Done did not persist one approach");
            done.actionIntent.send();Thread.sleep(300);check(cursor(store)==1,"Stale action counted approach twice");
            check(!store.active().optBoolean("rest"),"Unexpected rest stage");
            find("Назад").actionIntent.send();Thread.sleep(300);check(cursor(store)==0,"Undo failed");
            runOnMainSync(()->{Engine.put(store.active(),"cursor",store.active().optJSONArray("sets").length());Engine.put(store.active(),"revision",store.active().optInt("revision")+1);Notices.update(getTargetContext());});
            check(find("Завершить")!=null,"Finish action missing");
            result.putString("stream","PASS: real notification PendingIntent; stale-action protection; no rest; undo; finish action. Original session restored.\n");code=Activity.RESULT_OK;
        }catch(Throwable t){result.putString("stream","FAIL: "+t+"\n");}
        finally{runOnMainSync(()->{if(original!=null)Engine.put(store.data,"active",original);store.save();Notices.update(getTargetContext());});}
        finish(code,result);
    }
}
