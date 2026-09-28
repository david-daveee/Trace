package com.podhod.app;
import android.graphics.*;
import android.view.View;
import org.json.*;
final class WalkVisuals {
 static long seconds(JSONObject w,long now){return w==null?0:Math.max(0,(w.optLong("ended",now)-w.optLong("started"))/1000);}
 static String time(JSONObject w,long now){long s=seconds(w,now);return s>=3600?String.format(java.util.Locale.US,"%d:%02d:%02d",s/3600,s/60%60,s%60):String.format(java.util.Locale.US,"%d:%02d",s/60,s%60);}
 static String pace(JSONObject w,long now){if(w==null||w.optDouble("meters")<50||seconds(w,now)==0)return "—";long s=Math.round(seconds(w,now)*1000/w.optDouble("meters"));return String.format(java.util.Locale.US,"%d:%02d",s/60,s%60);}
 static final class Route extends View {
  final double[] speeds;final JSONObject walk;final Paint p=new Paint(3);
  Route(MainActivity a,JSONObject walk){super(a);this.walk=walk;speeds=WalkSpeed.speeds(walk.optJSONArray("points"));setBackground(a.shape(0xFF101A25,16));setContentDescription(Lang.t("Миниатюра маршрута"));}
  protected void onDraw(Canvas c){super.onDraw(c);JSONArray points=walk.optJSONArray("points");if(points==null||points.length()==0)return;int n=points.length();double[] xs=new double[n],ys=new double[n];double minX=1e9,maxX=-1e9,minY=1e9,maxY=-1e9;for(int i=0;i<n;i++){JSONArray a=points.optJSONArray(i);double x=(a.optDouble(1)+180)/360*256;if(i>0){while(x-xs[i-1]>128)x-=256;while(x-xs[i-1]<-128)x+=256;}xs[i]=x;ys[i]=WalkMap.y(a.optDouble(0));minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,ys[i]);maxY=Math.max(maxY,ys[i]);}float pad=16*getResources().getDisplayMetrics().density;double scale=Math.min((getWidth()-2*pad)/Math.max(.00001,maxX-minX),(getHeight()-2*pad)/Math.max(.00001,maxY-minY));float[] routeX=new float[n],routeY=new float[n];Path path=new Path();float lastX=0,lastY=0;for(int i=0;i<n;i++){lastX=(float)((xs[i]-(minX+maxX)/2)*scale+getWidth()/2);lastY=(float)((ys[i]-(minY+maxY)/2)*scale+getHeight()/2);routeX[i]=lastX;routeY[i]=lastY;if(i==0||points.optJSONArray(i).optBoolean(4))path.moveTo(lastX,lastY);else path.lineTo(lastX,lastY);}p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3*getResources().getDisplayMetrics().density);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);WalkRoutePaint.draw(c,p,points,routeX,routeY,speeds,3*getResources().getDisplayMetrics().density);p.setStyle(Paint.Style.FILL);p.setColor(0xFFF4F7FB);c.drawCircle(lastX,lastY,4*getResources().getDisplayMetrics().density,p);}
 }
}
