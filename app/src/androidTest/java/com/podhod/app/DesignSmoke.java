package com.podhod.app;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import org.json.*;

/** Renders the real Activity at device dimensions, without requiring changes to the lock screen. */
public class DesignSmoke extends Instrumentation {
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    TextView find(View root,String text){if(root instanceof TextView && ((TextView)root).getText().toString().equals(text))return (TextView)root;if(root instanceof ViewGroup){ViewGroup g=(ViewGroup)root;for(int i=0;i<g.getChildCount();i++){TextView t=find(g.getChildAt(i),text);if(t!=null)return t;}}return null;}
    CheckBox setCheck(View root,int index){if(root instanceof CheckBox&&root.getContentDescription()!=null&&root.getContentDescription().toString().startsWith("Подход "+(index+1)+":"))return (CheckBox)root;if(root instanceof ViewGroup){ViewGroup g=(ViewGroup)root;for(int i=0;i<g.getChildCount();i++){CheckBox found=setCheck(g.getChildAt(i),index);if(found!=null)return found;}}return null;}
    void capture(MainActivity a,String page,String filename)throws Exception {
        runOnMainSync(()->{a.page=page;a.render();});waitForIdleSync();Thread.sleep(250);
        runOnMainSync(()->{try{View view=a.root;int width=a.getResources().getDisplayMetrics().widthPixels,height=a.getResources().getDisplayMetrics().heightPixels;view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));view.layout(0,0,width,height);check(view.getWidth()>0,"Activity not measured");Bitmap b=Bitmap.createBitmap(view.getWidth(),view.getHeight(),Bitmap.Config.ARGB_8888);view.draw(new Canvas(b));try(FileOutputStream out=getTargetContext().openFileOutput(filename,0)){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();}catch(Exception e){throw new RuntimeException(e);}});
    }
    @Override public void onStart(){Bundle report=new Bundle();int code=Activity.RESULT_CANCELED;MainActivity activity=null;Store store=Store.get(getTargetContext());JSONObject original=Engine.copy(store.data);
        try{
            Intent i=new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);activity=(MainActivity)startActivitySync(i);final MainActivity a=activity;
            capture(a,"mine","design-mine.png");
            runOnMainSync(()->check(find(a.root,"Твои максимумы")!=null,"Missing common maximum panel"));
            capture(a,"home","design-home.png");
            capture(a,"library","design-library.png");
            capture(a,"settings","design-settings.png");
            capture(a,"history","design-history.png");
            runOnMainSync(()->a.selected=store.data.optJSONArray("programs").optJSONObject(0).optString("id"));
            capture(a,"program","design-program.png");
            final AlertDialog[] sheet=new AlertDialog[1];
            runOnMainSync(()->{LinearLayout box=a.form();a.input(box,"Жим лёжа · 100%, кг","100",true);a.input(box,"Присед · 100%, кг","140",true);a.input(box,"Становая тяга · 100%, кг","180",true);sheet[0]=a.formDialog("Максимумы программы",box,"Применить ко всем дням",()->{});});waitForIdleSync();Thread.sleep(250);
            runOnMainSync(()->{try{View v=sheet[0].getWindow().getDecorView();check(v.getHeight()>0&&v.getHeight()<a.getResources().getDisplayMetrics().heightPixels,"Sheet must fit on screen");TextView apply=find(v,"Применить ко всем дням");check(apply!=null&&apply.getBottom()<=((View)apply.getParent()).getHeight(),"Sheet action clipped");Bitmap b=Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.ARGB_8888);v.draw(new Canvas(b));try(FileOutputStream out=getTargetContext().openFileOutput("design-sheet.png",0)){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();sheet[0].dismiss();}catch(Exception e){throw new RuntimeException(e);}});
            if(store.active()!=null)capture(a,"workout","design-workout.png");
            // Isolated temporary fixture in the same store: exercise the save/recalculate path,
            // notification refresh and persistence. All original data restored in finally.
            JSONObject p=Engine.copy(store.data.optJSONArray("programs").optJSONObject(0));Engine.put(p,"id","design-test");Engine.put(p,"nextDay",0);
            JSONObject session=Engine.start(p,1);
            runOnMainSync(()->{store.data.optJSONArray("programs").put(p);Engine.put(store.data,"active",session);store.updateMaxima(p,Engine.obj("{\"squat\":200,\"bench\":150,\"deadlift\":220}"),120);Notices.update(getTargetContext());});
            check(session.optJSONArray("sets").optJSONObject(0).optDouble("kg")==100,"Active weight not updated");
            JSONObject saved=Engine.obj(getTargetContext().getSharedPreferences("podhod",0).getString("state","{}"));
            check(saved.optJSONObject("active").optJSONArray("sets").optJSONObject(0).optDouble("kg")==100,"Updated weight not persisted");
            JSONArray programs=saved.optJSONArray("programs");check(programs.optJSONObject(programs.length()-1).optJSONObject("maxima").optDouble("squat")==200,"Maxima not persisted");
            boolean notice=false;for(var n:getTargetContext().getSystemService(NotificationManager.class).getActiveNotifications())if(n.getId()==7&&n.getNotification().extras.getCharSequence(Notification.EXTRA_TEXT,"").toString().contains("100"))notice=true;
            check(notice,"Notification did not receive recalculated weight");
            runOnMainSync(()->{a.page="home";a.render();TextView tab=find(a.root,"Тренировка");check(tab!=null,"Workout tab missing");((View)tab.getParent()).performClick();check(a.page.equals("workout"),"Workout tab did not navigate");TextView undo=find(a.actionDock,"↶  Отменить");check(undo!=null&&!undo.isEnabled(),"Undo must be disabled before first completed set");find(a.actionDock,"✓  Подход выполнен").performClick();check(store.active().optInt("cursor")==1,"Set not completed from UI");});
            capture(a,"workout","workout-undo.png");
            runOnMainSync(()->{TextView undo=find(a.actionDock,"↶  Отменить");check(undo!=null&&undo.isEnabled(),"Undo is not available after completion");undo.performClick();check(store.active().optInt("cursor")==0,"Undo button did not restore set");check(!store.active().optBoolean("rest"),"Undo did not stop rest");check(!store.active().optJSONArray("sets").optJSONObject(0).optBoolean("done"),"Completion mark remains");});
            JSONObject afterUndo=Engine.obj(getTargetContext().getSharedPreferences("podhod",0).getString("state","{}"));check(afterUndo.optJSONObject("active").optInt("cursor")==0,"Undo was not persisted");
            runOnMainSync(()->{setCheck(a.root,2).performClick();check(Engine.doneCount(store.active())==1&&store.active().optInt("cursor")==0,"Future checkbox marked earlier sets");setCheck(a.root,0).performClick();setCheck(a.root,1).performClick();check(store.active().optInt("cursor")==3,"Cursor did not skip marked set");setCheck(a.root,0).performClick();check(store.active().optInt("cursor")==0&&Engine.doneCount(store.active())==2,"Unchecking first set affected others");check(setCheck(a.root,1).isChecked()&&setCheck(a.root,2).isChecked(),"Independent checkbox state lost");});
            capture(a,"workout","workout-independent.png");
            JSONObject independent=Engine.obj(getTargetContext().getSharedPreferences("podhod",0).getString("state","{}"));check(Engine.doneCount(independent.optJSONObject("active"))==2&&independent.optJSONObject("active").optInt("cursor")==0,"Independent marks not persisted");
            runOnMainSync(()->{JSONArray sets=store.active().optJSONArray("sets");for(int n=0;n<sets.length();n++)Engine.put(sets.optJSONObject(n),"done",true);Engine.put(store.active(),"cursor",sets.length());a.render();check(setCheck(a.root,4)!=null,"All-complete screen hid checklist");setCheck(a.root,4).performClick();check(store.active().optInt("cursor")==4&&Engine.doneCount(store.active())==sets.length()-1,"Cannot reopen arbitrary set from completed screen");});
            runOnMainSync(()->{JSONArray sets=store.active().optJSONArray("sets");for(int n=0;n<sets.length();n++)Engine.put(sets.optJSONObject(n),"done",true);Engine.put(store.active(),"cursor",sets.length());a.render();TextView undo=find(a.actionDock,"↶  Отменить");check(undo!=null&&undo.isEnabled(),"Cannot undo final set before saving");int last=Engine.lastCompleted(store.active());undo.performClick();check(!sets.optJSONObject(last).optBoolean("done")&&Engine.doneCount(store.active())==sets.length()-1,"Most recently marked set not reopened");store.data.remove("active");a.render();check(find(a.root,"Начать тренировку  →")!=null,"Empty workout tab has no start action");});
            runOnMainSync(()->{JSONObject z=Zlat.create();Engine.put(z,"id","zlat-device-test");Zlat.configure(z,20,40,3);store.data.optJSONArray("programs").put(z);store.start(z);String zsid=store.active().optString("id");store.start(p);check(store.active().optString("programId").equals(p.optString("id")),"Program selection opened wrong session");store.start(z);check(store.active().optString("id").equals(zsid),"Switching lost Zlat session");JSONObject zs=store.active();for(int n=0;n<6;n++)store.setDone(zs.optString("id"),zs.optInt("revision"),n,true);check(!store.action(zs.optString("id"),zs.optInt("revision"),"finish"),"Zlat must confirm actual reps before notification finish");Engine.put(Zlat.last(zs,"pullup"),"reps",6);Engine.put(Zlat.last(zs,"dip"),"reps",4);Engine.put(zs,"confirmedRevision",zs.optInt("revision"));check(store.action(zs.optString("id"),zs.optInt("revision"),"finish"),"Zlat finish failed");check(z.optJSONObject("workingWeights").optDouble("pullup")==21.25&&z.optJSONObject("workingWeights").optDouble("dip")==40,"Zlat progression incorrect");check(z.optInt("nextDay")==1,"Zlat did not advance day");store.start(z);check(Zlat.last(store.active(),"pullup").optDouble("kg")==21.25,"Next session did not receive weight");});
            capture(a,"workout","zlat-workout.png");
            report.putString("stream","PASS: independent marks; persistence; undo; Zlat confirmation guard, progression and next-day weights. Original data restored.\n");code=Activity.RESULT_OK;
        }catch(Throwable e){report.putString("stream","FAIL: "+e+"\n");}
        finally{MainActivity a=activity;runOnMainSync(()->{store.data=original;store.save();Notices.update(getTargetContext());if(a!=null){a.page="mine";a.render();}});}
        finish(code,report);
    }
}
