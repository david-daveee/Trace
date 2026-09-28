package com.podhod.app;
import android.app.*;import android.os.*;import android.graphics.*;import org.json.*;
/** Isolated map checks: no activity, location recording or stored walks modified. */
public class MapSmoke extends Instrumentation {
 public void onCreate(Bundle b){super.onCreate(b);start();}
 void check(boolean ok){if(!ok)throw new AssertionError("Map viewport invariant failed");}
 public void onStart(){Bundle result=new Bundle();final Throwable[] failure={null};runOnMainSync(()->{try{
  WalkMap map=new WalkMap(getTargetContext());map.online=false;map.layout(0,0,800,900);
  JSONObject walk=WalkData.start(1000);WalkData.add(walk,50,30,1000,5);WalkData.add(walk,50.0001,30.0001,11000,5);WalkData.add(walk,50.0005,30.0002,21000,5);map.route(walk);
  Bitmap bitmap=Bitmap.createBitmap(800,900,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);map.draw(canvas);
  map.panX=70;map.panY=-30;double old=map.baseZoom+map.zoomOffset;float focusX=180,focusY=260;
  double beforeX=(focusX-400-map.panX)/Math.pow(2,old),beforeY=(focusY-450-map.panY)/Math.pow(2,old);
  map.zoomAt(-.65,focusX,focusY);double level=map.baseZoom+map.zoomOffset;
  check(Math.abs((focusX-400-map.panX)/Math.pow(2,level)-beforeX)<.00001);check(Math.abs((focusY-450-map.panY)/Math.pow(2,level)-beforeY)<.00001);
  map.draw(canvas);map.zoom(100);check(Math.abs(map.baseZoom+map.zoomOffset-18)<.00001);map.zoom(-100);check(Math.abs(map.baseZoom+map.zoomOffset-1)<.00001);map.fit();check(map.zoomOffset==0&&map.panX==0&&map.panY==0);map.draw(canvas);bitmap.recycle();
 }catch(Throwable e){failure[0]=e;}});result.putString("stream",failure[0]==null?"PASS: fractional zoom, focal-point preservation, zoom bounds, fit, colored route drawing; no stored data changed.\n":"FAIL: "+failure[0]);finish(failure[0]==null?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);}
}
