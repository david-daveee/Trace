package com.podhod.app;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.hardware.*;
import android.os.Build;
import org.json.*;
import java.time.LocalDate;

public final class Steps {
    static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("trace-steps",0);}
    public static boolean available(Context c){SensorManager m=c.getSystemService(SensorManager.class);return m!=null&&m.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)!=null;}
    public static boolean permitted(Context c){return Build.VERSION.SDK_INT<29||c.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION)==PackageManager.PERMISSION_GRANTED;}
    public static boolean enabled(Context c){return prefs(c).getBoolean("enabled",false);}
    public static int goal(Context c){return prefs(c).getInt("goal",8000);}
    public static double stepLength(Context c){return prefs(c).getFloat("stepLengthCm",70f);}
    public static void stepLength(Context c,double cm){if(!Double.isFinite(cm)||cm<20||cm>200)throw new IllegalArgumentException(Lang.t("Длина шага — от 20 до 200 см"));prefs(c).edit().putFloat("stepLengthCm",(float)cm).apply();}
    public static String distance(Context c,long steps){java.text.NumberFormat f=java.text.NumberFormat.getNumberInstance(Lang.locale());f.setMinimumFractionDigits(2);f.setMaximumFractionDigits(2);return "≈ "+f.format(steps*stepLength(c)/100000.0)+Lang.t(" км");}
    public static void goal(Context c,int n){if(n<100||n>100000)throw new IllegalArgumentException(Lang.t("Цель — от 100 до 100 000 шагов"));prefs(c).edit().putInt("goal",n).apply();}
    static synchronized JSONObject data(Context c){return Engine.obj(prefs(c).getString("data","{}"));}
    public static long today(Context c){return StepData.count(data(c),LocalDate.now().toString());}
    public static synchronized void enable(Context c){if(!enabled(c)){JSONObject d=data(c);StepData.resetBaseline(d);prefs(c).edit().putString("data",d.toString()).putBoolean("enabled",true).commit();}resume(c);}
    public static void resume(Context c){if(enabled(c)&&available(c)&&permitted(c)&&!StepsService.running)c.startForegroundService(new Intent(c,StepsService.class));}
    public static synchronized void disable(Context c){JSONObject d=data(c);StepData.resetBaseline(d);prefs(c).edit().putString("data",d.toString()).putBoolean("enabled",false).commit();c.stopService(new Intent(c,StepsService.class));}
    static synchronized void record(Context c,long counter,int boot,long nanos,String day){JSONObject d=data(c);if(StepData.accept(d,counter,boot,nanos,day))prefs(c).edit().putString("data",d.toString()).commit();}
}
