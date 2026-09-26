package com.podhod.app;
import android.app.*;import android.content.*;import android.graphics.*;import android.os.*;import android.view.*;import android.widget.*;import java.io.*;import org.json.*;
public class LanguageSmoke extends Instrumentation {
 volatile MainActivity latest;
 public void onCreate(Bundle b){super.onCreate(b);start();}
 void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 void main(Runnable r){final Throwable[] e={null};runOnMainSync(()->{try{r.run();}catch(Throwable t){e[0]=t;}});if(e[0]!=null)throw new AssertionError(e[0]);}
 TextView find(View v,String text){if(v instanceof TextView&&((TextView)v).getText().toString().equals(text))return (TextView)v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){TextView t=find(((ViewGroup)v).getChildAt(i),text);if(t!=null)return t;}return null;}
 boolean customName(String name){JSONArray plans=Store.get(getTargetContext()).data.optJSONArray("programs");for(int i=0;i<plans.length();i++){JSONObject p=plans.optJSONObject(i);if(name.equals(p.optString("name"))&&name.equals(Lang.content(name)))return true;}return false;}
 String withoutCustomExerciseNames(String text){JSONArray plans=Store.get(getTargetContext()).data.optJSONArray("programs");for(int i=0;i<plans.length();i++){JSONArray days=plans.optJSONObject(i).optJSONArray("days");for(int j=0;j<days.length();j++){JSONArray groups=days.optJSONObject(j).optJSONArray("groups");for(int k=0;k<groups.length();k++){String name=groups.optJSONObject(k).optString("exercise");if(!name.isEmpty()&&name.equals(Lang.content(name)))text=text.replace(name,"");}}}return text;}
 void english(View v){if(v instanceof TextView&&!(v instanceof EditText)){String s=((TextView)v).getText().toString();if(!s.equals("Русский")&&!s.equals("✓  Русский")&&!customName(s))check(!withoutCustomExerciseNames(s).matches("(?s).*[А-Яа-яЁё].*"),"Untranslated UI: "+s);}if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)english(((ViewGroup)v).getChildAt(i));}
 void capture(MainActivity a,String page,String filename){main(()->{try{a.page=page;a.render();english(a.root);View v=a.root;int w=a.getResources().getDisplayMetrics().widthPixels,h=a.getResources().getDisplayMetrics().heightPixels;v.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));v.layout(0,0,w,h);Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);v.draw(new Canvas(b));try(FileOutputStream out=getTargetContext().openFileOutput(filename,0)){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();}catch(Exception e){throw new RuntimeException(e);}});}
 public void onStart(){String originalLanguage=Lang.english()?"en":"ru";String original=Store.get(getTargetContext()).data.toString();Bundle report=new Bundle();int code=Activity.RESULT_CANCELED;
 Application app=(Application)getTargetContext().getApplicationContext();Application.ActivityLifecycleCallbacks callbacks=new Application.ActivityLifecycleCallbacks(){public void onActivityResumed(Activity a){if(a instanceof MainActivity)latest=(MainActivity)a;}public void onActivityCreated(Activity a,Bundle b){}public void onActivityStarted(Activity a){}public void onActivityPaused(Activity a){}public void onActivityStopped(Activity a){}public void onActivitySaveInstanceState(Activity a,Bundle b){}public void onActivityDestroyed(Activity a){}};app.registerActivityLifecycleCallbacks(callbacks);
 try{
 File imageFile=new File(getTargetContext().getCacheDir(),"cover-smoke.jpg");
 Bitmap source=Bitmap.createBitmap(1600,800,Bitmap.Config.ARGB_8888);source.eraseColor(Color.RED);
 try(FileOutputStream out=new FileOutputStream(imageFile)){source.compress(Bitmap.CompressFormat.JPEG,95,out);}source.recycle();
 android.media.ExifInterface exif=new android.media.ExifInterface(imageFile.getPath());exif.setAttribute(android.media.ExifInterface.TAG_ORIENTATION,"6");exif.saveAttributes();
 String encoded=PlanImages.read(getTargetContext(),android.net.Uri.fromFile(imageFile));imageFile.delete();
 JSONObject coverPlan=new JSONObject();Engine.put(coverPlan,"coverImage",encoded);
 Bitmap decoded=PlanImages.decode(Engine.copy(coverPlan).optString("coverImage"));
 check(decoded!=null&&decoded.getHeight()>decoded.getWidth()&&decoded.getHeight()<=1024,"Cover sizing, rotation or JSON persistence failed");
 check(PlanImages.decode("not an image")==null,"Invalid cover did not fall back");
 main(()->Lang.set(getTargetContext(),"ru"));latest=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));MainActivity before=latest;
 main(()->{before.page="settings";before.render();TextView choice=find(before.root,"English");check(choice!=null,"English selector missing");choice.performClick();});
 for(int i=0;i<30&&latest==before;i++)Thread.sleep(100);waitForIdleSync();check(latest!=before,"Language change did not recreate activity");check(latest.page.equals("settings"),"Language change left settings");check(Lang.english(),"English not selected");check(getTargetContext().getSharedPreferences("trace-settings",0).getString("language","").equals("en"),"Language not persisted");main(()->Lang.init(getTargetContext()));check(Lang.english(),"Language lost on initialization");
 for(String page:new String[]{"settings","mine","library","workout","progress","history","steps"})capture(latest,page,"en-"+page+".png");
 main(()->{latest.page="progress";latest.render();WalkingProgress walking=latest.walkingProgress;check(walking!=null,"Walking card missing");walking.chart.getChildAt(0).performClick();check(walking.selected.equals(java.time.LocalDate.now().minusDays(6)),"Chart selection failed");check(!walking.detail.getText().toString().isEmpty(),"Selected day details missing");walking.chart.getChildAt(6).performClick();check(walking.selected.equals(java.time.LocalDate.now()),"Today selection failed");check(walking.steps.getText().toString().equals(StepsPage.format(Steps.today(latest))),"Walking total mismatch");});
 main(()->{
 JSONObject testPlan=Engine.copy(latest.store.program("builtin-sheiko-12-week"));Engine.put(testPlan,"coverImage",encoded);
 android.widget.LinearLayout holder=latest.column();latest.programCover(holder,testPlan,"CUSTOM COVER",true);
 int w=latest.dp(360),h=latest.dp(180);holder.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));holder.layout(0,0,w,h);
 Bitmap preview=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);holder.getChildAt(0).getBackground().setBounds(0,0,w,h);holder.getChildAt(0).getBackground().draw(new Canvas(preview));
 int pixel=preview.getPixel(w/2,h/2);check(Color.red(pixel)>Color.green(pixel)+30,"Custom cover was not displayed");preview.recycle();
 CoverFrame frame=new CoverFrame(.5f,.5f,2);CoverEditor.CropView crop=new CoverEditor.CropView(latest,decoded,frame);android.widget.FrameLayout parent=new android.widget.FrameLayout(latest);parent.addView(crop);crop.layout(0,0,400,180);
 long now=android.os.SystemClock.uptimeMillis();android.view.MotionEvent down=android.view.MotionEvent.obtain(now,now,0,100,100,0);android.view.MotionEvent move=android.view.MotionEvent.obtain(now,now+30,2,160,110,0);android.view.MotionEvent up=android.view.MotionEvent.obtain(now,now+60,1,160,110,0);
 crop.onTouchEvent(down);crop.onTouchEvent(move);crop.onTouchEvent(up);down.recycle();move.recycle();up.recycle();check(frame.x<.5f,"Cover drag gesture did not move image");
 AlertDialog editor=CoverEditor.show(latest,testPlan,encoded,true);
 english(editor.getWindow().getDecorView());editor.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
 latest.selected="builtin-sheiko-12-week";
 });capture(latest,"program","en-sheiko-cycle.png");
 main(()->Lang.set(getTargetContext(),"ru"));
 main(()->{latest.page="program";latest.render();View v=latest.root;int w=latest.getResources().getDisplayMetrics().widthPixels,h=latest.getResources().getDisplayMetrics().heightPixels;v.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));v.layout(0,0,w,h);Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);v.draw(new Canvas(b));try(FileOutputStream out=getTargetContext().openFileOutput("ru-sheiko-cycle.png",0)){b.compress(Bitmap.CompressFormat.PNG,100,out);}catch(Exception e){throw new RuntimeException(e);}b.recycle();Lang.set(getTargetContext(),"en");});
 main(()->{latest.error(new IllegalArgumentException(Lang.t("Некорректный вес")));});
 check(Lang.content("Неделя 2 · День 3").equals("Week 2 · Day 3"),"Built-in day not translated");check(Lang.content("Мой личный план 42").equals("Мой личный план 42"),"Custom name changed");
 for(var n:getTargetContext().getSystemService(NotificationManager.class).getActiveNotifications())if(n.getId()==7){String title=n.getNotification().extras.getCharSequence(Notification.EXTRA_TITLE,"").toString();check(!withoutCustomExerciseNames(title).matches(".*[А-Яа-я].*"),"Notification not translated");}
 main(()->{Lang.set(getTargetContext(),"ru");latest.page="settings";latest.render();check(find(latest.root,"Настройки")!=null,"Russian switch back failed");});
 check(original.equals(Store.get(getTargetContext()).data.toString()),"Language changed workout data");report.putString("stream","PASS: language picker, recreation, persistence, six English screens, built-in names, notification, Russian return, unchanged workout data.\n");code=Activity.RESULT_OK;
 }catch(Throwable e){report.putString("stream","FAIL: "+e+"\n");}finally{main(()->{Lang.set(getTargetContext(),originalLanguage);Notices.update(getTargetContext());if(latest!=null){latest.page="settings";latest.render();}});app.unregisterActivityLifecycleCallbacks(callbacks);}finish(code,report);}
}
