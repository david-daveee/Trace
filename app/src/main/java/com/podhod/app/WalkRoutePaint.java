package com.podhod.app;
import android.graphics.*;
import org.json.JSONArray;

final class WalkRoutePaint {
 static void draw(Canvas c,Paint paint,JSONArray points,float[] xs,float[] ys,double[] speeds,float width){
  paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeWidth(width);
  for(int i=1;i<xs.length;i++){
   if(points.optJSONArray(i).optBoolean(4))continue;
   if(xs[i]==xs[i-1]&&ys[i]==ys[i-1])continue;
   int end=WalkSpeed.color(speeds[i]);
   int start=Double.isFinite(speeds[i])&&Double.isFinite(speeds[i-1])?WalkSpeed.color(speeds[i-1]):end;
   paint.setShader(new LinearGradient(xs[i-1],ys[i-1],xs[i],ys[i],start,end,Shader.TileMode.CLAMP));
   c.drawLine(xs[i-1],ys[i-1],xs[i],ys[i],paint);
  }
  paint.setShader(null);paint.setStyle(Paint.Style.FILL);
 }
}
