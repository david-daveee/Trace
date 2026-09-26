package com.podhod.app;
import android.app.job.*;
public final class AutoBackupService extends JobService {
 @Override public boolean onStartJob(JobParameters params){new Thread(()->{boolean ok=AutoBackup.run(getApplicationContext());jobFinished(params,!ok);},"trace-auto-backup").start();return true;}
 @Override public boolean onStopJob(JobParameters params){return true;}
}
