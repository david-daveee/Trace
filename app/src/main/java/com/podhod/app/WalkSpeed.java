package com.podhod.app;
import org.json.JSONArray;
import java.util.Arrays;

/** Speeds in m/s from recorded fixes; never bridge missing GPS intervals. */
final class WalkSpeed {
 static final int UNKNOWN=0xFF8292A6;
 static double[] speeds(JSONArray points){
  int n=points==null?0:points.length();double[] result=new double[n];Arrays.fill(result,Double.NaN);
  double previous=Double.NaN;
  for(int i=1;i<n;i++){
   JSONArray a=points.optJSONArray(i-1),b=points.optJSONArray(i);
   if(a==null||b==null||b.optBoolean(4)||a.optLong(2)<=0||b.optLong(2)<=a.optLong(2)){previous=Double.NaN;continue;}
   double dt=(b.optLong(2)-a.optLong(2))/1000.0;
   double speed=WalkData.distance(a.optDouble(0),a.optDouble(1),b.optDouble(0),b.optDouble(1))/dt;
   if(dt>120||!Double.isFinite(speed)||speed>12){previous=Double.NaN;continue;}
   // Time-based smoothing reduces GPS flicker without depending on sample frequency.
   double alpha=1-Math.exp(-dt/12);
   result[i]=Double.isNaN(previous)?speed:previous+alpha*(speed-previous);
   previous=result[i];
  }
  return result;
 }
 static int color(double speed){
  if(!Double.isFinite(speed))return UNKNOWN;
  double t=Math.max(0,Math.min(1,speed*3.6/12));
  return t<.5?mix(0xFF27B9CD,0xFFF2BD56,t*2):mix(0xFFF2BD56,0xFFF06462,(t-.5)*2);
 }
 static int mix(int a,int b,double t){int c=0xFF000000;for(int shift:new int[]{16,8,0})c|=((int)Math.round(((a>>shift)&255)*(1-t)+((b>>shift)&255)*t))<<shift;return c;}
}
