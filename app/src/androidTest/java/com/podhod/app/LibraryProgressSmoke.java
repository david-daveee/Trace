package com.podhod.app;
import android.app.*;import android.content.*;import android.os.*;import android.view.*;import android.graphics.*;import java.io.*;import org.json.*;
public class LibraryProgressSmoke extends Instrumentation {
 public void onCreate(Bundle b){super.onCreate(b);start();}
 void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 void main(Runnable r){final Throwable[] e={null};runOnMainSync(()->{try{r.run();}catch(Throwable t){e[0]=t;}});if(e[0]!=null)throw new AssertionError(e[0]);}
 void capture(MainActivity a,String page,String filename){main(()->{try{a.page=page;a.render();View v=a.root;int w=a.getResources().getDisplayMetrics().widthPixels,h=a.getResources().getDisplayMetrics().heightPixels;v.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));v.layout(0,0,w,h);Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);v.draw(new Canvas(b));try(FileOutputStream out=getTargetContext().openFileOutput(filename,0)){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();}catch(Exception e){throw new RuntimeException(e);}});}
 public void onStart(){Store store=Store.get(getTargetContext());JSONObject original=Engine.copy(store.data);Bundle result=new Bundle();int code=Activity.RESULT_CANCELED;MainActivity a=null;
 try{try(FileOutputStream out=getTargetContext().openFileOutput("library-progress-original.json",0)){out.write(original.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}
 a=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
 capture(a,"progress","progress-real.png");capture(a,"mine","mine-unified.png");capture(a,"library","library-covers.png");capture(a,"workout","workout-cover.png");
 JSONObject p=Zlat.create();Engine.put(p,"id","library-progress-test");Engine.put(p,"mine",true);Zlat.configure(p,20,40,3);
 main(()->{store.data.optJSONArray("programs").put(p);store.start(p);JSONObject active=store.active();store.setDone(active.optString("id"),active.optInt("revision"),0,true);int history=store.data.optJSONArray("history").length();store.removeFromMine(p);check(!p.optBoolean("mine"),"Still in mine");check(store.active()==null,"Removed program still active");check(store.program(p.optString("id"))!=null,"Catalog plan lost");check(store.data.optJSONArray("history").length()==history,"History changed");check(Engine.doneCount(store.sessionFor(p))==1,"Saved marks lost");Engine.put(p,"mine",true);store.start(p);check(Engine.doneCount(store.active())==1,"Re-adding lost progress");});
 main(()->{JSONArray history=new JSONArray();java.time.LocalDate today=java.time.LocalDate.now();for(int i=0;i<100;i++){if(i%3==0)continue;JSONObject entry=Engine.copy(store.active());Engine.put(entry,"ended",today.minusDays(i).atTime(12,0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());history.put(entry);}Engine.put(store.data,"history",history);});
 capture(a,"progress","progress-sample.png");capture(a,"mine","mine-removal.png");
 result.putString("stream","PASS: remove/re-add preserves marks, plan and history; progress screens rendered. Original data restored.\n");code=Activity.RESULT_OK;
 }catch(Throwable e){result.putString("stream","FAIL: "+e+"\n");}finally{final MainActivity activity=a;main(()->{store.data=original;store.save();Notices.update(getTargetContext());if(activity!=null){activity.page="progress";activity.render();}});}finish(code,result);}
}
