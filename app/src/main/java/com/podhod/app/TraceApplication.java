package com.podhod.app;
public final class TraceApplication extends android.app.Application { @Override public void onCreate(){super.onCreate();SyncRecovery.recover(this);Lang.init(this);} }
