package com.podhod.app;
public final class TraceApplication extends android.app.Application { @Override public void onCreate(){super.onCreate();Lang.init(this);} }
