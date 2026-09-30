package com.podhod.app;
import android.app.*;
import android.content.*;
import android.widget.*;
import org.json.*;
import com.google.firebase.firestore.*;

/** Detached moderator drafts never enter the user's training store. */
final class ModeratorEditor {
 final MainActivity a;final JSONObject draft;final DocumentSnapshot expected;final boolean submission;final Runnable refresh;
 AlertDialog dialog;LinearLayout body;boolean saving;
 ModeratorEditor(MainActivity a,JSONObject source,DocumentSnapshot expected,boolean submission,Runnable refresh){this.a=a;this.draft=Engine.copy(source);this.expected=expected;this.submission=submission;this.refresh=refresh;}
 static String s(String en,String ru){return CommunityUi.s(en,ru);}
 static void submission(MainActivity a,DocumentSnapshot row,Runnable refresh){if(!CommunityPlans.admin(a))return;try{new ModeratorEditor(a,CommunityPlans.decode(row.getData()),row,true,refresh).show();}catch(Exception e){a.error(e);}}
 static void library(MainActivity a,JSONObject source){
  if(!CommunityPlans.admin(a))return;
  String key=source.optString("catalogId",PlanPopularity.key(source));
  for(JSONObject p:CommunityCatalog.get(a).published)if(CommunityCatalog.catalogKey(p).equals(CommunityCatalog.catalogKey(source))){key=p.optString("catalogId");break;}
  final String id=key;
  FriendsPanel.run(a,store->store.waitFor(store.db.document("publicPlans/"+id).get(Source.SERVER)),row->{try{JSONObject p=row.exists()?CommunityPlans.decode(row.getData()):Engine.copy(source);EnglishPlans.plan(p);new ModeratorEditor(a,p,row,false,()->CommunityCatalog.get(a).load(true)).show();}catch(Exception e){a.error(e);}});
 }
 void show(){body=a.form();draw();dialog=a.panel(s("Library editor","Редактор библиотеки"),body,s("Close","Закрыть"),()->{},false);dialog.setOnCancelListener(v->{});}
 void draw(){body.removeAllViews();a.programCover(body,draft,s("MODERATOR DRAFT","ЧЕРНОВИК МОДЕРАТОРА"),true);
  a.space(body,12);body.addView(a.text(s("Use English for public content. Personal copies, workout history, author credit and likes are preserved.","Публичные тексты — на английском. Личные копии, история тренировок, авторство и лайки сохраняются."),14,a.MUTED));
  a.actionRow(body,"library",s("Plan details","Описание плана"),s("Title, description, category and source","Название, описание, категория и источник"),this::details);
  a.actionRow(body,"library",s("Days and exercises","Дни и упражнения"),s("Sets, reps, weights, percentages and muscles","Подходы, повторы, веса, проценты и мышцы"),()->new PlanEditor(a,draft,this::draw).show());
  a.actionRow(body,"library",s("Cover image","Обложка"),s("Choose a photo and adjust the crop","Выбрать фото и настроить положение"),()->{
   a.moderatorPhoto=uri->new Thread(()->{try{String encoded=PlanImages.read(a.getApplicationContext(),uri);a.runOnUiThread(()->{if(!a.isDestroyed()&&dialog.isShowing())CoverEditor.show(a,draft,encoded,false,this::draw);});}catch(Exception e){a.runOnUiThread(()->a.error(e));}},"moderator-cover").start();
   a.startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE),75);
  });
  if(!draft.optString("coverImage").isEmpty())a.link(body,s("Reset cover","Сбросить обложку"),()->{for(String key:new String[]{"coverImage","coverX","coverY","coverZoom"})draft.remove(key);draw();});
  a.button(body,submission?s("Save reviewed draft","Сохранить исправленную заявку"):s("Update library plan","Обновить план в библиотеке"),true,()->{
   if(saving)return;
   try{Engine.validateProgram(draft);}catch(Exception e){a.error(e);return;}
   new AlertDialog.Builder(a).setTitle(submission?s("Save changes for review?","Сохранить исправления заявки?"):s("Publish these changes?","Опубликовать изменения?"))
    .setMessage(submission?s("The author stays the same. Review the updated copy before approving it.","Автор останется прежним. Перед одобрением проверь исправленную копию."):s("The library will show this version. Existing personal copies and active workouts will stay as they are.","В библиотеке появится эта версия. Существующие личные копии и текущие тренировки сохранятся."))
    .setNegativeButton(s("Cancel","Отмена"),null).setPositiveButton(s("Save","Сохранить"),(v,w)->{
     saving=true;JSONObject outgoing=Engine.copy(draft);FriendsPanel.run(a,store->{try{if(submission)CommunityPlans.saveSubmission(store,expected,outgoing);else CommunityPlans.saveLibrary(store,expected,outgoing);return true;}finally{a.runOnUiThread(()->saving=false);}},ok->{dialog.dismiss();Toast.makeText(a,s("Changes saved","Изменения сохранены"),Toast.LENGTH_SHORT).show();refresh.run();});
    }).show();
  });
 }
 void details(){LinearLayout box=a.form();EditText name=a.input(box,s("Title","Название"),draft.optString("name"),false),description=a.input(box,s("Description","Описание"),draft.optString("description"),false),source=a.input(box,s("Source","Источник"),draft.optString("source"),false),ending=a.input(box,s("Final instructions","Заключительные инструкции"),draft.optString("terminalNote"),false);description.setSingleLine(false);description.setMinLines(3);
  Spinner category=new Spinner(a);String[] labels=new String[PlanCategory.LABELS.length];for(int i=0;i<labels.length;i++)labels[i]=Lang.t(PlanCategory.LABELS[i]);category.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,labels));category.setSelection(PlanCategory.index(draft));box.addView(category);EditText custom=a.input(box,s("Custom category","Своя категория"),draft.optString("categoryName"),false);
  a.formDialog(s("Plan details","Описание плана"),box,s("Save","Сохранить"),()->{String n=name.getText().toString().trim();if(n.isEmpty()||n.length()>120)throw new IllegalArgumentException(s("Title must be 1–120 characters.","Название — от 1 до 120 символов."));JSONObject candidate=Engine.copy(draft);PlanCategory.set(candidate,category.getSelectedItemPosition(),custom.getText().toString());Engine.put(candidate,"name",n);Engine.put(candidate,"description",description.getText().toString().trim());Engine.put(candidate,"source",source.getText().toString().trim());Engine.put(candidate,"terminalNote",ending.getText().toString().trim());Engine.validateProgram(candidate);candidate.keys().forEachRemaining(k->Engine.put(draft,k,candidate.opt(k)));if(!candidate.has("categoryName"))draft.remove("categoryName");draw();});
 }
}
