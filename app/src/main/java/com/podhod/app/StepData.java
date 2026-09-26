package com.podhod.app;

import org.json.*;

/** Counter deltas only: never treat the device's lifetime total as today's steps. */
public final class StepData {
    public static void resetBaseline(JSONObject data){data.remove("counter");data.remove("eventNanos");}
    public static boolean accept(JSONObject data,long counter,int boot,long eventNanos,String day){
        if(counter<0||eventNanos<0)return false;
        boolean sameBoot=data.optInt("boot",-1)==boot;
        if(sameBoot&&data.has("eventNanos")&&eventNanos<=data.optLong("eventNanos"))return false;
        long delta=sameBoot&&data.has("counter")?Math.max(0,counter-data.optLong("counter")):0;
        // A counter decrease indicates a reset even on devices without a reliable boot count.
        if(data.has("counter")&&counter<data.optLong("counter"))delta=0;
        JSONObject days=data.optJSONObject("days");if(days==null){days=new JSONObject();Engine.put(data,"days",days);}
        Engine.put(days,day,days.optLong(day)+delta);
        Engine.put(data,"counter",counter);Engine.put(data,"boot",boot);Engine.put(data,"eventNanos",eventNanos);
        return true;
    }
    public static long count(JSONObject data,String day){JSONObject days=data.optJSONObject("days");return days==null?0:days.optLong(day);}
}
