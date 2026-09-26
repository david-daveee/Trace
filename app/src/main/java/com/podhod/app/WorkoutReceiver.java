package com.podhod.app;
import android.content.*;
public class WorkoutReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i) {
        Store.get(c).action(i.getStringExtra("session"),i.getIntExtra("revision",-1),i.getAction());
        Notices.update(c);
        c.sendBroadcast(new Intent("com.podhod.app.CHANGED").setPackage(c.getPackageName()));
    }
}
