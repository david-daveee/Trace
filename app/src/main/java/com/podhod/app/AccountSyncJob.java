package com.podhod.app;

import android.app.job.*;
import android.content.*;

/** Android schedules opportunistic retries while the device has connectivity. */
public final class AccountSyncJob extends JobService {
    private static final int ID=7312;
    static void schedule(Context c){if(c.getSystemService(JobScheduler.class).getPendingJob(ID)!=null)return;c.getSystemService(JobScheduler.class).schedule(new JobInfo.Builder(ID,new ComponentName(c,AccountSyncJob.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPeriodic(15*60*1000L).setPersisted(true).build());}
    static void cancel(Context c){c.getSystemService(JobScheduler.class).cancel(ID);}
    @Override public boolean onStartJob(JobParameters params){
        AccountSync sync=AccountSync.get(this);
        if(sync.user()==null||sync.busy()||sync.prefs().getString("status", "").equals("restored"))return false;
        sync.prepare(new AccountSync.Listener(){public void done(){jobFinished(params,false);}public void error(Exception e){jobFinished(params,true);}},false);return true;
    }
    @Override public boolean onStopJob(JobParameters params){return true;}
}
